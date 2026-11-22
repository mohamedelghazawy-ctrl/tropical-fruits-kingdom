package com.example.cloud

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * حفظ التقدم على السحابة عبر "ألعاب Google Play" (Saved Games).
 * - عند التشغيل: يقارن تقدّم السحابة بالمحلي؛ الأعلى يفوز (ويُسأل اللاعب قبل استبدال تقدّم محلي موجود).
 * - أثناء اللعب: رفع تلقائي بعد كل فوز وعند الخروج من اللعبة.
 * كل الدوال آمنة: أي خطأ يعني فقط أن الحفظ السحابي لا يعمل، واللعبة تكمل بشكل طبيعي.
 */
object CloudSave {
    private const val NAME = "progress_v1"
    private const val PREFS = "tropical_crush_prefs"
    /** مفاتيح خاصة بالجهاز لا تُرفع ولا تُستبدل */
    private val SKIP = setOf("notif_asked", "trace", "sound_enabled", "haptics_enabled")

    /** 0 = مخفي/غير معروف، 1 = غير مسجَّل (نعرض زر الدخول)، 2 = التقدم محفوظ على الحساب */
    var state by mutableIntStateOf(0)

    private var actRef: WeakReference<Activity>? = null
    private var started = false
    private var ready = false          // لا نرفع قبل إتمام المقارنة الأولى
    private var busy = false
    private var lastHash = 0
    private val handler = Handler(Looper.getMainLooper())
    private val pushRunnable = Runnable { actRef?.get()?.let { push(it) } }

    private fun act(): Activity? = actRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

    fun attach(a: Activity) {
        actRef = WeakReference(a)
        if (started) return
        started = true
        try {
            PlayGames.getGamesSignInClient(a).isAuthenticated().addOnCompleteListener { t ->
                val ok = try { t.isSuccessful && t.result.isAuthenticated } catch (e: Throwable) { false }
                if (ok) sync(a) else state = 1
            }
        } catch (e: Throwable) { state = 0 }
    }

    /** زر "احفظ تقدمك بحساب ألعاب Google" */
    fun signIn() {
        val a = act() ?: return
        try {
            PlayGames.getGamesSignInClient(a).signIn().addOnCompleteListener { t ->
                val ok = try { t.isSuccessful && t.result.isAuthenticated } catch (e: Throwable) { false }
                if (ok) sync(a)
                else Toast.makeText(a, "تعذّر تسجيل الدخول بألعاب Google", Toast.LENGTH_LONG).show()
            }
        } catch (e: Throwable) { }
    }

    // ───────── تحويل التقدم إلى/من JSON ─────────
    private fun localMap(a: Context): Map<String, Any?> =
        a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.filterKeys { it !in SKIP }

    private fun score(m: Map<String, Any?>): Int {
        var s = 0
        for ((k, v) in m) {
            val n = (v as? Int) ?: continue
            when {
                k.startsWith("stars_level_") -> s += n * 10
                k.startsWith("completions_level_") -> s += n
                k == "rescue_wins" -> s += n * 30
            }
        }
        return s
    }

    private fun toBytes(m: Map<String, Any?>): ByteArray {
        val p = JSONObject()
        for ((k, v) in m) {
            val e = JSONArray()
            when (v) {
                is Int -> { e.put("i"); e.put(v) }
                is Long -> { e.put("l"); e.put(v) }
                is Boolean -> { e.put("b"); e.put(v) }
                is Float -> { e.put("f"); e.put(v.toDouble()) }
                is String -> { e.put("s"); e.put(v) }
                else -> continue
            }
            p.put(k, e)
        }
        return JSONObject().put("v", 1).put("ts", System.currentTimeMillis()).put("p", p).toString().toByteArray(Charsets.UTF_8)
    }

    private fun parse(bytes: ByteArray): Map<String, Any?>? = try {
        if (bytes.isEmpty()) null else {
            val p = JSONObject(String(bytes, Charsets.UTF_8)).getJSONObject("p")
            val out = HashMap<String, Any?>()
            for (k in p.keys()) {
                if (k in SKIP) continue
                val e = p.getJSONArray(k)
                out[k] = when (e.getString(0)) {
                    "i" -> e.getInt(1); "l" -> e.getLong(1); "b" -> e.getBoolean(1)
                    "f" -> e.getDouble(1).toFloat(); else -> e.getString(1)
                }
            }
            out
        }
    } catch (e: Throwable) { null }

    private fun hashOf(m: Map<String, Any?>): Int = m.toSortedMap().toString().hashCode()

    // ───────── التزامن ─────────
    private fun openSnapshot(a: Activity, tries: Int = 3, done: (Snapshot?, SnapshotsClient) -> Unit) {
        val client = PlayGames.getSnapshotsClient(a)
        client.open(NAME, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED).addOnCompleteListener { t ->
            try {
                if (!t.isSuccessful) { done(null, client); return@addOnCompleteListener }
                val r = t.result
                if (r.isConflict) {
                    val c = r.conflict!!
                    client.resolveConflict(c.conflictId, c.snapshot).addOnCompleteListener {
                        if (tries > 0) openSnapshot(a, tries - 1, done) else done(null, client)
                    }
                } else done(r.data, client)
            } catch (e: Throwable) { done(null, client) }
        }
    }

    private fun writeLocal(a: Activity, snap: Snapshot, client: SnapshotsClient) {
        try {
            val m = localMap(a)
            snap.snapshotContents.writeBytes(toBytes(m))
            client.commitAndClose(snap, SnapshotMetadataChange.Builder().setDescription("تقدّم مملكة الفواكه الاستوائية").build())
                .addOnCompleteListener { t -> if (t.isSuccessful) { lastHash = hashOf(m); state = 2 }; busy = false }
        } catch (e: Throwable) { busy = false }
    }

    /** المقارنة الأولى بعد تسجيل الدخول */
    private fun sync(a: Activity) {
        if (busy) return
        busy = true
        openSnapshot(a) { snap, client ->
            if (snap == null) { busy = false; return@openSnapshot }
            try {
                val cloud = try { parse(snap.snapshotContents.readFully()) } catch (e: Throwable) { null }
                val local = localMap(a)
                val ls = score(local)
                val cs = cloud?.let { score(it) } ?: -1
                if (cloud != null && cs > ls) {
                    client.discardAndClose(snap)
                    if (ls == 0) { applyAndRestart(a, cloud) }
                    else askRestore(a, cloud, cs, ls)
                } else {
                    ready = true
                    writeLocal(a, snap, client)   // يضبط busy=false عند الانتهاء
                }
            } catch (e: Throwable) { busy = false }
        }
    }

    private fun askRestore(a: Activity, cloud: Map<String, Any?>, cs: Int, ls: Int) {
        if (a.isFinishing) { busy = false; return }
        android.app.AlertDialog.Builder(a)
            .setTitle("☁️ تقدّم محفوظ على حسابك")
            .setMessage("وجدنا تقدّمًا أعلى محفوظًا على حساب ألعاب Google (${cs / 10} نجمة تقريبًا مقابل ${ls / 10} على هذا الجهاز).\n\nهل تريد استرجاعه؟ سيُعاد تشغيل اللعبة، وسيُستبدل تقدّم هذا الجهاز.")
            .setCancelable(false)
            .setPositiveButton("استرجاع") { _, _ -> applyAndRestart(a, cloud) }
            .setNegativeButton("إبقاء تقدّم هذا الجهاز") { _, _ -> busy = false; ready = true; push(a, force = true) }
            .show()
    }

    private fun applyAndRestart(a: Activity, cloud: Map<String, Any?>) {
        try {
            val sp = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val ed = sp.edit()
            for (k in sp.all.keys) if (k !in SKIP) ed.remove(k)
            for ((k, v) in cloud) when (v) {
                is Int -> ed.putInt(k, v); is Long -> ed.putLong(k, v); is Boolean -> ed.putBoolean(k, v)
                is Float -> ed.putFloat(k, v); is String -> ed.putString(k, v)
            }
            ed.commit()
            ready = true; busy = false
            val launch = a.packageManager.getLaunchIntentForPackage(a.packageName)!!
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
            a.startActivity(android.content.Intent(a, com.example.PhoenixActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("next", launch))
            Runtime.getRuntime().exit(0)
        } catch (e: Throwable) { busy = false }
    }

    /** رفع مؤجّل (يُستدعى بعد الفوز) */
    fun requestPush() {
        if (!ready) return
        handler.removeCallbacks(pushRunnable)
        handler.postDelayed(pushRunnable, 4000)
    }

    /** رفع التقدّم الحالي إن تغيّر */
    fun push(a: Activity, force: Boolean = false) {
        if (!ready || busy) return
        try {
            val m = localMap(a)
            if (!force && hashOf(m) == lastHash) return
            busy = true
            openSnapshot(a) { snap, client ->
                if (snap == null) { busy = false } else writeLocal(a, snap, client)
            }
        } catch (e: Throwable) { busy = false }
    }
}
