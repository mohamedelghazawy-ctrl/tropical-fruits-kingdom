package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * نظام جسيمات بدون تخصيص ذاكرة أثناء التحديث (SoA + FloatArray ثابتة).
 * الإحداثيات بوحدات الخلية (col + 0.5, row + 0.5) فتتوافق مع أي حجم شاشة.
 * يُستدعى من الـ Main thread فقط (ViewModel + إطار Compose).
 */
class ParticleSystem(private val capacity: Int = 192) {
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val size = FloatArray(capacity)
    val life = FloatArray(capacity)
    val argb = IntArray(capacity)
    private val vx = FloatArray(capacity)
    private val vy = FloatArray(capacity)

    var count = 0
        private set

    /** يتغير عند كل انبعاث ليوقظ حلقة الإطارات في الواجهة. */
    var spawnVersion by mutableIntStateOf(0)
        private set

    val hasActive: Boolean get() = count > 0

    fun emit(cx: Float, cy: Float, color: Int, n: Int = 6) {
        repeat(n) {
            if (count >= capacity) return
            val angle = Random.nextFloat() * TWO_PI
            val speed = Random.nextFloat() * 8f + 2f // خلية/ثانية ≈ تقريب للسرعة القديمة
            val i = count++
            x[i] = cx; y[i] = cy
            vx[i] = cos(angle) * speed * SPEED_SCALE
            vy[i] = sin(angle) * speed * SPEED_SCALE
            size[i] = Random.nextFloat() * 7f + 4f
            life[i] = 1f
            argb[i] = color
        }
    }

    fun notifySpawned() { spawnVersion++ }

    /** تحديث بالزمن الفعلي (ثواني) مع حذف الميت بنقل آخر عنصر (O(1)). */
    fun update(dt: Float) {
        val d = dt.coerceAtMost(0.05f)
        var i = 0
        while (i < count) {
            life[i] -= d * LIFE_DECAY
            if (life[i] <= 0f) {
                val last = --count
                if (i != last) {
                    x[i] = x[last]; y[i] = y[last]; vx[i] = vx[last]; vy[i] = vy[last]
                    size[i] = size[last]; life[i] = life[last]; argb[i] = argb[last]
                }
                continue
            }
            x[i] += vx[i] * d
            y[i] += vy[i] * d
            vy[i] += GRAVITY * d
            i++
        }
    }

    fun clear() { count = 0 }

    private companion object {
        const val TWO_PI = 6.2831855f
        const val SPEED_SCALE = 0.6f // وحدة خلية/ث
        const val LIFE_DECAY = 1.7f   // ~0.6 ثانية عمر
        const val GRAVITY = 2.5f
    }
}
