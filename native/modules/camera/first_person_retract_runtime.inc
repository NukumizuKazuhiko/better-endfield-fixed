// Samples the game's own camera and animation owners on the engine thread.
// Each binding is resolved by its complete IL2CPP signature before use.
struct FpRetractSampler {
    MethodContract manager{"retract.camera_manager",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.View","CameraUtils",
            "get_cameraManager",nullptr,"Beyond.Gameplay.View.CameraManager",0}};
    MethodContract active_controller{"retract.active_controller",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.View","CameraManager",
            "get_curActiveController",nullptr,"Beyond.Gameplay.View.CameraControllerBase",0}};
    MethodContract level_controller{"retract.level_controller",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.View","CameraManager",
            "GetMainLevelCameraController",nullptr,"Beyond.Gameplay.View.LevelCameraController",0}};
    MethodContract cinematic{"retract.in_cinematic",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.Core","Entity",
            "get_inCinematic",nullptr,"System.Boolean",0}};
    MethodContract ability{"retract.ability",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.Core","Entity",
            "get_abilityCom",nullptr,"Beyond.Gameplay.Core.AbilitySystem",0}};
    MethodContract in_skill{"retract.in_skill",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.Core","AbilitySystem",
            "get_inSkill",nullptr,"System.Boolean",0}};
    MethodContract current_skill{"retract.current_skill",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.Core","AbilitySystem",
            "get_curSkill",nullptr,"Beyond.Gameplay.Core.Skill",0}};
    MethodContract ultimate_id{"retract.ultimate_id",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.Core","AbilitySystem",
            "get_curUltimateSkill",nullptr,"System.String",0}};
    MethodContract skill_id{"retract.skill_id",
        {"Gameplay.Beyond.dll","Beyond.Gameplay.Core","Skill",
            "get_skillId",nullptr,"System.String",0}};
    bool attempted=false, camera_ready=false, skill_ready=false;
    unsigned reports=0;

    void Report(const char* reason) {
        if (reports<8) {++reports;Log(std::string("First person camera retract unavailable: ")+reason);}
    }
    bool Resolve(MethodContract& contract, bool expected_static,
            uint32_t (*flags)(const void*,uint32_t*)) {
        BE_ResolvedMethodV1 resolved{};
        if (g_host->resolve_method(g_host->context,&contract.descriptor,&resolved)!=BE_Result_Ok ||
            !resolved.method_info) {Report(contract.key);return false;}
        uint32_t implementation_flags=0;
        if (((flags(resolved.method_info,&implementation_flags)&0x10u)!=0)!=expected_static) {
            Report("method staticness mismatch");return false;
        }
        contract.method_info=resolved.method_info;
        contract.resolved=true;
        return true;
    }
    bool Init() {
        if (attempted) return camera_ready;
        attempted=true;
        if (!g_host || !g_host->resolve_method || !g_host->runtime_invoke ||
            !g_host->gchandle_new || !g_host->gchandle_free) {
            Report("host capabilities");return false;
        }
        const auto game=GetModuleHandleW(L"GameAssembly.dll");
        if (!game) {Report("runtime module");return false;}
        auto flags=reinterpret_cast<uint32_t(*)(const void*,uint32_t*)>(
            GetProcAddress(game,"il2cpp_method_get_flags"));
        if (!flags) {Report("method flags export");return false;}
        camera_ready=Resolve(manager,true,flags) && Resolve(active_controller,false,flags) &&
            Resolve(level_controller,false,flags);
        // The two contracts are independent: a missing skill accessor must not
        // disable normal first-person play or the game-camera handoff.
        skill_ready=g_host->object_unbox && g_host->copy_managed_string &&
            Resolve(cinematic,false,flags) &&
            Resolve(ability,false,flags) && Resolve(in_skill,false,flags) &&
            Resolve(current_skill,false,flags) && Resolve(ultimate_id,false,flags) &&
            Resolve(skill_id,false,flags);
        return camera_ready;
    }
    bool ReadBool(MethodContract& method,void* object,bool& value) {
        return Unbox(Invoke(&method,object,nullptr),value);
    }
    bool ReadId(void* object,std::string& value) {
        value.clear();
        if (!object) return false;
        char text[256]{};
        const int copied=g_host->copy_managed_string(g_host->context,object,text,sizeof(text));
        if (copied<=0 || copied>=static_cast<int>(sizeof(text)-1)) return false;
        value.assign(text,static_cast<size_t>(copied));
        return true;
    }
    // Returns false when a required runtime value cannot be read. A null level
    // controller with an active controller is a game-owned non-level view.
    bool Sample(bool& game_camera,bool& ultimate,bool& in_cinematic) {
        game_camera=ultimate=in_cinematic=false;
        if (!Init()) return false;
        FpRoot camera_manager(Invoke(&manager,nullptr,nullptr));
        if (!camera_manager) {Report("camera manager missing");return false;}
        FpRoot active(Invoke(&active_controller,camera_manager.object,nullptr));
        if (!active) {game_camera=true;return true;}
        FpRoot level(Invoke(&level_controller,camera_manager.object,nullptr));
        game_camera=!level || active.object!=level.object;
        if (game_camera) return true;
        if (!skill_ready) return true;

        FpRoot entity(Invoke(Contract("player_controller.get_main_character"),nullptr,nullptr));
        // During a scene transition the player may not exist. Keep the game
        // camera in charge until a valid gameplay actor is back.
        if (!entity) {game_camera=true;return true;}
        if (!ReadBool(cinematic,entity.object,in_cinematic)) {
            Report("cinematic getter");return true;
        }
        if (in_cinematic) return true;
        FpRoot ability_object(Invoke(&ability,entity.object,nullptr));
        if (!ability_object) {Report("ability component missing");return true;}
        bool casting=false;
        if (!ReadBool(in_skill,ability_object.object,casting)) {
            Report("skill state getter");return true;
        }
        if (!casting) return true;
        FpRoot skill(Invoke(&current_skill,ability_object.object,nullptr));
        if (!skill) return true;
        FpRoot current_id(Invoke(&skill_id,skill.object,nullptr));
        FpRoot configured_id(Invoke(&ultimate_id,ability_object.object,nullptr));
        std::string current,configured;
        if (!ReadId(current_id.object,current) || !ReadId(configured_id.object,configured)) {
            Report("ultimate skill id");return true;
        }
        ultimate=current==configured;
        return true;
    }
};
FpRetractSampler g_fp_retract;
