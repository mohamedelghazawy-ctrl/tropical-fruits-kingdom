package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.unit.Dp
import com.example.island.*
import com.example.island.IslandData.BDS
import com.example.island.IslandData.IFAC
import com.example.island.IslandData.WORLD_H
import com.example.island.IslandData.WORLD_W
import kotlin.math.max
import kotlin.math.min

private fun fa(n: Int): String = n.toString().map { if (it in '0'..'9') "٠١٢٣٤٥٦٧٨٩"[it - '0'] else it }.joinToString("")
private fun fmtMs(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0); val h = t / 3600; val m = (t % 3600) / 60; val sec = t % 60
    val raw = if (h > 0) "$h:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}" else "$m:${sec.toString().padStart(2, '0')}"
    return raw.map { c -> if (c in '0'..'9') "٠١٢٣٤٥٦٧٨٩"[c - '0'] else c }.joinToString("")
}

@Composable
private fun drawableId(name: String): Int {
    val ctx = LocalContext.current
    return remember(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName).takeIf { it != 0 } ?: android.R.drawable.ic_menu_gallery }
}

/**
 * شاشة جزيرة القصر. ملاحظة: كُتبت دون إمكانية الترجمة في بيئة التطوير (لا Android SDK) —
 * يجب بناؤها ومراجعتها على Android Studio.
 */
@Composable
fun IslandScreen(
    engine: IslandEngine,
    version: Int,
    onChanged: (block: IslandEngine.() -> Unit) -> Unit,
    onAdSpeedUp: (Int) -> Unit,
    onAdBuilder: () -> Unit,
    onAdDouble: (Int) -> Unit,
    bagCount: (Item) -> Int,
    onBack: () -> Unit
) {
    @Suppress("UNUSED_EXPRESSION") version
    val now = System.currentTimeMillis()
    var selected by remember { mutableStateOf<Int?>(null) }
    var scale by remember { mutableFloatStateOf(0f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val density = LocalDensity.current
    val news = remember(version) { engine.takeNews() }
    var newsShown by remember { mutableStateOf<List<String>>(emptyList()) }
    if (news.isNotEmpty()) newsShown = newsShown + news
    var welcome by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(version) { val nw = engine.newResidents(); if (nw.isNotEmpty()) { welcome = welcome + nw; onChanged { } } }

    BackHandler { if (selected != null) selected = null else onBack() }
    val worldPxW = with(density) { WORLD_W.dp.toPx() }
    val worldPxH = with(density) { WORLD_H.dp.toPx() }

    fun clampPan(p: Offset, k: Float): Offset {
        val w = worldPxW * k; val h = worldPxH * k
        val m = with(density) { 56.dp.toPx() }   // حافة مياه حول الجزيرة
        val x = if (w + 2 * m <= viewport.width) (viewport.width - w) / 2 else p.x.coerceIn(viewport.width - w - m, m)
        val y = if (h + 2 * m <= viewport.height) (viewport.height - h) / 2 else p.y.coerceIn(viewport.height - h - m, m)
        return Offset(x, y)
    }

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFF7FBFC), Color(0xFFDCEEF2), Color(0xFFF2F8FA))))) {
        // العالم (الصورة + المباني) دائمًا LTR كي تتطابق الإحداثيات مع الصورة حتى في الواجهة العربية
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.fillMaxSize().onSizeChanged { viewport = it
                if (scale == 0f && it.width > 0) { scale = max(0.34f, max(it.width / worldPxW, it.height / worldPxH)); pan = clampPan(Offset.Zero, scale) } }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, dp_, zoom, _ ->
                        val kMin = max(0.28f, max(viewport.width / worldPxW, viewport.height / worldPxH))
                        val nk = (scale * zoom).coerceIn(kMin, 1.1f)
                        val np = Offset(centroid.x - (centroid.x - pan.x) * nk / scale + dp_.x, centroid.y - (centroid.y - pan.y) * nk / scale + dp_.y)
                        scale = nk; pan = clampPan(np, nk)
                    }
                }
        ) {
            if (scale > 0f) Box(
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).size(WORLD_W.dp, WORLD_H.dp).graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f); scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y
                }.clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { selected = null }
            ) {
                Image(painterResource(drawableId("isl_clean")), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                val all = (Sites.ALL.map { it.id } + PALACE).sortedBy { IslandData.POS.getValue(it).second }
                for (id in all) SiteView(engine, id, selected == id, now) { selected = id }
            }
        }
        }

        // الشريط العلوي
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = onBack) { Text("➜") }
            Spacer(Modifier.width(8.dp))
            Chip("💎 ${fa(engine.s.gems)}")
            Spacer(Modifier.width(6.dp))
            Chip("👷 ${fa(engine.s.q.size)}/${fa(engine.builders(now))}")
            Spacer(Modifier.width(6.dp))
            Chip("🏠 ${fa(engine.assigned)}/${fa(engine.residents)}")
        }
        Spacer(Modifier.height(6.dp))
        Chip("🎒 🔨${fa(bagCount(Item.HAMMER))}  🌪️${fa(bagCount(Item.SHUFFLE))}  ⚡${fa(bagCount(Item.ROW))}  🌊${fa(bagCount(Item.COL))}")
        }

        // لوحة تفاصيل المبنى
        selected?.let { id ->
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), color = Color(0xE6102A3A), tonalElevation = 6.dp
            ) { SiteSheet(engine, id, now, onChanged, onAdSpeedUp, onAdBuilder, onAdDouble, bagCount) }
        }

        // إشعارات (اكتمال بناء، وصول مراكب…)
        if (newsShown.isNotEmpty()) AlertDialog(
            onDismissRequest = { newsShown = emptyList() },
            confirmButton = { TextButton(onClick = { newsShown = emptyList() }) { Text("حسنًا") } },
            title = { Text("أخبار الجزيرة") }, text = { Column { newsShown.forEach { Text(it); Spacer(Modifier.height(4.dp)) } } }
        )
        // الترحيب بالسكان الجدد
        welcome.firstOrNull()?.let { idx ->
            AlertDialog(
                onDismissRequest = { welcome = welcome.drop(1) },
                confirmButton = { TextButton(onClick = { welcome = welcome.drop(1) }) { Text("أهلًا بك! 👋") } },
                title = { Text("ساكن جديد يصل إلى الجزيرة") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Image(painterResource(drawableId("resident_$idx")), null, Modifier.size(96.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Spacer(Modifier.height(8.dp)); Text(IslandData.RESIDENTS[idx % IslandData.RESIDENTS.size], fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("رحّب به — وعيّنه في أحد المباني", textAlign = TextAlign.Center)
                    }
                }
            )
        }
    }
}

@Composable
private fun Chip(text: String) = Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xAA000000)).padding(horizontal = 10.dp, vertical = 6.dp)) {
    Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

/** مبنى واحد على الجزيرة: صورة مرمَّم/أطلال + اسم + شارات (جاهز / بلا عامل / يُبنى). */
@Composable
private fun SiteView(engine: IslandEngine, id: Int, selected: Boolean, now: Long, onTap: () -> Unit) {
    val (px, py) = IslandData.POS.getValue(id)
    val site = Sites.of(id)
    val built = engine.has(id)
    val building = engine.inQ(id) != null
    val dpX = px * IFAC; val dpY = py * IFAC
    val name = if (id == PALACE) "قصر الملكة" else site.name
    if (id == PALACE) {
        // منطقة اللمس تغطي القصر كله (البرجين والقبة والواجهة) دون الخروج عن شكله — بإحداثيات صورة الجزيرة
        val ms = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        for ((hx, hy, hw, hh) in listOf(
            listOf(399, 55, 140, 515),   // البرج الأيسر
            listOf(815, 55, 140, 515),   // البرج الأيمن
            listOf(539, 85, 276, 485)    // القبة والواجهة والباب
        )) {
            Box(Modifier.offset(x = hx.dp, y = hy.dp).size(hw.dp, hh.dp).clickable(indication = null, interactionSource = ms) { onTap() })
        }
        Tag(name + " ${fa(engine.s.pal)}", dpX, dpY + 12, selected, true)
        return
    }
    val meta = (if (built) IslandData.BMETA else IslandData.RMETA).getValue(id)
    val w = meta.w * BDS * IFAC; val h = meta.h * BDS * IFAC; val off = meta.off * BDS * IFAC
    val res = drawableId((if (built) "isl_b" else "isl_r") + id)
    val dim = site.soon
    Image(
        painterResource(res), name,
        Modifier.offset(x = (dpX - w / 2).dp, y = (dpY - (h - off)).dp).size(w.dp, h.dp)
            .graphicsLayer { alpha = if (dim) 0.6f else 1f }
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onTap() }
    )
    Tag(name + if (built) " ${fa(engine.lvOf(id))}" else if (building) " 🚧" else "", dpX, dpY + 6, selected, built)
    val badge = when {
        built && engine.readyList(now).contains(id) -> "!" to Color(0xFFFF5D73)
        built && site.needsWorker && (engine.s.wk[id] ?: 0) == 0 && !engine.workerFree(id) -> "👷" to Color(0xFFF4B942)
        else -> null
    }
    badge?.let { (t, c) -> Box(Modifier.offset(x = (dpX + w / 4).dp, y = (dpY - h / 2).dp).size(22.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) { Text(t, fontSize = 12.sp, color = Color.White) } }
}

@Composable
private fun Tag(text: String, cx: Float, y: Float, selected: Boolean, built: Boolean) {
    Box(
        Modifier.offset(x = (cx - 46).dp, y = y.dp).width(92.dp), contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(if (built) Color(0xCC08241A) else Color(0xCC3A2A12))
                .then(if (selected) Modifier.border(2.dp, Color(0xFFFFD166), RoundedCornerShape(9.dp)) else Modifier).padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SiteSheet(
    engine: IslandEngine, id: Int, now: Long,
    onChanged: (IslandEngine.() -> Unit) -> Unit, onAdSpeedUp: (Int) -> Unit, onAdBuilder: () -> Unit, onAdDouble: (Int) -> Unit,
    bagCount: (Item) -> Int
) {
    val site = Sites.of(id)
    val lv = engine.lvOf(id)
    val q = engine.inQ(id)
    var pick by remember(id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${if (id == PALACE) "قصر الملكة" else site.name} — المستوى ${fa(lv)}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(site.perk, color = Color(0xFFBFE9FF), fontSize = 13.sp)

        // بناء / ترقية
        if (q != null) {
            val total = (q.end - q.start).coerceAtLeast(1)
            LinearProgressIndicator(progress = { ((now - q.start).toFloat() / total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
            Text("🚧 يُبنى… ${fmtMs(q.end - now)}", color = Color.White)
            Button(onClick = { onAdSpeedUp(id) }) { Text("📺 شاهد إعلانًا: −٣٠ دقيقة") }
        } else {
            val up = engine.canUp(id, now)
            if (!up.maxed && !site.soon) {
                Button(enabled = up.ok, onClick = { onChanged { startBuild(id, System.currentTimeMillis()) } }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (lv == 0) "رمّم" else "ارفع إلى ${fa(up.n)}")
                    Text("  💎${fa(up.cost)} · ⏱ ${fmtMs(up.ms)}", fontSize = 12.sp)
                }
                if (!up.ok && up.why.isNotEmpty()) Text(up.why, color = Color(0xFFFFD166), fontSize = 13.sp)
                if (up.why == "كل البنّائين مشغولون") TextButton(onClick = onAdBuilder) { Text("📺 بنّاء إضافي ٢٤ ساعة (إعلان)") }
            } else if (up.maxed) Text("بلغ أقصى مستوى ✨", color = Color(0xFF7FD184))
        }

        if (lv >= 1 && site.needsWorker) {
            val w = engine.workerOf(id)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (w >= 0) {
                    Image(painterResource(drawableId("resident_$w")), null, Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    Text("  ${IslandData.RESIDENTS[w % IslandData.RESIDENTS.size]}", color = Color.White, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onChanged { setWorker(id, -1, System.currentTimeMillis()) } }) { Text("أزِل العامل") }
                } else {
                    Text(if (engine.workerFree(id)) "يعمل بلا عامل (حتى المستوى ${fa(FREE_MARKET_LV)})" else "⚠ لا يوجد عامل", color = Color(0xFFFFD166), modifier = Modifier.weight(1f))
                    TextButton(onClick = { pick = !pick }) { Text("عيّن عاملًا") }
                }
            }
            if (pick) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(engine.freeResidents()) { r ->
                    Column(Modifier.clickable { onChanged { setWorker(id, r, System.currentTimeMillis()) }; pick = false }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(drawableId("resident_$r")), null, Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Text(IslandData.RESIDENTS[r % IslandData.RESIDENTS.size], color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        if (lv >= 1 && engine.produces(id)) {
            val v = engine.prodVal(id, now); val cap = engine.prodCap(id)
            Text("المخزون: ${fa(v.toInt())} / ${fa(cap.toInt())}", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = v >= 1 && engine.worked(id), onClick = { onChanged { collect(id, false, System.currentTimeMillis()) } }) { Text("اجمع") }
                OutlinedButton(enabled = v >= 1 && engine.worked(id), onClick = { onAdDouble(id) }) { Text("📺 ×٢") }
            }
        }
        if (id == Sites.FOUNTAIN && lv >= 1) {
            var msg by remember { mutableStateOf<String?>(null) }
            Button(onClick = {
                onChanged { val r = wish(System.currentTimeMillis(), java.util.TimeZone.getDefault().rawOffset.toLong()); msg = r?.let { (k, a) -> "حصلت على ${fa(a)} ${if (k == "gems") "💎" else if (k == "hammer") "🔨" else "🌪️"}" } ?: "استُخدمت أمنية اليوم، عُد غدًا ✨" }
            }) { Text("🌠 اطلب أمنية اليوم") }
            msg?.let { Text(it, color = Color(0xFFFFD166)) }
        }
        if (id == Sites.PORT && lv >= 1) PortPanel(engine, now, onChanged, bagCount)
    }
}

@Composable
private fun PortPanel(engine: IslandEngine, now: Long, onChanged: (IslandEngine.() -> Unit) -> Unit, bagCount: (Item) -> Int) {
    val qty = engine.tripQty()
    Text("الحقيبة: 🔨${fa(bagCount(Item.HAMMER))}  🌪️${fa(bagCount(Item.SHUFFLE))}  ⚡${fa(bagCount(Item.ROW))}  🌊${fa(bagCount(Item.COL))}", color = Color.White, fontSize = 13.sp)
    for (i in 0 until engine.boatCount()) {
        val b = engine.s.boats.getOrNull(i)
        if (b != null) Text("⛵ مركب ${fa(i + 1)}: ${fmtMs(b.end - now)}", color = Color.White)
        else Column {
            Text("⛵ مركب ${fa(i + 1)} جاهز — الرحلة: ${fa(qty)} قطع", color = Color(0xFFBFE9FF), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (it in listOf(Item.HAMMER, Item.SHUFFLE)) {
                    OutlinedButton(enabled = bagCount(it) >= qty, onClick = { onChanged { boatGo(i, true, it, null, qty, System.currentTimeMillis()) } }) {
                        Text("بِع ${if (it == Item.HAMMER) "🔨" else "🌪️"} 💎${fa(qty * it.sell)}", fontSize = 12.sp)
                    }
                }
                OutlinedButton(enabled = bagCount(Item.HAMMER) >= qty, onClick = { onChanged { boatGo(i, false, Item.HAMMER, Item.SHUFFLE, qty, System.currentTimeMillis()) } }) { Text("🔨→🌪️", fontSize = 12.sp) }
                OutlinedButton(enabled = bagCount(Item.SHUFFLE) >= qty, onClick = { onChanged { boatGo(i, false, Item.SHUFFLE, Item.HAMMER, qty, System.currentTimeMillis()) } }) { Text("🌪️→🔨", fontSize = 12.sp) }
            }
        }
    }
}
