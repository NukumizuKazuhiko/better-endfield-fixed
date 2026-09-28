#include "../modules/camera/first_person_policy.h"
#include <iostream>
using namespace BetterEndfield::FirstPerson;
int main() {
    PolicyState state;
    PolicyInput input;
    input.requested=true;
    if (!StepPolicy(state,input).apply) return 1;
    input.yield_dialogue=true;
    if (StepPolicy(state,input).reason != Suppression::DialogueUnavailable) return 2;
    input.dialogue_known=true; input.dialogue=true;
    if (StepPolicy(state,input).reason != Suppression::Dialogue) return 3;
    input.dialogue=false;
    if (!StepPolicy(state,input).apply) return 4;
    input.third_person_in_combat=true;
    if (StepPolicy(state,input).reason != Suppression::CombatUnavailable) return 5;
    input.combat_known=true; input.combat=true; input.return_delay=.5f;
    if (StepPolicy(state,input).reason != Suppression::Combat) return 6;
    input.combat=false; input.elapsed=.1f;
    if (StepPolicy(state,input).reason != Suppression::CombatCooldown) return 7;
    for (int i=0;i<5;++i) StepPolicy(state,input);
    if (!StepPolicy(state,input).apply) return 8;
    input.combat=true; StepPolicy(state,input);
    input.requested=false; StepPolicy(state,input);
    input.requested=true; input.combat=false;
    if (!StepPolicy(state,input).apply) return 9;
    std::cout << "first_person_policy: dialogue, combat, unavailable state, cooldown and exit reset passed\n";
}
