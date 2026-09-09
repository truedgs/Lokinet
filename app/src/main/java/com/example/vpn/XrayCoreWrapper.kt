package com.example.vpn

import android.util.Log
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Direct (compile-time) binding to libv2ray.aar (AndroidLibXrayLite / v2rayNG core).
 *
 * IMPORTANT: this file previously used java.lang.reflect to call into the core.
 * That reflection code called the WRONG method signatures:
 *   - newCoreController(String)        -> actual signature is newCoreController(CoreCallbackHandler)
 *   - startLoop(String, Long)          -> actual signature is startLoop(String, Int)
 * Both calls silently failed with NoSuchMethodException (caught and logged only),
 * so Xray core never actually started even when the .aar was present. Traffic never
 * reached the tunnel, and the OS eventually tore the VPN interface down.
 *
 * Binding directly against the .aar's real Kotlin/Java API catches signature
 * mismatches at COMPILE time instead of silently failing at runtime.
 */
object XrayCoreWrapper {

    private const val TAG = "XrayCoreWrapper"
    private var coreController: CoreController? = null

    /** Simple callback handler required by libv2ray. Logs core lifecycle events. */
    private val callbackHandler = object : CoreCallbackHandler {
        override fun startup(): Long {
            Log.i(TAG, "Xray core startup() callback")
            return 0L
        }

        override fun shutdown(): Long {
            Log.i(TAG, "Xray core shutdown() callback")
            return 0L
        }

        override fun onEmitStatus(l: Long, s: String?): Long {
            Log.d(TAG, "Xray core status: $s ($l)")
            return 0L
        }
    }

    fun initCoreEnv(envPath: String) {
        try {
            Libv2ray.initCoreEnv(envPath, "")
            Log.i(TAG, "Xray core environment initialized at $envPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init Xray core environment", e)
        }
    }

    /**
     * Starts the Xray routing loop.
     * @param configContent The generated JSON configuration for Xray.
     * @param tunFd The RAW file descriptor int of the Android VpnService TUN interface.
     */
    fun startLoop(configContent: String, tunFd: Int): Boolean {
        return try {
            val controller = coreController ?: Libv2ray.newCoreController(callbackHandler).also {
                coreController = it
            }
            controller.startLoop(configContent, tunFd)
            Log.i(TAG, "Xray core loop started, isRunning=${controller.isRunning}")
            controller.isRunning
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray core loop", e)
            false
        }
    }

    fun stopLoop() {
        try {
            coreController?.stopLoop()
            Log.i(TAG, "Xray core loop stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Xray core loop", e)
        } finally {
            coreController = null
        }
    }

    fun isRunning(): Boolean = coreController?.isRunning ?: false
}
