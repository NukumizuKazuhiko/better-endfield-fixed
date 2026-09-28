// Runtime-only S6 adapter. Exact owners, parameter counts and Boolean return
// types were cross-checked against this client's IL2CPP metadata; the resolver
// still validates each signature before any managed call.
struct FpCombatSampler {
    enum Method { Fight, Player, MainCharacter, Ability, Skill, Casting, Count };
    MethodContract methods[Count]{
        {"combat.fight",{"Gameplay.Beyond.dll","Beyond.Gameplay","GlobalTagUtils","IsInFight",nullptr,"System.Boolean",0}},
        {"combat.player",{"Gameplay.Beyond.dll","Beyond.Gameplay","GameInstance","get_playerController",nullptr,"Beyond.Gameplay.Core.PlayerController",0}},
        {"combat.main_character",{"Gameplay.Beyond.dll","Beyond.Gameplay.Core","PlayerController","GetMainCharacter",nullptr,"Beyond.Gameplay.Core.Entity",0}},
        {"combat.ability",{"Gameplay.Beyond.dll","Beyond.Gameplay.Core","Entity","get_abilityCom",nullptr,"Beyond.Gameplay.Core.AbilitySystem",0}},
        {"combat.skill",{"Gameplay.Beyond.dll","Beyond.Gameplay.Core","AbilitySystem","get_inSkill",nullptr,"System.Boolean",0}},
        {"combat.casting",{"Gameplay.Beyond.dll","Beyond.Gameplay.Core","PlayerController","get_castingNormalAttack",nullptr,"System.Boolean",0}}
    };
    BE_ResolvedClassV1 entity_class{},player_class{},ability_class{};
    const void* (*object_class)(void*) = nullptr;
    bool (*assignable)(const void*,const void*) = nullptr;
    uint32_t (*method_flags)(const void*,uint32_t*) = nullptr;
    bool attempted=false,ready=false;
    unsigned reports=0;
    bool Unavailable(const char* reason) {
        if(reports<8){++reports;Log(std::string("First person combat state unavailable: ")+reason);}
        return false;
    }
    bool Init() {
        if(attempted)return ready;
        attempted=true;
        if(!g_host||!g_host->resolve_method||!g_host->resolve_class||!g_host->runtime_invoke||
            !g_host->object_unbox||!g_host->gchandle_new||!g_host->gchandle_free)
            return Unavailable("host capabilities");
        const auto game=GetModuleHandleW(L"GameAssembly.dll");
        if(!game)return Unavailable("runtime module");
        object_class=reinterpret_cast<decltype(object_class)>(GetProcAddress(game,"il2cpp_object_get_class"));
        assignable=reinterpret_cast<decltype(assignable)>(GetProcAddress(game,"il2cpp_class_is_assignable_from"));
        method_flags=reinterpret_cast<decltype(method_flags)>(GetProcAddress(game,"il2cpp_method_get_flags"));
        if(!object_class||!assignable||!method_flags)return Unavailable("type verification exports");
        for(int index=0;index<Count;++index) {
            BE_ResolvedMethodV1 resolved{};
            auto& method=methods[index];
            if(g_host->resolve_method(g_host->context,&method.descriptor,&resolved)!=BE_Result_Ok||
                !resolved.method_info)return Unavailable(method.key);
            uint32_t impl_flags=0;
            const bool is_static=(method_flags(resolved.method_info,&impl_flags)&0x10u)!=0;
            if(is_static!=(index==Fight||index==Player||index==MainCharacter))return Unavailable("method staticness mismatch");
            method.method_info=resolved.method_info;method.resolved=true;
        }
        const struct {const char* ns;const char* name;BE_ResolvedClassV1* result;} classes[]{
            {"Beyond.Gameplay.Core","Entity",&entity_class},
            {"Beyond.Gameplay.Core","PlayerController",&player_class},
            {"Beyond.Gameplay.Core","AbilitySystem",&ability_class}
        };
        for(const auto& item:classes)
            if(g_host->resolve_class(g_host->context,"Gameplay.Beyond.dll",item.ns,item.name,item.result)!=BE_Result_Ok||
                !item.result->class_info)return Unavailable("class metadata");
        ready=true;
        return true;
    }
    bool IsInstance(void* object,const BE_ResolvedClassV1& expected) const {
        return object && expected.class_info && assignable(expected.class_info,object_class(object));
    }
    bool Call(Method which,void* instance,void*& result) {
        result=nullptr;void* exception=nullptr;
        result=g_host->runtime_invoke(g_host->context,methods[which].method_info,instance,nullptr,&exception);
        return !exception;
    }
    bool ReadBool(Method which,void* instance,bool& value) {
        void* result=nullptr;
        return Call(which,instance,result)&&Unbox(result,value);
    }
    bool Sample(bool& active) {
        active=false;
        if(!Init())return false;
        bool fight=false;
        if(!ReadBool(Fight,nullptr,fight))return Unavailable("fight getter");
        if(fight){active=true;return true;}
        void* player_object=nullptr;
        if(!Call(Player,nullptr,player_object))return Unavailable("player getter");
        FpRoot player(player_object);
        if(!player||!IsInstance(player.object,player_class))return Unavailable("player missing or wrong type");
        bool casting=false;
        if(!ReadBool(Casting,player.object,casting))return Unavailable("attack getter");
        if(casting){active=true;return true;}
        void* entity_object=nullptr;
        if(!Call(MainCharacter,nullptr,entity_object))return Unavailable("main character getter");
        FpRoot entity(entity_object);
        if(!entity||!IsInstance(entity.object,entity_class))return Unavailable("main character missing or wrong type");
        void* ability_object=nullptr;
        if(!Call(Ability,entity.object,ability_object))return Unavailable("ability getter");
        if(!ability_object)return Unavailable("ability component missing");
        FpRoot ability(ability_object);
        if(!ability||!IsInstance(ability.object,ability_class))return Unavailable("ability wrong type");
        bool skill=false;
        if(!ReadBool(Skill,ability.object,skill))return Unavailable("skill getter");
        active=skill;
        return true;
    }
};
FpCombatSampler g_fp_combat;
bool FpCombatActive(bool& active) {return g_fp_combat.Sample(active);}
