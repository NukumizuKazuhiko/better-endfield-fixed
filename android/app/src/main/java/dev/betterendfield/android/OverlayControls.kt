package dev.betterendfield.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun OverlaySection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = Be.Colors.textSecondary, fontSize = 11.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 2.dp))
        content()
    }
}

@Composable
internal fun OverlayAction(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.fillMaxWidth().height(42.dp)
            .background(Be.Colors.overlayRow, RoundedCornerShape(11.dp))
            .border(1.dp, Be.Colors.outline, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = title },
        contentAlignment = Alignment.Center,
    ) {
        Text(title, color = Be.Colors.textPrimary, fontSize = 13.sp,
            fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun OverlayActionRow(
    first: String, firstClick: () -> Unit,
    second: String, secondClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        OverlayAction(first, Modifier.weight(1f), firstClick)
        OverlayAction(second, Modifier.weight(1f), secondClick)
    }
}

/** Sends exactly one press and one release, including pointer cancellation. */
@Composable
internal fun HoldControl(
    title: String,
    key: Int,
    description: String,
    modifier: Modifier = Modifier,
    callbacks: OverlaySurface.Callbacks,
) {
    Box(
        modifier.height(43.dp)
            .background(Be.Colors.overlayRow, RoundedCornerShape(11.dp))
            .border(1.dp, Be.Colors.outline, RoundedCornerShape(11.dp))
            .pointerInput(key) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    callbacks.hold(key, true, description)
                    try { waitForUpOrCancellation() }
                    finally { callbacks.hold(key, false, description) }
                }
            }
            .semantics { contentDescription = "$description，按住生效" },
        contentAlignment = Alignment.Center,
    ) {
        Text(title, color = Be.Colors.textPrimary, fontSize = 16.sp)
    }
}

@Composable
internal fun MovementPad(callbacks: OverlaySurface.Callbacks) {
    OverlaySection("镜头移动（按住）") {
        Column(
            Modifier.fillMaxWidth().background(Be.Colors.overlayField, RoundedCornerShape(12.dp))
                .border(1.dp, Be.Colors.outline, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HoldControl("◀", Hotkeys.MOVE_LEFT, "左移", Modifier.weight(1f), callbacks)
                HoldControl("▲", Hotkeys.MOVE_FORWARD, "前进", Modifier.weight(1f), callbacks)
                HoldControl("▼", Hotkeys.MOVE_BACK, "后退", Modifier.weight(1f), callbacks)
                HoldControl("▶", Hotkeys.MOVE_RIGHT, "右移", Modifier.weight(1f), callbacks)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HoldControl("上升", Hotkeys.MOVE_UP, "上升", Modifier.weight(1f), callbacks)
                HoldControl("下降", Hotkeys.MOVE_DOWN, "下降", Modifier.weight(1f), callbacks)
            }
        }
    }
}

@Composable
internal fun MotionControls(callbacks: OverlaySurface.Callbacks) {
    OverlaySection("运镜 / 关键帧") {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OverlayActionRow("播放 / 停止", { callbacks.pulse(Hotkeys.MOTION, "运镜 播放/停止") },
                "视角回正", { callbacks.pulse(Hotkeys.VIEW_RESET, "视角回正") })
            OverlayActionRow("广角 +", { callbacks.pulse(Hotkeys.FOV_WIDE, "广角 +") },
                "长焦 +", { callbacks.pulse(Hotkeys.FOV_NARROW, "长焦 +") })
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HoldControl("滚转 ↺", Hotkeys.ROLL_LEFT, "逆时针滚转", Modifier.weight(1f), callbacks)
                HoldControl("滚转 ↻", Hotkeys.ROLL_RIGHT, "顺时针滚转", Modifier.weight(1f), callbacks)
            }
            OverlayAction("记录关键帧") { callbacks.pulse(Hotkeys.KEYFRAME_ADD, "记录关键帧") }
            OverlayActionRow("回放关键帧", { callbacks.pulse(Hotkeys.KEYFRAME_PLAY, "回放关键帧") },
                "清除", { callbacks.pulse(Hotkeys.KEYFRAME_CLEAR, "清除关键帧") })
        }
    }
}
