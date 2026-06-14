package com.voicerecorder.presentation.ui.crash

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTraceString = sw.toString()

            Log.e("CrashHandler", "Uncaught exception caught: $stackTraceString")

            // Launch the CrashActivity
            val intent =
                Intent(context, CrashActivity::class.java).apply {
                    putExtra(CrashActivity.EXTRA_STACK_TRACE, stackTraceString)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error inside exception handler", e)
            defaultHandler?.uncaughtException(thread, throwable)
        } finally {
            // Kill the current crashed process cleanly
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    companion object {
        fun initialize(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val crashHandler = CrashHandler(context, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }
    }
}
