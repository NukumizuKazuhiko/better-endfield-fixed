#pragma once
#include <algorithm>
#include <cmath>

namespace BetterEndfield::FirstPerson {
enum class Suppression { None, NotRequested, DialogueUnavailable, Dialogue,
    CombatUnavailable, Combat, CombatCooldown };
struct PolicyInput {
    bool requested = false;
    bool yield_dialogue = false, dialogue_known = false, dialogue = false;
    bool third_person_in_combat = false, combat_known = false, combat = false;
    float elapsed = 0, return_delay = .5f;
};
struct PolicyState { float remaining = 0; };
struct PolicyDecision { bool apply = false; Suppression reason = Suppression::NotRequested; };
inline PolicyDecision StepPolicy(PolicyState& state, const PolicyInput& input) {
    if (!input.requested) { state = {}; return {}; }
    const float elapsed = std::isfinite(input.elapsed) ? std::clamp(input.elapsed,0.f,.1f) : 0.f;
    const float delay = std::isfinite(input.return_delay) ? std::clamp(input.return_delay,0.f,10.f) : .5f;
    if (!input.third_person_in_combat) state.remaining = 0;
    else if (input.combat_known) {
        if (input.combat) state.remaining = delay;
        else state.remaining = std::max(0.f,state.remaining-elapsed);
    }
    if (input.yield_dialogue) {
        if (!input.dialogue_known) return {false,Suppression::DialogueUnavailable};
        if (input.dialogue) return {false,Suppression::Dialogue};
    }
    if (input.third_person_in_combat) {
        if (!input.combat_known) return {false,Suppression::CombatUnavailable};
        if (input.combat) return {false,Suppression::Combat};
        if (state.remaining > .0001f) return {false,Suppression::CombatCooldown};
    }
    return {true,Suppression::None};
}
} // namespace BetterEndfield::FirstPerson
