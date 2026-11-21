package com.example

import android.app.Application
import android.content.Context

/** يلتقط الانهيارات المبكرة (قبل MainActivity، مثل تهيئة مكتبة الإعلانات) ويحفظها لعرضها عند التشغيل التالي. */
class TropicalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.analytics.Track.init(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            val prefs = base.getSharedPreferences("crash_report", MODE_PRIVATE)
            val prev = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                try {
                    val full = android.util.Log.getStackTraceString(e)
                    val i = full.lastIndexOf("Caused by")
                    val s = if (i > 0) full.take(900) + "\n...\n" + full.substring(i).take(1800) else full.take(2500)
                    prefs.edit().putString("trace", s).commit()
                } catch (ex: Throwable) { }
                prev?.uncaughtException(t, e)
            }
        } catch (e: Throwable) { }
    }
}
