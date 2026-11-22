package com.example.analytics

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/** تسجيل أحداث اللعب (Firebase Analytics) + مفاتيح مخصصة لتقارير الأعطال. كل الدوال آمنة (لا تُسقط اللعبة أبدًا). */
object Track {
    private var fa: FirebaseAnalytics? = null

    fun init(app: Application) {
        try {
            fa = FirebaseAnalytics.getInstance(app)
            val installer = installerOf(app)
            fa?.setUserProperty("installer", (installer ?: "sideload").takeLast(36))
        } catch (e: Throwable) { }
    }

    @Suppress("DEPRECATION")
    private fun installerOf(app: Application): String? = try {
        if (android.os.Build.VERSION.SDK_INT >= 30) app.packageManager.getInstallSourceInfo(app.packageName).installingPackageName
        else app.packageManager.getInstallerPackageName(app.packageName)
    } catch (e: Throwable) { null }

    private fun log(name: String, vararg kv: Pair<String, Any>) {
        try {
            val b = Bundle()
            for ((k, v) in kv) when (v) { is Int -> b.putLong(k, v.toLong()); is Long -> b.putLong(k, v); is String -> b.putString(k, v); else -> b.putString(k, v.toString()) }
            fa?.logEvent(name, b)
        } catch (e: Throwable) { }
    }

    private fun key(k: String, v: Int) { try { FirebaseCrashlytics.getInstance().setCustomKey(k, v) } catch (e: Throwable) { } }

    // ───────── المستويات العادية ─────────
    private val tries = HashMap<Int, Int>(); private val rtries = HashMap<Int, Int>()
    fun levelStart(level: Int) { val n = (tries[level] ?: 0) + 1; tries[level] = n; key("level", level); log("lvl_start", "level" to level, "attempt" to n) }
    fun levelWin(level: Int, stars: Int, score: Int, movesLeft: Int) { log("lvl_win", "level" to level, "stars" to stars, "score" to score, "moves_left" to movesLeft); com.example.cloud.CloudSave.requestPush() }
    fun levelLose(level: Int, score: Int, target: Int, fruitsLeft: Int) =
        log("lvl_lose", "level" to level, "score_pct" to (if (target > 0) (score * 100 / target).coerceAtMost(100) else 0), "fruits_left" to fruitsLeft)
    /** خرج اللاعب من المستوى أثناء اللعب (رجوع للخريطة/القائمة) */
    fun levelQuit(level: Int, movesLeft: Int, score: Int, target: Int) =
        log("lvl_quit", "level" to level, "moves_left" to movesLeft, "score_pct" to (if (target > 0) (score * 100 / target).coerceAtMost(100) else 0))
    fun levelRestart(level: Int) = log("lvl_restart", "level" to level)
    fun levelRevive(level: Int) = log("lvl_revive_ad", "level" to level)

    // ───────── إنقاذ الملكة ─────────
    fun rescueStart(id: Int) { val n = (rtries[id] ?: 0) + 1; rtries[id] = n; key("rescue", id); log("rescue_start", "level" to id, "attempt" to n) }
    fun rescueWin(id: Int, stars: Int, movesLeft: Int) { log("rescue_win", "level" to id, "stars" to stars, "moves_left" to movesLeft); com.example.cloud.CloudSave.requestPush() }
    fun rescueLose(id: Int, movesLeft: Int) = log("rescue_lose", "level" to id, "moves_left" to movesLeft)
    fun rescueRevive(id: Int) = log("rescue_revive_ad", "level" to id)
}
