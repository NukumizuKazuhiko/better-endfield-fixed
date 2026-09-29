package dev.betterendfield.android

import android.app.Activity
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/** Compose state and view ownership for the existing Java game-process controller. */
class OverlaySurface(
    activity: Activity,
    private val preview: Boolean,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun toggle()
        fun collapse()
        fun drag(dx: Float, dy: Float)
        fun pulse(key: Int, description: String)
        fun hold(key: Int, pressed: Boolean, description: String)
        fun openSettings()
        fun saveLog()
        fun refreshLog()
        fun copyLog()
    }

    private val owner = OverlayOwner()
    private var features by mutableStateOf(OverlayFeatures.off())
    private var journal by mutableStateOf("")
    private var compositionInitialized = false

    val handle: View = ComposeView(activity).apply {
        setViewTreeOwners(this)
    }
    val panel: View = ComposeView(activity).apply {
        setViewTreeOwners(this)
    }

    private fun setViewTreeOwners(view: View) {
        OverlayViewOwners.set(view, owner, owner, owner)
    }

    fun installOwnersOn(host: View) {
        setViewTreeOwners(host)
    }

    fun render(next: OverlayFeatures) { features = next }
    fun renderJournal(next: String) { journal = next }
    fun composeNow() {
        if (compositionInitialized) return
        check(handle.isAttachedToWindow && panel.isAttachedToWindow)
        (handle as ComposeView).setContent {
            OverlayTheme { FloatingHandle(callbacks::toggle, callbacks::drag) }
        }
        (panel as ComposeView).setContent {
            OverlayTheme { OverlayPanel(features, preview, journal, callbacks) }
        }
        (handle as ComposeView).createComposition()
        (panel as ComposeView).createComposition()
        compositionInitialized = true
    }
    fun resumed() { owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
    fun paused() { owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }
    fun dispose() {
        (handle as ComposeView).disposeComposition()
        (panel as ComposeView).disposeComposition()
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        owner.viewModelStore.clear()
    }

    private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        override val lifecycle: Lifecycle get() = registry
        val registry = LifecycleRegistry(this)
        override val viewModelStore = ViewModelStore()
        private val savedState = SavedStateRegistryController.create(this)
        override val savedStateRegistry get() = savedState.savedStateRegistry

        init {
            savedState.performAttach()
            savedState.performRestore(null)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }
}
