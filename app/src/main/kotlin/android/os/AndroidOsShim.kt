package android.os

object SystemClock {
    private val startNano = System.nanoTime()
    private var overrideElapsedMs: Long? = null

    fun elapsedRealtime(): Long {
        overrideElapsedMs?.let { return it }
        return (System.nanoTime() - startNano) / 1_000_000L
    }

    fun setCurrentTimeMillis(millis: Long): Boolean {
        overrideElapsedMs = millis
        return true
    }
}

class Bundle {
    private val map = mutableMapOf<String, Any?>()

    fun putString(key: String, value: String?) {
        map[key] = value
    }

    fun getString(key: String): String? = map[key] as? String

    fun putParcelable(key: String, value: Any?) {
        map[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getParcelable(key: String): T? = map[key] as? T
}


object Build {
    object VERSION {
        const val SDK_INT: Int = 35
    }

    object VERSION_CODES {
        const val TIRAMISU: Int = 33
    }
}

class PowerManager {
    inner class WakeLock {
        var isHeld: Boolean = false
            private set

        fun acquire(timeout: Long = 0L) {
            isHeld = true
        }

        fun release() {
            isHeld = false
        }
    }

    fun newWakeLock(levelAndFlags: Int, tag: String): WakeLock = WakeLock()

    companion object {
        const val PARTIAL_WAKE_LOCK: Int = 1
    }
}

object Looper {
    fun getMainLooper(): Looper = this

    fun idle() {
        try {
            if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
                javax.swing.SwingUtilities.invokeAndWait {}
            }
        } catch (_: Exception) {
        }
    }
}
