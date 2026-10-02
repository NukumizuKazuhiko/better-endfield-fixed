# 第一人称陀螺仪接入方案（2026-10-01）

> 依据：`CAMERA_FIRST_PERSON_LOOK_PROBE_RESULT_20261001.md` 的实机枚举结果。
> 该结果推翻了设计文档假设的控制器；本方案基于**实际存在的方法**重写。
> 文档状态：待用户确认，未动代码。

## 0. 前提（已实证，非推测）

| 事实 | 证据 |
|---|---|
| 入口类 = `Beyond.Gameplay.View.SnapshotCameraController` | 探针枚举，该控制器 35 个方法全 `:code` |
| `RotateCameraHorizontal/Vertical(float)` 存在且可调 | 同上 |
| `GetCameraRotation()->Vector2` 可回读朝向 | 同上 |
| 模块**已**持有该控制器实例 | `FindSnapshotCameraController()`（`module.cpp:984`） |
| 模块**不**持有第一人称相机朝向 | `ApplyFirstPersonState` 注释：orientation 原样保留游戏值 |
| 该类**没有**可发现的字段 | 28 个候选名全不中；运行时无 field 名 getter |
| 陀螺仪增量已经在往 `g_mouse_dx/dy` 里灌 | `GyroscopeController` → 输入中继 → `FoldPanelLookInput` |

关键推论：**陀螺仪链路本身是好的，缺的是第一人称侧的消费端**。

`g_mouse_dx/dy` 今天**只有一个消费者**——自由视角的 `g_free_target.yaw/pitch`（`free_camera_runtime.inc:772`）。第一人称路径从来没接过这个增量，所以陀螺仪只在自由视角下有反应。**自由视角的行为是对的，不改动**；要做的只是**新增**一条第一人称消费分支。

（此前本文写作「终点错了」，那是我理解偏了：自由视角本来就是好的，需求是把它**额外**接到第一人称。）

## 方案 A：增量注入 `RotateCamera*`（推荐）

### 思路

每帧读出陀螺仪增量 → 换算成角度 → 调用游戏自己的 `RotateCameraHorizontal/Vertical`。

### 数据流

```
GyroscopeController (Java)          陀螺仪角速度 → 像素当量增量
      ↓ 输入中继 (input.latch, "m dx dy")
DrainVirtualMouseDelta / FoldPanelLookInput
      ↓ g_mouse_dx / g_mouse_dy  (atomic，累加器)
      ↓ ★新增分支点★（二选一消费，不叠加）
   ┌── 自由视角开 → 现有路径 g_free_target.yaw/pitch（**一字不改**）
   └── 第一人称开 → RotateCameraHorizontal(dx*sens) / RotateCameraVertical(dy*sens)
```

### 具体改动

1. **`native/modules/camera/module.cpp`**
   - 新增两条方法合同：
     ```cpp
     {"snapshot.rotate_horizontal",
       {"Gameplay.Beyond.dll", "Beyond.Gameplay.View", "SnapshotCameraController",
        "RotateCameraHorizontal", "System.Void", "System.Single", 1}},
     {"snapshot.rotate_vertical",
       {…, "RotateCameraVertical", "System.Void", "System.Single", 1}},
     ```
   - 新增 `ApplyFirstPersonLook()`：在 `PumpFromEngineTick` 里、`FoldPanelLookInput()` 之后调用；**仅当第一人称激活且自由视角未激活**时 `exchange(0)` 取增量并 `InvokeVoid` 两次旋转。自由视角分支那一侧的代码保持原样。
   - 与自由视角互斥（二者互斥已是现状，见 `FpFacingUpdate(!g_free_camera_active && …)`）。
   - **顺带解除一处既有耦合**：`ModuleSettings.setCameraSettings()` 现在写着「陀螺仪开 ⇒ 强制打开自由视角」（`if (gyro.enabled()) { freeCamera = true; }`）。那是上一版把陀螺仪硬接到自由视角时的权宜，接到第一人称后必须去掉，否则开陀螺仪会顺带把自由视角也armed起来，两条消费路径同时抢 `g_mouse_dx/dy`。

2. **灵敏度换算**：陀螺仪现有灵敏度（`horizontal=1.83` / `vertical=1.84`）是像素/弧度当量，直接乘即为「度/帧」。**需要一个新的独立灵敏度上限夹取**，理由见风险 2。

3. **`Android` UI**：无明显改动；沿用现有水平/垂直灵敏度与两个反向开关。

### 优点

- **与玩家触摸输入天然共存**：走的是游戏自己的旋转方法，不是覆盖朝向。玩家摸屏幕转视角时，两者叠加，不会互相打架。
- **规避设计文档 §3.1 的警告**：不绕过身体跟随。身体跟随逻辑在旋转方法下游，仍照常生效。
- **不引入新状态**：不需要维护「陀螺仪绝对朝向」，无累积漂移、无退出时跳变。
- **失败可降级**：方法解析不到就整条路径不启用，不影响现有功能。

### 风险与对策

1. **`RotateCamera*` 的参数单位未知**。可能是「度/秒」（需乘 dt）也可能是「度/帧」。**对策**：先按「度/帧」实现并给保守默认，实机看到转速不对再调——这是唯一需要实机两轮标定的点。
2. **与原生灵敏度叠加**：现在 `dx*1.83` 直接当角度用，数值很可能过大。**对策**：第一人称陀螺仪走**独立换算系数**，不复用自由视角的 `g_mouse_sensitivity`。
3. **`RotateCamera*` 内部是否也做限位**：若它自带 pitch 夹取，我们的增量会被它裁掉（好事）；若不做，需要我们自己夹。**对策**：第一版不自己夹，观察是否出现翻转；出问题再加。
4. **每帧两次 Il2Cpp 调用**：开销可接受（同 tick 内已有若干次 Invoke）。

## 方案 B：绝对写入 `GetCameraRotation` + `SetCameraRotation`

### 思路

读当前朝向 → 加增量 → 写回绝对值。

### 缺点（明确不推荐）

- **与游戏自身 look 争抢**：每帧写绝对值会覆盖掉玩家的触摸输入，出现「一摸屏幕就被陀螺仪拽回去」。
- **需要维护模块侧的朝向真值**，引入漂移与退出跳变——正是设计文档 §3.1 警告的情形。
- `SetCameraRotation` 的 `bool` 参数语义未知（疑似「是否立即」或「是否相对」），不确认就用有踩空风险。

### 保留理由

若方案 A 的 `RotateCamera*` 实测发现是「设置目标而非增量」，则 B 是退路。

## 方案 C（否决）：折进 `CameraState`

设计文档 §3.1 已明确否决：绕过身体跟随、与瞄准输入抢朝向、退出时跳变。**不做。**

## 实施顺序

| 阶段 | 内容 | 验收 |
|---|---|---|
| 1 | 加两条方法合同 + `ApplyFirstPersonLook()`，`dx` 直接当角度、系数 1.0 | 实机开第一人称转手机，视角是否动 |
| 2 | 按实测手感标定单位（度/帧 vs 度/秒） | 转速与「跟手」程度 |
| 3 | 接入现有灵敏度/反向开关，双向验证 | 水平垂直方向、反向开关、灵敏度范围 |
| 4 | 与自由视角互斥、与触摸共存、关闭后无残留 | 切换无跳变 |
| 5 | 四件套文档同步 + 出包 | — |

## 待用户确认的三个决策点

1. **单位标定需要 1–2 轮实机**：接受吗？（这是唯一无法静态确定的量）
2. **灵敏度是否复用现有那对数值**：我倾向给第一人称一组独立默认（现值 1.83/1.84 大概率偏大），但这会让 UI 多两个滑杆。或者先复用、实测再拆。
3. **`SetCameraRotation` 的 bool 语义要不要现在补探**：可以再跑一轮探针，用不同参数调用并回读 `GetCameraRotation` 观察差异。**但会改动游戏相机状态**，需要在可弃存档上做。
