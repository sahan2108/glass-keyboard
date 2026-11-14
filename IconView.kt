package com.studioerow.glasskb

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

enum class Ic { GRID, CLOSE, SMILE, CLIP, AUTOCAP, STYLE, THEME, SIZE, GEAR }

/** Small vector style icons for the toolbar, drawn by hand so they look the same on every phone. */
class IconView(ctx: Context, private val kind: Ic, private val pal: Pal, private val on: Boolean = false) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val s = minOf(w, h) * 0.30f
        var col = pal.text
        if (on) {
            p.style = Paint.Style.FILL
            p.color = pal.act
            val bw = minOf(w * 0.9f, h * 1.3f)
            c.drawRoundRect(RectF(cx - bw / 2, cy - h * 0.38f, cx + bw / 2, cy + h * 0.38f), dp(10f).toFloat(), dp(10f).toFloat(), p)
            col = 0xFFFFFFFF.toInt()
        }
        p.style = Paint.Style.STROKE
        p.color = col
        p.strokeWidth = dp(1.7f).toFloat()
        when (kind) {
            Ic.GRID -> {
                val g = s * 0.14f
                for (i in 0..1) for (j in 0..1) {
                    val l = cx - s + i * (s + g) - (if (i == 1) 0f else 0f)
                    val t = cy - s + j * (s + g)
                    c.drawRoundRect(RectF(l, t, l + s - g, t + s - g), dp(2f).toFloat(), dp(2f).toFloat(), p)
                }
            }
            Ic.CLOSE -> {
                c.drawLine(cx - s * 0.8f, cy - s * 0.8f, cx + s * 0.8f, cy + s * 0.8f, p)
                c.drawLine(cx - s * 0.8f, cy + s * 0.8f, cx + s * 0.8f, cy - s * 0.8f, p)
            }
            Ic.SMILE -> {
                c.drawCircle(cx, cy, s * 1.1f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx - s * 0.4f, cy - s * 0.25f, s * 0.12f, p)
                c.drawCircle(cx + s * 0.4f, cy - s * 0.25f, s * 0.12f, p)
                p.style = Paint.Style.STROKE
                c.drawArc(RectF(cx - s * 0.6f, cy - s * 0.35f, cx + s * 0.6f, cy + s * 0.65f), 25f, 130f, false, p)
            }
            Ic.CLIP -> {
                c.drawRoundRect(RectF(cx - s * 0.85f, cy - s * 0.95f, cx + s * 0.85f, cy + s * 1.1f), dp(3f).toFloat(), dp(3f).toFloat(), p)
                c.drawRoundRect(RectF(cx - s * 0.4f, cy - s * 1.2f, cx + s * 0.4f, cy - s * 0.7f), dp(2f).toFloat(), dp(2f).toFloat(), p)
                c.drawLine(cx - s * 0.45f, cy + s * 0.1f, cx + s * 0.45f, cy + s * 0.1f, p)
                c.drawLine(cx - s * 0.45f, cy + s * 0.55f, cx + s * 0.2f, cy + s * 0.55f, p)
            }
            Ic.AUTOCAP -> {
                p.style = Paint.Style.FILL
                p.textAlign = Paint.Align.CENTER
                p.isFakeBoldText = true
                p.textSize = s * 1.5f
                c.drawText("Aa", cx, cy - (p.descent() + p.ascent()) / 2f, p)
                p.isFakeBoldText = false
            }
            Ic.STYLE -> {
                c.drawRoundRect(RectF(cx - s * 1.0f, cy - s * 0.6f, cx + s * 0.4f, cy + s * 1.0f), dp(3f).toFloat(), dp(3f).toFloat(), p)
                c.drawRoundRect(RectF(cx - s * 0.4f, cy - s * 1.0f, cx + s * 1.0f, cy + s * 0.6f), dp(3f).toFloat(), dp(3f).toFloat(), p)
            }
            Ic.THEME -> {
                c.drawCircle(cx, cy, s * 1.05f, p)
                p.style = Paint.Style.FILL
                c.drawArc(RectF(cx - s * 1.05f, cy - s * 1.05f, cx + s * 1.05f, cy + s * 1.05f), 90f, 180f, true, p)
            }
            Ic.SIZE -> {
                p.style = Paint.Style.FILL
                p.textAlign = Paint.Align.CENTER
                p.textSize = s * 1.0f
                c.drawText("A", cx - s * 0.55f, cy + s * 0.45f, p)
                p.textSize = s * 1.6f
                c.drawText("A", cx + s * 0.55f, cy + s * 0.6f, p)
            }
            Ic.GEAR -> {
                c.drawCircle(cx, cy, s * 0.5f, p)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val x1 = cx + (s * 0.85f * Math.cos(a)).toFloat()
                    val y1 = cy + (s * 0.85f * Math.sin(a)).toFloat()
                    val x2 = cx + (s * 1.2f * Math.cos(a)).toFloat()
                    val y2 = cy + (s * 1.2f * Math.sin(a)).toFloat()
                    c.drawLine(x1, y1, x2, y2, p)
                }
                c.drawCircle(cx, cy, s * 0.85f, p)
            }
        }
    }
}
