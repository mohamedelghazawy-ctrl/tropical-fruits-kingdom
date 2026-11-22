package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** يعمل في عملية منفصلة (:phoenix): يشغّل اللعبة من جديد بعد استرجاع التقدّم من السحابة ثم ينتهي. */
class PhoenixActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val next = intent.getParcelableExtra<Intent>("next")
        if (next != null) startActivity(next)
        finish()
        Runtime.getRuntime().exit(0)
    }
}
