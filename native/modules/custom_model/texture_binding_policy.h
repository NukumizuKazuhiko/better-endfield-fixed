#pragma once
#include <string_view>
#include <vector>

namespace BetterEndfield::CustomModel {
enum class TexturePinStatus { Matched, Missing, Ambiguous };

template<class Slot> struct TexturePinMatch {
    TexturePinStatus status=TexturePinStatus::Missing;
    std::vector<const Slot*> slots;
};

// Multiple shader properties may refer to one source Texture object. Distinct
// objects sharing a name cannot establish an unambiguous package binding.
template<class Slot,class Name>
TexturePinMatch<Slot> MatchTexturePins(const std::vector<Slot>& slots,
    std::string_view expected,Name&& object_name) {
    TexturePinMatch<Slot> result;
    for (const auto& slot:slots) {
        if (object_name(slot.texture)!=expected) continue;
        if (!result.slots.empty() && result.slots.front()->texture!=slot.texture) {
            result.status=TexturePinStatus::Ambiguous;
            return result;
        }
        result.slots.push_back(&slot);
    }
    if (!result.slots.empty()) result.status=TexturePinStatus::Matched;
    return result;
}
}
