# 第一人称视角入口探测结果（2026-10-01）

设备：PJX110 / Android 16 / 未 root。探针：`betterendfield.fp_look_probe`（只读枚举，alpha.13 引入，alpha.15 起由设置项 `debug_first_person_look_probe` 开启）。

## 结论

`Beyond.Gameplay.View.SnapshotCameraController` 就是第一人称视角的入口，**原生已经提供了完整的视角读写 API，且全部有编译体**（探针输出 `:code`，非 `:stub`）。

设计文档假设的 `FirstPersonLookInput` / `localYaw` / `soft_limit` / `hard_limit` **全部不存在**（全仓 0 命中，字段探测也确认为 0）。文档 §2/§7 基于一个不存在的控制器编写，不可再作为实现依据。

## 可用方法（该控制器 35 个，按用途归类）

### 视角旋转（增量注入的首选）

| 方法 | 签名 | 说明 |
|---|---|---|
| `RotateCameraHorizontal` | `(System.Single) -> System.Void` | 水平增量旋转 |
| `RotateCameraVertical` | `(System.Single) -> System.Void` | 垂直增量旋转 |
| `OnInput` | `(System.Single, System.Single) -> System.Void` | 疑似 `(deltaX, deltaY)` 原生输入入口 |

### 视角读写（绝对值，用于叠加与回读）

| 方法 | 签名 |
|---|---|
| `GetCameraRotation` | `() -> UnityEngine.Vector2` |
| `SetCameraRotation` | `(UnityEngine.Vector3, System.Boolean) -> System.Void` |
| `SetCameraRotation` | `(UnityEngine.Vector2, System.Boolean) -> System.Void` |
| `GetCameraRoll` | `() -> System.Single` |
| `SetCameraRoll` | `(System.Single) -> System.Void` |

`SetCameraRotation` 有**两个重载**，按名解析会歧义，必须用 `ResolveMethodExact` 指定参数类型。

### 第一人称与角色显示

- `get_isFirstPerson() -> System.Boolean` / `set_isFirstPerson(System.Boolean) -> System.Void`（属性访问器是一对，`get` 无参、`set` 带一个 `System.Boolean`；按名解析需用 `ResolveMethodExact` 区分，否则两者同名同参数个数时歧义）
- `SetFirstPerson(System.Boolean) -> System.Void`
- `_CreateFirstPersonCCS() -> System.Void`
- `_ClearFirstPersonStatus() -> System.Void`
- `_HideChar()` / `_ShowChar()`

### 相机参数与状态快照

- `GetCameraOffset() -> Vector3` / `SetCameraOffset(Vector3)` / `AddCameraOffset(Vector3, System.Single)`
- `SetAdditiveFOV(System.Single)`
- `GetZoomScale() -> System.Single` / `SetZoomScale(System.Single)`
- `GetCameraParamFullSnapshot() -> CameraControlParamFullSnapshot`
- `RestoreCameraParamFullSnapshot(CameraControlParamFullSnapshot)`
- `SetAperture(System.Single)` / `SetFocusDistance(System.Single)`
- `ResetToInitialParam()` / `OnInit()` / `OnActivate()`
- `ActivateSnapshotCamera()` / `DeactivateSnapshotCamera()`

## 字段：无可用结果

28 个候选字段名（`lookSensitivity` / `yaw` / `pitch` / `lookDelta` / `aimAxis` 等）在 `SnapshotCameraController` 与 `PlayerController` 上**全部不存在**（`present: (none)`）。

这不是探针故障：Android 运行时**没有接字段名 getter**，探针只能按名字逐个询问。既然一个都不中，**字段面到此为止，不应再猜偏移**——硬猜等于伪造证据。行为面（方法）已经足够。

## 为什么这是决定性的

在探针之前，唯一已知的写入面是：

- `FpFacingUpdate` —— 写的是**骨骼根**（身体），不是相机朝向
- `Axes`（`PlayerController.get_moveAxis`）—— 是**移动**输入，不是 look
- `ApplyFirstPersonState` —— 注释明确「orientation 原样保留游戏给出的值」，只改眼位

也就是说模块**不持有第一人称相机朝向**，也没有任何地方消费陀螺仪塞进 `g_mouse_dx/dy` 的增量——该增量的唯一消费者是自由视角（`free_camera_runtime.inc:772`）。现在 `RotateCameraHorizontal/Vertical` + `GetCameraRotation` 提供了完整闭环：施加增量（必要时回读校验）。**自由视角侧无需任何改动，缺的只是第一人称侧新增一条消费分支。**

## 设计影响

1. **增量注入是唯一不破坏自由视角与触摸共存的做法**：用 `RotateCamera*` 施加增量，天然与玩家的触摸输入叠加、不会互相覆盖。绝对 `SetCameraRotation` 会与游戏自身的 look 争抢。
2. 这同时规避了设计文档 §3.1 警告的问题（把增量直接折进 `CameraState` 会绕过身体跟随、与瞄准输入抢朝向、退出跳变）——因为现在走的是游戏自己的旋转方法，身体跟随逻辑仍在其下游生效；**且自由视角路径完全不动**。
3. `SetCameraRotation` 的 `System.Boolean` 第二参数语义未知（疑似「是否立即/是否相对」），实现前需再探或谨慎默认。

## 探针自身的两个缺陷（已在 alpha.16 修复方向中处理）

1. **logcat 单行约 4 KB 截断**：首轮 `DescribeClass` 整类打一行，被砍在 `RotateCameraVertical`，**后面 25 个方法无声丢失**。改为按 `" | "` 边界切 1000 字符分片后，41 个方法全出。教训：整类 dump 打一行必然丢尾部，且丢得没有任何提示。
2. `fields` 原先同时打印 present 与 absent 两份长列表；absent 在本例中信息量为零（全 absent），已改为只打 present。

## 部署要点（供复用）

- 设备**未 root**：`su -c` 不存在，`setprop` 对 release 包无效（`BuildConfig.DEBUG` 被 R8 折掉，属性名在 release dex 中 0 命中）。**设置项是唯一通道**。
- **横屏时 `uiautomator dump` 读不到设置页内容、点击会落到游戏上**——调试 UI 前先确认方向。
- debug 包可 `adb install -r` 直接覆盖 release 包（证书不同也成功），且 `run-as` 可用，能直接读 `shared_prefs/module_settings.xml` 核对设置。
