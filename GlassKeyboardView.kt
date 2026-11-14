package com.studioerow.glasskb

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class GlassKeyboardView(ctx: Context, private val ime: GlassKeyboardService) : LinearLayout(ctx) {

    private var sug: List<String> = listOf("", "", "")
    private val sugViews = ArrayList<TextView>()
    private var showTools = false
    private var showEmoji = false

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
        grid.setOnClickListener { showTools = !showTools; if (!showTools) showEmoji = false; rebuild() }
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
        tool("😊") { showEmoji = true; rebuild() }
        tool("📋") { ime.pasteClipboard() }
        tool("Aa") { ime.cycleSize(); rebuild() }
        tool("◐") { ime.cycleTheme(); rebuild() }
        tool("123") { ime.toggleNumRow(); rebuild() }
        tool("⚙") { ime.openSettings() }
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(8f)
        bar.layoutParams = lp
        return bar
    }

    private fun buildEmoji(pal: Pal, rowH: Int, haptic: Boolean) {
        val all = Layouts.emojis
        var i = 0
        while (i < all.size) {
            val line = LinearLayout(context)
            line.orientation = HORIZONTAL
            for (j in i until minOf(i + 8, all.size)) {
                val e = all[j]
                val t = label(pal, e, 22f)
                t.setOnClickListener { ime.commit(e) }
                line.addView(t, LayoutParams(0, rowH, 1f))
            }
            addView(line, LayoutParams(LayoutParams.MATCH_PARENT, rowH))
            i += 8
        }
        val bottom = LinearLayout(context)
        bottom.orientation = HORIZONTAL
        val back = GlassKey(context, K("ABC", "", KT.SYM, 1f), pal, false, haptic,
            { showEmoji = false; rebuild() }, null)
        val del = GlassKey(context, K("⌫", "", KT.DEL, 1f), pal, false, haptic,
            { ime.onKey(it) }, null)
        bottom.addView(back, LayoutParams(0, rowH, 1f))
        bottom.addView(View(context), LayoutParams(0, rowH, 5f))
        bottom.addView(del, LayoutParams(0, rowH, 1f))
        addView(bottom, LayoutParams(LayoutParams.MATCH_PARENT, rowH))
    }
}
