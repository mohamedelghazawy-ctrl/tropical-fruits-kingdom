package com.example.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

/** إشعارات "جاهز": اكتمال البناء، وصول المراكب، عجلة الحظ. تُجدوَل بمنبّه غير دقيق (لا يحتاج إذنًا خاصًا). */
object ReadyNotifier {
    const val CHANNEL = "ready"
    const val ID_BUILD = 1001
    const val ID_BOAT = 1002
    const val ID_WHEEL = 1003

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "الجاهزية", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    private fun pending(ctx: Context, id: Int, text: String): PendingIntent {
        val i = Intent(ctx, ReadyReceiver::class.java).putExtra("id", id).putExtra("text", text)
        return PendingIntent.getBroadcast(ctx, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context, id: Int, atMs: Long, text: String) {
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pending(ctx, id, text)
            if (atMs <= System.currentTimeMillis()) am.cancel(pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        } catch (e: Exception) { }
    }

    fun cancel(ctx: Context, id: Int) {
        try {
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(ctx, id, ""))
        } catch (e: Exception) { }
    }
}

class ReadyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val text = intent.getStringExtra("text") ?: return
        val id = intent.getIntExtra("id", 0)
        ReadyNotifier.ensureChannel(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val open = PendingIntent.getActivity(context, id, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, ReadyNotifier.CHANNEL)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("مملكة الفواكه الاستوائية")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, n)
    }
}
