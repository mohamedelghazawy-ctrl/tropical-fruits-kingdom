package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** إعلان مكافئ حقيقي من AdMob. نسخ debug تستخدم وحدة Google التجريبية لتفادي حظر الحساب. */
object AdMobRewardedProvider : RewardedAdProvider {
    private const val REAL_UNIT = "ca-app-pub-6221523107197866/8375224473"
    private const val TEST_UNIT = "ca-app-pub-3940256099942544/5224354917"

    fun init(ctx: Context) {
        val app = ctx.applicationContext
        Thread { try { MobileAds.initialize(app) {} } catch (e: Throwable) { } }.start()
        RewardedAds.provider = this
    }

    private fun findActivity(c: Context): Activity? {
        var x: Context? = c
        while (x is ContextWrapper) { if (x is Activity) return x; x = x.baseContext }
        return null
    }

    @Composable
    override fun Show(title: String, onReward: () -> Unit, onCancel: () -> Unit) {
        val ctx = LocalContext.current
        var failed by remember { mutableStateOf(false) }
        val rewardCb by rememberUpdatedState(onReward)
        val cancelCb by rememberUpdatedState(onCancel)

        LaunchedEffect(Unit) {
            val act = findActivity(ctx)
            if (act == null) { failed = true; return@LaunchedEffect }
            val debuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            RewardedAd.load(act, if (debuggable) TEST_UNIT else REAL_UNIT, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        var earned = false
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() { if (earned) rewardCb() else cancelCb() }
                            override fun onAdFailedToShowFullScreenContent(e: AdError) { failed = true }
                        }
                        ad.show(act, OnUserEarnedRewardListener { earned = true })
                    }
                    override fun onAdFailedToLoad(e: LoadAdError) { failed = true }
                })
        }

        Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF008241A)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📺 $title", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    if (failed) {
                        Text("الإعلان غير متاح الآن، حاول بعد قليل", color = Color(0xFFFFD166), fontSize = 16.sp, textAlign = TextAlign.Center)
                    } else {
                        CircularProgressIndicator(color = Color(0xFFFFD166))
                        Spacer(Modifier.height(8.dp))
                        Text("جاري تحميل الإعلان…", color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(44.dp)) { Text("إغلاق", color = Color.White) }
                }
            }
        }
    }
}
