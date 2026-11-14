package com.studioerow.glasskb

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class GlassKeyboardView(ctx: Context, private val ime: GlassKeyboardService) : LinearLayout(ctx) {

    private var sug: List<String> = listOf("", "", "")
    private val sugViews = ArrayList<TextView>()
    private var showTools = false
    private var showEmoji = false
    private var showClip = false
    private var emojiCat = 1

    init {
        orientation = VERTICAL
        rebuild()
    }

    fun setSuggestions(list: List<String>) {
        sug = list
        for (i in 0 until sugViews.size) sugViews[i].text = sug.getOrElse(i) { "" }
    }

    fun rebuild() {
        val prefs = ime.prefs
        val pal = Pal.make(context, prefs)
        val scale = when (prefs.getInt("size", 1)) { 0 -> 0.85f; 2 -> 1.15f; else -> 1f }
        val rowH = dp(46f * scale)
        val haptic = prefs.getBoolean("haptic", true)

        removeAllViews()
        setPadding(dp(6f), dp(8f), dp(6f), dp(6f))

        addView(buildStrip(pal))
        if (showTools) addView(buildTools(pal))

        if (showEmoji) {
            buildEmoji(pal, rowH, haptic)
            return
        }
        if (showClip) {
            buildClip(pal, rowH, haptic)
            return
        }

        val rows = Layouts.rows(ime.lang, ime.shift != 0, ime.page, prefs.getBoolean("numrow", false))
        for (row in rows) {
            val line = LinearLayout(context)
            line.orientation = HORIZONTAL
            for (k in row) {
                if (k.type == KT.SPACER) {
                    val sp = View(context)
                    line.addView(sp, LayoutParams(0, rowH, k.w))
                    continue
                }
                val active = k.type == KT.SHIFT && ime.shift != 0
                val label = when {
                    k.type == KT.SHIFT && ime.shift == 2 -> "⇪"
                    else -> k.label
                }
                val key = GlassKey(
                    context, k.copy(label = label), pal, active, haptic,
                    { ime.onKey(it) },
                    if (k.type == KT.LANG) ({ _: K -> ime.showPicker() }) else null
                )
                line.addView(key, LayoutParams(0, rowH, k.w))
            }
            addView(line, LayoutParams(LayoutParams.MATCH_PARENT, rowH))
        }
    }

    private fun pill(pal: Pal, radius: Float): GradientDrawable = GradientDrawable().apply {
        cornerRadius = radius
        setColor(pal.strip)
        setStroke(dp(0.8f), pal.panelRim)
    }

    private fun label(pal: Pal, t: String, size: Float): TextView = TextView(context).apply {
        text = t
        setTextColor(pal.text)
        textSize = size
        gravity = Gravity.CENTER
        maxLines = 1
    }

    private fun buildStrip(pal: Pal): View {
        val strip = LinearLayout(context)
        strip.orientation = HORIZONTAL
        strip.background = pill(pal, dp(18f).toFloat())
        val grid = label(pal, if (showTools) "✕" else "⊞", 18f)
        grid.setOnClickListener { showTools = !showTools; if (!showTools) { showEmoji = false; showClip = false }; rebuild() }
        strip.addView(grid, LayoutParams(dp(44f), dp(40f)))
        sugViews.clear()
        for (i in 0 until 3) {
            val t = label(pal, sug.getOrElse(i) { "" }, 16f)
            t.setOnClickListener { ime.pickSuggestion(t.text.toString()) }
            sugViews.add(t)
            strip.addView(t, LayoutParams(0, dp(40f), 1f))
        }
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(8f)
        strip.layoutParams = lp
        return strip
    }

    private fun buildTools(pal: Pal): View {
        val bar = LinearLayout(context)
        bar.orientation = HORIZONTAL
        fun tool(icon: String, action: () -> Unit) {
            val t = label(pal, icon, 20f)
            t.background = pill(pal, dp(16f).toFloat())
            t.setOnClickListener { action() }
            val lp = LayoutParams(0, dp(40f), 1f)
            lp.marginEnd = dp(4f)
            bar.addView(t, lp)
        }
        tool("😊") { showEmoji = true; emojiCat = if (ime.recentEmojis().isEmpty()) 1 else 0; rebuild() }
        tool("📋") { showClip = true; rebuild() }
        tool("Aa") { ime.cycleSize(); rebuild() }
        tool("◐") { ime.cycleTheme(); rebuild() }
        tool("123") { ime.toggleNumRow(); rebuild() }
        tool("⚙") { ime.openSettings() }
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(8f)
        bar.layoutParams = lp
        return bar
    }

    private fun buildClip(pal: Pal, rowH: Int, haptic: Boolean) {
        val top = LinearLayout(context)
        top.orientation = HORIZONTAL
        val back = GlassKey(context, K("ABC", "", KT.SYM, 1f), pal, false, haptic,
            { showClip = false; rebuild() }, null)
        val title = label(pal, "Clipboard · lomba chap = delete", 13f)
        val clear = GlassKey(context, K("Clear", "", KT.SYM, 1f), pal, false, haptic,
            { ime.clearClips(); rebuild() }, null)
        top.addView(back, LayoutParams(0, rowH, 1.3f))
        top.addView(title, LayoutParams(0, rowH, 3f))
        top.addView(clear, LayoutParams(0, rowH, 1.3f))
        addView(top, LayoutParams(LayoutParams.MATCH_PARENT, rowH))

        val box = LinearLayout(context)
        box.orientation = VERTICAL
        val items = ime.clipItems()
        if (items.isEmpty()) {
            val empty = label(pal, "Kichu copy kora nai. Ekhon theke ja copy korbe ekhane jomba hobe.", 14f)
            empty.setPadding(dp(16f), dp(28f), dp(16f), dp(28f))
            box.addView(empty)
        }
        for (t in items) {
            val tv = TextView(context)
            tv.text = t
            tv.setTextColor(pal.text)
            tv.textSize = 15f
            tv.maxLines = 2
            tv.ellipsize = TextUtils.TruncateAt.END
            tv.setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
            tv.background = pill(pal, dp(16f).toFloat())
            tv.setOnClickListener { ime.commit(t) }
            tv.setOnLongClickListener { ime.removeClip(t); rebuild(); true }
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(6f)
            box.addView(tv, lp)
        }
        val sv = ScrollView(context)
        sv.addView(box)
        addView(sv, LayoutParams(LayoutParams.MATCH_PARENT, rowH * 4))
    }

    private fun buildEmoji(pal: Pal, rowH: Int, haptic: Boolean) {
        val list = if (emojiCat == 0) ime.recentEmojis() else Emojis.category(emojiCat)
        val box = LinearLayout(context)
        box.orientation = VERTICAL
        if (list.isEmpty()) {
            val empty = label(pal, "Ekhono kono emoji use koro ni", 14f)
            empty.setPadding(dp(16f), dp(28f), dp(16f), dp(28f))
            box.addView(empty)
        }
        var i = 0
        while (i < list.size) {
            val line = LinearLayout(context)
            line.orientation = HORIZONTAL
            for (j in 0 until 8) {
                val idx = i + j
                if (idx < list.size) {
                    val e = list[idx]
                    val t = label(pal, e, 24f)
                    t.setOnClickListener { ime.commitEmoji(e) }
                    line.addView(t, LayoutParams(0, dp(44f), 1f))
                } else {
                    line.addView(View(context), LayoutParams(0, dp(44f), 1f))
                }
            }
            box.addView(line)
            i += 8
        }
        val sv = ScrollView(context)
        sv.addView(box)
        addView(sv, LayoutParams(LayoutParams.MATCH_PARENT, rowH * 4))

        val bar = LinearLayout(context)
        bar.orientation = HORIZONTAL
        val back = GlassKey(context, K("ABC", "", KT.SYM, 1f), pal, false, haptic,
            { showEmoji = false; rebuild() }, null)
        bar.addView(back, LayoutParams(0, rowH, 1.4f))
        for (c in 0 until Emojis.icons.size) {
            val t = label(pal, Emojis.icons[c], 17f)
            if (c == emojiCat) t.background = pill(pal, dp(12f).toFloat())
            t.setOnClickListener { emojiCat = c; rebuild() }
            bar.addView(t, LayoutParams(0, rowH, 1f))
        }
        val del = GlassKey(context, K("⌫", "", KT.DEL, 1f), pal, false, haptic,
            { ime.onKey(it) }, null)
        bar.addView(del, LayoutParams(0, rowH, 1.4f))
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, rowH))
    }
}
