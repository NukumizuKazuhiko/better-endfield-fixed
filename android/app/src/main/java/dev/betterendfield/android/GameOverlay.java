package dev.betterendfield.android;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.util.function.Supplier;

/** Activity-scoped controller. Compose owns only the two visible touch surfaces. */
final class GameOverlay {
    private final Activity activity;
    private final FrameLayout host;
    private final View handle;
    private final View panel;
    private final OverlaySurface ui;
    private final boolean preview;
    private final Supplier<OverlayFeatures> features;
    private boolean closed;
    private float xFraction = 0.02f;
    private float yFraction = 0.28f;
    private OverlayFeatures shown = OverlayFeatures.off();
    private boolean bridgeMissing;
    private boolean removedByUs;
    private boolean compositionCreated;
    private final android.os.Handler mainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable reattachCheck = this::ensureOnTop;
    private final Runnable composeCheck = this::composeWhenAttached;

    static void install(Application app, ClassLoader loader, Supplier<OverlayFeatures> features) {
        RuntimeLog.record("overlay install: checking UnityPlayer");
        try {
            Class.forName("com.unity3d.player.UnityPlayer", false, loader);
        } catch (ClassNotFoundException notUnity) {
            RuntimeLog.record("overlay skipped: UnityPlayer class not found");
            return;
        }
        RuntimeLog.record("overlay install: UnityPlayer found, registering lifecycle");
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private final java.util.Map<Activity, GameOverlay> surfaces = new java.util.HashMap<>();

            @Override public void onActivityResumed(Activity activity) {
                // Make sure the save dialog's result can reach us even if this
                // activity overrides onActivityResult without calling super.
                XposedEntry.hookConcreteActivityResult(activity.getClass());
                OverlayFeatures current;
                try {
                    current = features.get();
                } catch (RuntimeException unavailable) {
                    current = OverlayFeatures.off();
                }
                GameOverlay surface = surfaces.get(activity);
                if (surface != null && surface.host.getParent() == null) {
                    // Game SDKs re-call setContentView on resume, which strips
                    // every child of the content view — including our host.
                    // A detached host can never become visible again; rebuild
                    // the surface on the current content view instead.
                    surfaces.remove(activity);
                    RuntimeLog.record("overlay host detached by setContentView; rebuilding");
                    // Disarm the discarded surface's re-attach watchdog so it
                    // cannot resurrect an orphaned host next to the new one.
                    surface.remove();
                    surface = null;
                }
                if (current.panel() && surface == null) {
                    try {
                        surface = new GameOverlay(activity, false, features);
                        surfaces.put(activity, surface);
                        RuntimeLog.record("overlay panel attached to "
                                + activity.getClass().getName());
                    } catch (Throwable error) {
                        RuntimeLog.record("overlay panel attach failed: " + error);
                        android.util.Log.e("BetterEndfield.Overlay", "Unable to attach panel", error);
                    }
                }
                if (surface == null) return;
                // A setting changed while the game was in the background has to
                // reach the controls, not just the panel's visibility.
                surface.ui.resumed();
                surface.refresh();
                surface.updateJournal();
                surface.host.setVisibility(
                        current.panel() && !surface.closed ? View.VISIBLE : View.GONE);
                RuntimeLog.record("activity resumed: host attached="
                        + (surface.host.getParent() != null));
                // The SDK may rebuild or bury the game view after this callback
                // returns; re-check the overlay's placement once the dust
                // settles rather than trusting this instant.
                surface.mainHandler.removeCallbacks(surface.reattachCheck);
                surface.mainHandler.postDelayed(surface.reattachCheck, 400);
                surface.mainHandler.postDelayed(surface.reattachCheck, 1500);
            }

            @Override public void onActivityPaused(Activity activity) {
                GameOverlay surface = surfaces.get(activity);
                if (surface == null) return;
                // Leaving a held movement key latched would keep the camera
                // drifting for as long as the game stays in the background.
                surface.releaseHeldKeys();
                surface.ui.paused();
                surface.host.setVisibility(View.GONE);
            }

            @Override public void onActivityDestroyed(Activity activity) {
                GameOverlay surface = surfaces.remove(activity);
                if (surface != null) {
                    surface.releaseHeldKeys();
                    surface.remove();
                }
            }

            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
        });
    }

    GameOverlay(Activity activity, boolean preview) {
        this(activity, preview, () -> OverlayFeatures.read(FrameworkSettings.open(activity)));
    }

    private GameOverlay(Activity activity, boolean preview, Supplier<OverlayFeatures> features) {
        this.activity = activity;
        this.preview = preview;
        this.features = features;
        host = new FrameLayout(activity);
        host.setClipChildren(false);
        host.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        host.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {
                mainHandler.post(composeCheck);
            }
            @Override public void onViewDetachedFromWindow(View v) {
                if (removedByUs) return;
                RuntimeLog.record("overlay host detached; re-attach scheduled");
                mainHandler.removeCallbacks(reattachCheck);
                mainHandler.postDelayed(reattachCheck, 400);
            }
        });
        host.setOnApplyWindowInsetsListener((view, insets) -> {
            host.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            host.post(this::layout);
            return insets;
        });
        ui = new OverlaySurface(activity, preview, new OverlaySurface.Callbacks() {
            @Override public void toggle() { togglePanel(); }
            @Override public void collapse() {
                if (preview) remove(); else collapsePanel();
            }
            @Override public void drag(float dx, float dy) { dragBy(dx, dy); }
            @Override public void pulse(int key, String description) {
                GameOverlay.this.pulse(key, description);
            }
            @Override public void hold(int key, boolean pressed, String description) {
                sendKey(key, pressed ? NativeCommandBridge.KEY_PRESS
                        : NativeCommandBridge.KEY_RELEASE, description);
            }
            @Override public void openSettings() { GameOverlay.this.openSettings(); }
            @Override public void saveLog() { saveJournalToFile(); }
            @Override public void refreshLog() { updateJournal(); }
            @Override public void copyLog() { copyJournal(); }
        });
        // ComposeView looks up owners through its parent when it attaches.
        // Unity's Activity provides none, so the host must own this tree.
        ui.installOwnersOn(host);
        handle = ui.getHandle();
        panel = ui.getPanel();
        host.addView(panel, new FrameLayout.LayoutParams(dp(320), -2));
        host.addView(handle, new FrameLayout.LayoutParams(dp(50), dp(50)));
        // A cold Activity may not have a window yet during onActivityResumed.
        // The host's attach callback composes after both children are attached.
        try {
            activity.addContentView(host, new ViewGroup.LayoutParams(-1, -1));
        } catch (Throwable failure) {
            removedByUs = true;
            if (host.getParent() instanceof ViewGroup) {
                ((ViewGroup) host.getParent()).removeView(host);
            }
            ui.dispose();
            if (failure instanceof Error) throw (Error) failure;
            if (failure instanceof RuntimeException) throw (RuntimeException) failure;
            throw new IllegalStateException("overlay composition failed", failure);
        }
        panel.setVisibility(preview ? View.VISIBLE : View.GONE);
        refresh();
        updateJournal();
        host.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or_, ob) -> layout());
        host.requestApplyInsets();
        host.post(this::layout);
    }

    private void composeWhenAttached() {
        if (closed || compositionCreated || !host.isAttachedToWindow()) return;
        try {
            ui.composeNow();
            compositionCreated = true;
            RuntimeLog.record("overlay Compose first composition created");
        } catch (Throwable failure) {
            RuntimeLog.record("overlay first composition failed: " + failure);
            android.util.Log.e("BetterEndfield.Overlay", "Unable to compose panel", failure);
            remove();
        }
    }

    private void refresh() {
        OverlayFeatures current;
        try { current = features.get(); }
        catch (RuntimeException unavailable) { current = OverlayFeatures.off(); }
        if (preview) {
            current = new OverlayFeatures(true, current.hideHud(), current.freeCamera(),
                    current.worldPause(), current.firstPerson(), current.vmdCamera());
        }
        if (current.equals(shown)) return;
        shown = current;
        ui.render(current);
        host.post(this::layout);
    }

    private void updateJournal() {
        String stamp = "构建 " + BuildConfig.VERSION_NAME
                + " (" + BuildConfig.VERSION_CODE + ") · 点按刷新 · 长按复制 · 下方按钮另存";
        String lines = RuntimeLog.tail(14);
        ui.renderJournal(lines.isEmpty()
                ? stamp + "\n（暂无记录）" : stamp + "\n" + lines.trim());
    }

    private void copyJournal() {
        String all = RuntimeLog.tail(150);
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) activity.getSystemService(
                        android.content.Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                    "BetterEndfield 运行日志", all.isEmpty() ? "(empty)" : all));
            toast("运行日志已复制（" + all.split("\n").length + " 行）");
        }
    }

    /** Request code for the system save dialog; must fit in the lower 16 bits. */
    private static final int REQUEST_SAVE_LOG = 0x1E10;
    /** The overlay surface waiting for the system save dialog to return. */
    private static GameOverlay saveRequester;
    /** Log text captured when the save dialog was opened. */
    private static String pendingSaveBody;

    /**
     * Opens the system save dialog (SAF ACTION_CREATE_DOCUMENT) so the user
     * picks where the log lands - Downloads, a cloud drive, anywhere. Needs no
     * storage permission. When the dialog cannot start or its result cannot
     * reach the hooked process, falls back to the share sheet instead.
     */
    private void saveJournalToFile() {
        pendingSaveBody = buildJournalBody();
        if (XposedEntry.activityResultRelayReady()) {
            try {
                String stamp = new java.text.SimpleDateFormat(
                        "yyyyMMdd-HHmmss", java.util.Locale.ROOT).format(new java.util.Date());
                Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                save.addCategory(Intent.CATEGORY_OPENABLE);
                save.setType("text/plain");
                save.putExtra(Intent.EXTRA_TITLE, "betterendfield-log-" + stamp + ".txt");
                saveRequester = this;
                XposedEntry.setActivityResultListener(GameOverlay::dispatchSaveResult);
                activity.startActivityForResult(save, REQUEST_SAVE_LOG);
                RuntimeLog.record("journal save: location picker opened");
                return;
            } catch (Throwable pickerFailed) {
                saveRequester = null;
                RuntimeLog.record("journal save picker unavailable: " + pickerFailed);
            }
        } else {
            RuntimeLog.record("journal save: activity result relay absent, sharing instead");
        }
        shareJournal(pendingSaveBody);
    }

    /** Receives the system save dialog's outcome and writes the log there. */
    private static void dispatchSaveResult(int requestCode, int resultCode, Intent data) {
        GameOverlay surface = saveRequester;
        if (surface == null || requestCode != REQUEST_SAVE_LOG) return;
        saveRequester = null;
        android.net.Uri target = resultCode == Activity.RESULT_OK && data != null
                ? data.getData() : null;
        if (target == null) {
            surface.toast("已取消保存");
            return;
        }
        String body = pendingSaveBody == null ? "" : pendingSaveBody;
        pendingSaveBody = null;
        new Thread(() -> {
            String outcome;
            try (java.io.OutputStream out = surface.activity.getContentResolver()
                    .openOutputStream(target)) {
                if (out == null) throw new java.io.IOException("provider returned no stream");
                out.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                outcome = "日志已保存到所选位置";
                RuntimeLog.record("journal saved: " + target);
            } catch (Exception error) {
                RuntimeLog.record("journal save failed: " + error);
                outcome = "日志保存失败: " + error.getMessage();
            }
            String message = outcome;
            surface.mainHandler.post(() -> surface.toast(message));
        }, "BetterEndfield-SaveJournal").start();
    }

    /**
     * Share-sheet fallback for devices where the save dialog cannot start or
     * its result cannot reach us: the plain-text log travels via ACTION_SEND,
     * from where any files app can store it wherever the user chooses. Needs
     * no storage permission and no FileProvider declaration in the host.
     */
    private void shareJournal(String body) {
        try {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "Better-Endfield 运行日志");
            send.putExtra(Intent.EXTRA_TEXT, body);
            activity.startActivity(Intent.createChooser(send, "分享运行日志"));
            RuntimeLog.record("journal share opened");
        } catch (Throwable shareFailed) {
            RuntimeLog.record("journal share failed: " + shareFailed);
            toast("日志保存失败: " + shareFailed.getMessage());
        }
    }

    /** Assembles the journal text: java ring plus the native log tail. */
    private String buildJournalBody() {
        String stamp = new java.text.SimpleDateFormat(
                "yyyyMMdd-HHmmss", java.util.Locale.ROOT).format(new java.util.Date());
        StringBuilder nativeTail = new StringBuilder();
        try {
            NativeCommandBridge.tailNativeLog(nativeTail, new long[]{0L});
        } catch (RuntimeException unavailable) {
            nativeTail.append("(native journal unavailable: ").append(unavailable).append(')');
        }
        String nativeText = nativeTail.toString();
        if (nativeText.length() > 40000) nativeText = nativeText.substring(nativeText.length() - 40000);
        return new StringBuilder()
                .append("Better-Endfield journal build ")
                .append(BuildConfig.VERSION_NAME).append(" (")
                .append(BuildConfig.VERSION_CODE).append(")\n")
                .append("saved at ").append(stamp).append("\n\n")
                .append("---- java journal (newest last) ----\n")
                .append(RuntimeLog.tail(150).trim()).append("\n\n")
                .append("---- native journal (newest last) ----\n")
                .append(nativeText.trim()).append('\n')
                .toString();
    }

    // ------------------------------------------------------------------ actions

    private void pulse(int virtualKey, String title) {
        sendKey(virtualKey, NativeCommandBridge.KEY_PULSE, title);
    }

    private void sendKey(int virtualKey, int action, String description) {
        if (preview) {
            if (action != NativeCommandBridge.KEY_RELEASE) {
                toast("预览模式：不会发送「" + description + "」");
            }
            return;
        }
        try {
            // False before RuntimeBootstrap.configure() opened the file relay,
            // i.e. while the native runtime cannot have loaded yet. Once the
            // channel exists every event is delivered; the native relay latches
            // it (presses held, pulses expiring) whenever it starts.
            if (!NativeCommandBridge.key(virtualKey, action)) {
                RuntimeLog.record("key rejected (relay not configured): " + description
                        + " (vk " + Integer.toHexString(virtualKey) + ")");
                if (!bridgeMissing) {
                    bridgeMissing = true;
                    toast("增强运行时尚未载入，请稍后重试");
                }
            } else if (!keyPathProven) {
                keyPathProven = true;
                bridgeMissing = false;
                RuntimeLog.record("key send ok; file relay active ("
                        + description + ", vk " + Integer.toHexString(virtualKey) + ")");
            }
        } catch (Throwable failure) {
            RuntimeLog.record("key " + description + " failed: " + failure);
            if (!bridgeMissing) {
                bridgeMissing = true;
                toast("按键发送失败，请稍后重试");
            }
        }
    }

    private static boolean keyPathProven;

    private void releaseHeldKeys() {
        if (preview) return;
        try {
            NativeCommandBridge.releaseKeys();
        } catch (UnsatisfiedLinkError | NoSuchMethodError ignored) {
            // Nothing is latched if the runtime was never loaded.
        }
    }

    private void openSettings() {
        Intent intent = new Intent()
                .setClassName(RuntimeBootstrap.MODULE_PACKAGE,
                        RuntimeBootstrap.MODULE_PACKAGE + ".MainActivity")
                .putExtra(MainActivity.EXTRA_PAGE, "enhancement")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(intent);
        } catch (RuntimeException unavailable) {
            toast("无法打开体验设置");
        }
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    // Position both real touch surfaces within the Activity's inset content.
    // The rest of the full-screen FrameLayout has no listener and passes events through.
    private void layout() {
        if (closed || host.getWidth() == 0) return;
        int left = host.getPaddingLeft() + dp(8);
        int top = host.getPaddingTop() + dp(8);
        int width = Math.max(1, host.getWidth() - left - host.getPaddingRight() - dp(8));
        int height = Math.max(1, host.getHeight() - top - host.getPaddingBottom() - dp(8));
        handle.setX(left + xFraction * Math.max(0, width - dp(50)));
        handle.setY(top + yFraction * Math.max(0, height - dp(50)));
        int panelWidth = Math.min(dp(320), width);
        panel.measure(View.MeasureSpec.makeMeasureSpec(panelWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
        int panelHeight = Math.min(height, panel.getMeasuredHeight());
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) panel.getLayoutParams();
        if (params.width != panelWidth || params.height != panelHeight) {
            params.width = panelWidth;
            params.height = panelHeight;
            panel.setLayoutParams(params);
        }
        float beside = handle.getX() + dp(58);
        if (beside + panelWidth > left + width) beside = handle.getX() - panelWidth - dp(8);
        panel.setX(Math.max(left, Math.min(beside, left + width - panelWidth)));
        panel.setY(Math.max(top, Math.min(handle.getY(), top + height - panelHeight)));
        handle.bringToFront();
    }

    private void dragBy(float dx, float dy) {
        int left = host.getPaddingLeft() + dp(8);
        int top = host.getPaddingTop() + dp(8);
        int w = Math.max(1, host.getWidth() - left - host.getPaddingRight() - dp(58));
        int h = Math.max(1, host.getHeight() - top - host.getPaddingBottom() - dp(58));
        xFraction = clamp((handle.getX() + dx - left) / w);
        yFraction = clamp((handle.getY() + dy - top) / h);
        layout();
    }

    private void togglePanel() {
        if (panel.getVisibility() == View.VISIBLE) { collapsePanel(); return; }
        refresh();
        updateJournal();
        panel.setAlpha(0f);
        panel.setVisibility(View.VISIBLE);
        layout();
        panel.animate().alpha(1f).setDuration(140).start();
    }

    private void collapsePanel() {
        releaseHeldKeys();
        panel.animate().alpha(0f).setDuration(100)
                .withEndAction(() -> panel.setVisibility(View.GONE)).start();
    }

    void remove() {
        if (closed) return;
        removedByUs = true;
        mainHandler.removeCallbacks(reattachCheck);
        mainHandler.removeCallbacks(composeCheck);
        closed = true;
        releaseHeldKeys();
        ui.dispose();
        if (host.getParent() instanceof ViewGroup) {
            ((ViewGroup) host.getParent()).removeView(host);
        }
    }

    private void ensureOnTop() {
        if (removedByUs || closed || activity.isDestroyed() || activity.isFinishing()) return;
        android.view.ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        if (host.getParent() == content) {
            // setContentView-style rebuilds can leave our host attached but
            // underneath the freshly added game view; the detach watchdog
            // never fires because the host was never detached. Only the
            // z-order tells the truth.
            if (content.getChildAt(content.getChildCount() - 1) != host) {
                content.bringChildToFront(host);
                RuntimeLog.record("overlay host re-raised above the game view");
            }
            return;
        }
        if (host.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) host.getParent()).removeView(host);
        }
        try {
            content.addView(host, new ViewGroup.LayoutParams(-1, -1));
            RuntimeLog.record("overlay host re-added to the content view");
        } catch (RuntimeException error) {
            RuntimeLog.record("overlay re-attach failed: " + error);
        }
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
