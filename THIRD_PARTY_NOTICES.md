# Third-Party Notices

## Better Endfield (upstream project)

- Source: https://github.com/Dr-hydra/Better-Endfield
- License: GNU Affero General Public License v3.0 (the same license this
  repository continues to use; see `LICENSE`)
- Relationship: this repository is a derivative of that project and is
  **independently maintained**. It is not an official upstream version and does
  not represent the upstream author. The upstream code, documentation and design
  remain the work of their original authors — principally `Dr-hydra`, with every
  contributor recorded in the preserved upstream commit history.
- Used in: the repository as a whole. The upstream runtime, modules, UI,
  installer and documentation form the base of this work; the modifications
  introduced here are listed in `CHANGELOG.md` and described in `README.md`
  ("Upstream & Project Origin").

## EIEM (Importing Endfield MMD)

- Source: https://github.com/Sasye/EIEM
- License: GNU Affero General Public License v3.0 (the same license as this project)
- Used in: `native/modules/camera/free_camera_runtime.inc`, where the VMD camera
  sampling follows EIEM's `camera_player.h` (Bezier evaluation, Euler signs,
  180 degree basis, 0.07 scale and 5 degree FOV defaults) and `vmd_parser.h`
  (camera record layout).

## RenoDX Endfield Enhancer

- Source: https://github.com/ItsTheSewerRat/renodx, branch `endfield-enhancer`
  (path `src/games/endfield-enhancer/`). That branch extends the RenoDX HDR addon
  at https://github.com/clshortfuse/renodx.
- Author: `ItsTheSewerRat` ("ItsaRat"). RenoDX itself is by Carlos Lopez Jr. and
  contributors.
- License: MIT
- Used in: the first-person camera implementation under
  `native/modules/camera/`. Ported with names, namespaces and the surrounding
  platform layer adapted; the expressions, constants and evaluation order are
  kept as published.
  - `first_person_math.h` — ported from the upstream `camera_math.hpp`:
    `Vec3`/`Quat` and their operators, `Rotate`, `AxisAngle`, `ExpandLookPitch`,
    `LateralFacingYaw`, `Unit`, `BlendRotation`, `FacingRotation` and the
    constants they carry (0.382683432, 45, 89, 57.295779513).
  - `first_person_facing.h` and `first_person_facing_runtime.inc` — lateral
    interpolation and movement eligibility adapted from `camera_movement.hpp`.
  - `first_person_motion.h` and `first_person_motion_runtime.inc` — animation
    rotation decomposition and bind-pose axes adapted from `camera_motion.hpp`.
  - `first_person_mesh.h` — the part-role predicates `IsDedicatedHeadMesh` and
    `IsBodyMesh`, and the body-skin triangle test inside `Build`, are ported from
    the upstream `camera_mesh.hpp` (`s_actor_` / `_lod` / `shadowproxy` role
    rules, the `_face_`/`_hair_`/`_brow_`/`_eyebrow_`/`_iris_`/`_eyeshadow_`/
    `_hairshadow_` token list, and the half-weight body-skin rule).
  - `module.cpp` — the eye-anchor formula (a horizontal `planar` forward offset
    plus a world-vertical height offset, so looking down does not drag the eye
    downwards), the `first_person_eye_height` (0.05), `first_person_eye_forward`
    (0.03) and `first_person_near_clip` (0.03) defaults, and the
    `first_person_extend_look_range` multipliers (1.10 up / 1.50 down) come from
    the upstream `camera.hpp`.
