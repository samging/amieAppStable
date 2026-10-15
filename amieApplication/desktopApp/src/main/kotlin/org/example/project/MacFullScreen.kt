package org.example.project

import java.awt.Window
import java.lang.reflect.Proxy

/**
 * macOS native full screen for the window (the green-button kind: its own Space, the menu bar and the dock
 * away), reached through `com.apple.eawt` by reflection so the code compiles and runs everywhere; on another
 * system [toggle] does nothing and says so, and the caller fills the screen by hand instead. The window is
 * undecorated, so the system shows no button of its own: the one in the header asks for it.
 */
object MacFullScreen {
    private val isMac = System.getProperty("os.name").lowercase().contains("mac")

    /** Asks the system to enter or leave full screen; false when that is not possible here */
    fun toggle(window: Window): Boolean {
        if (!isMac) return false
        return runCatching {
            val utilities = Class.forName("com.apple.eawt.FullScreenUtilities")
            utilities.getMethod("setWindowCanFullScreen", Window::class.java, Boolean::class.javaPrimitiveType).invoke(null, window, true)
            val application = Class.forName("com.apple.eawt.Application")
            val instance = application.getMethod("getApplication").invoke(null)
            application.getMethod("requestToggleFullScreen", Window::class.java).invoke(instance, window)
            true
        }.getOrElse { false }
    }

    /** Tells [onChange] when the window enters or leaves full screen, however that happened (the button, Esc, the Mission Control gesture) */
    fun listen(window: Window, onChange: (fullScreen: Boolean) -> Unit): Boolean {
        if (!isMac) return false
        return runCatching {
            val utilities = Class.forName("com.apple.eawt.FullScreenUtilities")
            val listenerClass = Class.forName("com.apple.eawt.FullScreenListener")
            val listener = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) { proxy, method, args ->
                when (method.name) {
                    "windowEnteredFullScreen" -> { onChange(true); null }
                    "windowExitedFullScreen" -> { onChange(false); null }
                    "equals" -> proxy === args?.firstOrNull()
                    "hashCode" -> System.identityHashCode(proxy)
                    "toString" -> "MacFullScreen.listener"
                    else -> null
                }
            }
            utilities.getMethod("addFullScreenListenerTo", Window::class.java, listenerClass).invoke(null, window, listener)
            true
        }.getOrElse { false }
    }
}
