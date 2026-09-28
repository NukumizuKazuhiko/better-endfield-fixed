// Optional cap-only bindings. The signatures match custom_model/module.cpp
// and upstream camera_mesh_copy.hpp; named metadata validates value layouts.
struct FpCapUpload {
    void (*vertex_params)(void*, int32_t, const void*, int32_t) = nullptr;
    void (*vertex_data)(void*, int32_t, const void*, int32_t, int32_t, int32_t, int32_t, int32_t) = nullptr;
    void (*get_bounds)(void*, void*) = nullptr;
    void (*set_bounds)(void*, const void*) = nullptr;
    int (*blend_shape_count)(void*) = nullptr;
    bool attempted = false, ready = false;
    bool Init() {
        if (attempted) return ready;
        attempted = true;
        auto& engine = g_fp_engine;
        if (!engine.resolve || !g_host || !g_host->resolve_class) return false;
        const char* types[]{"VertexAttributeDescriptor", "Bounds", "Vector3"};
        const char* namespaces[]{"UnityEngine.Rendering", "UnityEngine", "UnityEngine"};
        const int32_t sizes[]{static_cast<int32_t>(sizeof(FpAttribute)), static_cast<int32_t>(sizeof(FpBounds)), 12};
        for (int index = 0; index < 3; ++index) {
            BE_ResolvedClassV1 type{};
            uint32_t alignment = 0;
            if (g_host->resolve_class(g_host->context, "UnityEngine.CoreModule.dll", namespaces[index], types[index], &type) != BE_Result_Ok ||
                !type.class_info || FpValue<int32_t>(0, engine.value_size, type.class_info, &alignment) != sizes[index]) return false;
        }
        BE_FieldDescriptorV1 first{"UnityEngine.CoreModule.dll", "UnityEngine", "Vector3", "x", "System.Single"};
        BE_ResolvedFieldV1 field{};
        if (g_host->resolve_field(g_host->context, &first, &field) != BE_Result_Ok || field.offset < 0) return false;
        const int boxed = field.offset;
        const char* attributes[]{"<attribute>k__BackingField", "<format>k__BackingField", "<dimension>k__BackingField", "<stream>k__BackingField"};
        for (int index = 0; index < 4; ++index)
            if (!engine.CheckField("UnityEngine.Rendering", "VertexAttributeDescriptor", attributes[index], boxed + index * 4)) return false;
        if (!engine.CheckField("UnityEngine", "Bounds", "m_Center", boxed) ||
            !engine.CheckField("UnityEngine", "Bounds", "m_Extents", boxed + 12) ||
            !engine.CheckField("UnityEngine", "Vector3", "y", boxed + 4) ||
            !engine.CheckField("UnityEngine", "Vector3", "z", boxed + 8)) return false;
        ready = engine.Bind(vertex_params, "UnityEngine.Mesh::SetVertexBufferParamsFromPtr") &&
            engine.Bind(vertex_data, "UnityEngine.Mesh::InternalSetVertexBufferData") &&
            engine.Bind(get_bounds, "UnityEngine.Mesh::get_bounds_Injected") &&
            engine.Bind(set_bounds, "UnityEngine.Mesh::set_bounds_Injected") &&
            engine.Bind(blend_shape_count, "UnityEngine.Mesh::get_blendShapeCount");
        return ready;
    }
};
FpCapUpload g_fp_cap_upload;

bool FpCapBoundsEqual(const FpBounds& left, const FpBounds& right) {
    for (int axis = 0; axis < 3; ++axis) {
        if (!std::isfinite(left.center[axis]) || !std::isfinite(left.extents[axis]) || left.extents[axis] < 0 ||
            !std::isfinite(right.center[axis]) || !std::isfinite(right.extents[axis]) || right.extents[axis] < 0 ||
            std::abs(left.center[axis] - right.center[axis]) > 1e-5f ||
            std::abs(left.extents[axis] - right.extents[axis]) > 1e-5f) return false;
    }
    return true;
}

bool FpPrepareCapStreams(const FpSnapshot& original, const FpMesh::CapVertexResult& caps, const FpBounds& source_bounds,
    FpSnapshot& expected, FpBounds& bounds, std::string& error) {
    constexpr size_t budget = 128 * 1024 * 1024;
    if (caps.status != FpMesh::CapVertexStatus::Ok || caps.source_map.empty() || caps.normals.size() != caps.source_map.size()) {
        error = "invalid cap allocation"; return false;
    }
    FpAttribute normal{}, tangent{};
    const auto* normal_data = FpChannel(original, 1, 0, normal);
    const auto* tangent_data = FpChannel(original, 2, 0, tangent);
    if (!normal_data || normal.format != 0 || normal.dimension != 3 ||
        (tangent_data && (tangent.format != 0 || tangent.dimension != 4))) {
        error = "cap requires Float32x3 normals and optional Float32x4 tangents"; return false;
    }
    const size_t count = static_cast<size_t>(original.count) + caps.source_map.size();
    if (count > 200000) { error = "cap vertex budget exceeded"; return false; }
    size_t bytes = 0;
    for (const int stride : original.strides) {
        if (stride <= 0 || count > (budget - bytes) / static_cast<size_t>(stride)) {
            error = "cap stream budget exceeded"; return false;
        }
        bytes += count * static_cast<size_t>(stride);
    }
    expected = original;
    expected.count = static_cast<int>(count);
    for (size_t stream = 0; stream < expected.streams.size(); ++stream) {
        const size_t stride = static_cast<size_t>(original.strides[stream]);
        expected.streams[stream].resize(count * stride);
        for (size_t index = 0; index < caps.source_map.size(); ++index) {
            const size_t source = caps.source_map[index];
            if (source >= static_cast<size_t>(original.count)) { error = "invalid cap source"; return false; }
            std::memcpy(expected.streams[stream].data() + (static_cast<size_t>(original.count) + index) * stride,
                original.streams[stream].data() + source * stride, stride);
        }
    }
    for (size_t index = 0; index < caps.source_map.size(); ++index) {
        const size_t target = static_cast<size_t>(original.count) + index;
        const auto n = caps.normals[index];
        if (!FpMesh::Finite(n) || std::abs(FpMesh::Length(n) - 1) > 1e-5) {
            error = "invalid cap normal"; return false;
        }
        const float normal_value[]{static_cast<float>(n.x), static_cast<float>(n.y), static_cast<float>(n.z)};
        FpAttribute channel{};
        auto* target_normal = const_cast<uint8_t*>(FpChannel(expected, 1, target, channel));
        std::memcpy(target_normal, normal_value, sizeof(normal_value));
        if (tangent_data) {
            auto* target_tangent = const_cast<uint8_t*>(FpChannel(expected, 2, target, channel));
            const FpMesh::Point source{FpLoad<float>(target_tangent), FpLoad<float>(target_tangent + 4), FpLoad<float>(target_tangent + 8)};
            const float handedness = FpLoad<float>(target_tangent + 12);
            const auto projection = source - n * FpMesh::Dot(source, n);
            const double length = FpMesh::Length(projection);
            if (!FpMesh::Finite(projection) || !std::isfinite(length) || length < 1e-8 ||
                !std::isfinite(handedness) || std::abs(std::abs(handedness) - 1) > 1e-4f) {
                error = "cap tangent cannot be projected reliably"; return false;
            }
            const float value[]{static_cast<float>(projection.x / length), static_cast<float>(projection.y / length),
                static_cast<float>(projection.z / length), handedness};
            std::memcpy(target_tangent, value, sizeof(value));
        }
    }
    if (!FpCapBoundsEqual(source_bounds, source_bounds) ||
        !FpMesh::Finite(caps.bounds.minimum) || !FpMesh::Finite(caps.bounds.maximum)) {
        error = "invalid source or cap bounds"; return false;
    }
    const double minimum[]{caps.bounds.minimum.x,caps.bounds.minimum.y,caps.bounds.minimum.z};
    const double maximum[]{caps.bounds.maximum.x,caps.bounds.maximum.y,caps.bounds.maximum.z};
    for (int axis = 0; axis < 3; ++axis) {
        if (minimum[axis] > maximum[axis]) {error = "reversed cap bounds"; return false;}
        const double low = std::min(double(source_bounds.center[axis])-source_bounds.extents[axis],minimum[axis]);
        const double high = std::max(double(source_bounds.center[axis])+source_bounds.extents[axis],maximum[axis]);
        bounds.center[axis] = static_cast<float>((low+high)*.5);
        bounds.extents[axis] = static_cast<float>((high-low)*.5);
    }
    if (!FpCapBoundsEqual(bounds, bounds)) {error = "combined bounds overflow"; return false;}
    return true;
}

bool FpUploadCapStreams(void* mesh, const FpSnapshot& expected) {
    auto& engine = g_fp_engine;
    auto& upload = g_fp_cap_upload;
    if (!FpCall(upload.vertex_params, mesh, expected.count, expected.attributes.data(), static_cast<int32_t>(expected.attributes.size()))) return false;
    // Unity chooses offsets from declarations. Refuse a changed layout before
    // sending opaque source bytes; verification reads every stream afterwards.
    if (FpValue<int>(-1, engine.stream_count, mesh) != static_cast<int>(expected.streams.size())) return false;
    for (size_t index = 0; index < expected.attributes.size(); ++index) {
        const auto attribute = expected.attributes[index].attribute;
        if (FpValue<int>(-1, engine.offset, mesh, attribute) != expected.offsets[index]) return false;
    }
    for (size_t stream = 0; stream < expected.streams.size(); ++stream) {
        if (FpValue<int>(-1, engine.stride, mesh, static_cast<int>(stream)) != expected.strides[stream] ||
            !FpCall(upload.vertex_data, mesh, static_cast<int32_t>(stream), expected.streams[stream].data(), 0, 0,
                static_cast<int32_t>(expected.streams[stream].size()), 1, 0)) return false;
    }
    return true;
}

// Buffer reallocation can reset the submesh count. Restore it through the
// same exact managed contract used by the custom-model upload path.
bool FpRestoreSubmeshCount(void* mesh, int count) {
    if (FpValue<int>(-1, g_fp_engine.submesh_count, mesh) == count) return true;
    if (!g_host || !g_host->resolve_method || !g_host->runtime_invoke) return false;
    const BE_MethodDescriptorV1 descriptor{"UnityEngine.CoreModule.dll", "UnityEngine", "Mesh",
        "set_subMeshCount", "System.Int32", "System.Void", 1};
    BE_ResolvedMethodV1 method{};
    if (g_host->resolve_method(g_host->context, &descriptor, &method) != BE_Result_Ok || !method.method_info) return false;
    void* parameters[]{&count};
    void* exception = nullptr;
    g_host->runtime_invoke(g_host->context, method.method_info, mesh, parameters, &exception);
    return !exception && FpValue<int>(-1, g_fp_engine.submesh_count, mesh) == count;
}
