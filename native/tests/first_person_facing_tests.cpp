#include "../modules/camera/first_person_facing.h"
#include <cmath>
#include <iostream>
#include <limits>
#include <stdexcept>

using namespace BetterEndfield::FirstPerson;
void Check(bool value, const char* message) {
    if (!value) throw std::runtime_error(message);
}
bool Near(float a, float b) { return std::abs(a - b) < .001f; }
int main() {
    try {
        FacingState state;
        StepFacing(state, {true, 0, {0, 0, 0}, 60, .016f});
        auto result = StepFacing(state, {true, 50, {0, 0, 0}, 60, .016f});
        Check(result.mode == FacingMode::Entity && Near(result.yaw, 0),
            "standing side look within the limit must keep body yaw");
        result = StepFacing(state, {true, 80, {0, 0, 0}, 60, .016f});
        Check(Near(result.yaw, 20), "standing side look must turn only excess yaw");
        state = {};
        for (int frame = 0; frame < 8; ++frame)
            result = StepFacing(state, {true, 0, {1, 0, 0}, 60, .1f});
        Check(result.mode == FacingMode::Visual && Near(result.yaw, 45),
            "right strafe must converge to 45 degrees");
        for (int frame = 0; frame < 8; ++frame)
            result = StepFacing(state, {true, 0, {-1, 0, 0}, 60, .1f});
        Check(Near(result.yaw, -45), "strafe reversal must converge to -45 degrees");
        for (int frame = 0; frame < 8; ++frame)
            result = StepFacing(state, {true, 0, {0, 0, -1}, 60, .1f});
        Check(Near(result.yaw, 0), "pure backpedal must face the view");
        state = {};
        StepFacing(state, {true, 179, {}, 60, .016f});
        result = StepFacing(state, {true, -179, {}, 60, .016f});
        Check(Near(std::remainder(result.yaw - 179, 360.f), 0),
            "crossing the yaw wrap must not spin the body");
        FacingState slow, stalled;
        const auto ordinary = StepFacing(slow, {true, 0, {1, 0, 0}, 60, .1f});
        const auto bounded = StepFacing(stalled, {true, 0, {1, 0, 0}, 60, 30.f});
        Check(Near(ordinary.yaw, bounded.yaw), "a stalled frame must use the 100ms bound");
        result = StepFacing(state, {true, std::numeric_limits<float>::quiet_NaN(), {}, 60, .01f});
        Check(result.mode == FacingMode::Inactive && !state.attached,
            "invalid input must release the facing session");
        StepFacing(state, {true, 0, {1, 0, 0}, 60, .1f});
        result = StepFacing(state, {false, 0, {}, 60, .01f});
        Check(result.mode == FacingMode::Inactive && !state.attached,
            "disabled facing must release its state");
        const BetterEndfield::FirstPersonMath::Quat identity{0, 0, 0, 1};
        const auto external_change = BetterEndfield::FirstPersonMath::AxisAngle({0, 1, 0}, .2f);
        Check(SameOwnedRotation(identity, {-identity.x, -identity.y, -identity.z, -identity.w}),
            "equivalent quaternion signs must preserve ownership");
        Check(!SameOwnedRotation(identity, external_change),
            "even a small later animation rotation must prevent our restore");
        std::cout << "first_person_facing: thresholds, strafe, reversal, backpedal, wrap, time bounds and release passed\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << error.what() << '\n';
        return 1;
    }
}
