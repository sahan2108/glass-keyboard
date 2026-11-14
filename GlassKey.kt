package com.studioerow.glasskb

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class GlassKey(
    ctx: Context,
    private val k: K,
    private val pal: Pal,
    private val active: Boolean,
    private val haptic: Boolean,
    private val onTap: (K) -> Unit,
    private val onLong: ((K) -> Unit)?
) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val h = Handler(Looper.getMainLooper())
    private var down = false
    private var longFired = false
    private val fireOnDown = k.type != KT.LANG

    private val repeat = object : Runnable {
        override fun run() {
            onTap(k)
            h.postDelayed(this, 45)
        }
    }
    private val longRun = Runnable {
        longFired = true
        onLong?.invoke(k)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val hh = height.toFloat()
        val m = dp(2.5f).toFloat()
        val r = RectF(m, m, w - m, hh - m - dp(1f))
        val rad = hh * 0.26f

        val isFn = k.type == KT.SHIFT || k.type == KT.DEL || k.type == KT.SYM ||
                k.type == KT.PAGE || k.type == KT.LANG
        val top: Int
        val bot: Int
        var txt = pal.text
        when {
            k.type == KT.ENTER -> { top = pal.accTop; bot = pal.accBot; txt = 0xFFFFFFFF.toInt() }
            k.type == KT.SHIFT && active -> { top = pal.activeTop; bot = pal.activeTop; txt = pal.activeText }
            isFn -> { top = pal.fnTop; bot = pal.fnBot }
            else -> { top = pal.keyTop; bot = pal.keyBot }
        }

        // soft shadow under the glass
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = pal.shadow
        c.drawRoundRect(RectF(r.left, r.top + dp(1.5f), r.right, r.bottom + dp(1.5f)), rad, rad, p)

        // translucent glass body
        p.shader = LinearGradient(0f, r.top, 0f, r.bottom, top, bot, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, rad, rad, p)
        p.shader = null

        if (down) {
            p.color = 0x33FFFFFF
            c.drawRoundRect(r, rad, rad, p)
        }

        // rim light
        p.style = Paint.Style.STROKE
        p.strokeWidth = dp(0.8f).toFloat()
        p.color = pal.rim
        c.drawRoundRect(r, rad, rad, p)

        // top specular edge
        p.strokeWidth = dp(1.2f).toFloat()
        p.color = pal.spec
        val y = r.top + dp(1.6f)
        c.drawLine(r.left + rad, y, r.right - rad, y, p)

        // label
        val label = k.label
        if (label.isNotEmpty()) {
            tp.color = if (k.type == KT.SPACE) pal.sub else txt
            tp.textSize = when {
                k.type == KT.SPACE -> hh * 0.28f
                k.type == KT.SYM || k.type == KT.PAGE -> hh * 0.30f
                label.length > 2 -> hh * 0.30f
                else -> hh * 0.44f
            }
            val ty = hh / 2f - (tp.descent() + tp.ascent()) / 2f
            c.drawText(label, w / 2f, ty, tp)
        }
    }

    override fun onDetachedFromWindow() {
        h.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = true
                longFired = false
                invalidate()
                if (haptic) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (k.type == KT.DEL) {
                    onTap(k)
                    h.postDelayed(repeat, 400)
                } else if (fireOnDown) {
                    onTap(k)
                } else if (onLong != null) {
                    h.postDelayed(longRun, 450)
                }
            }
            MotionEvent.ACTION_UP -> {
                down = false
                invalidate()
                h.removeCallbacks(repeat)
                h.removeCallbacks(longRun)
                if (!fireOnDown && !longFired) onTap(k)
            }
            MotionEvent.ACTION_CANCEL -> {
                down = false
                invalidate()
                h.removeCallbacks(repeat)
                h.removeCallbacks(longRun)
            }
        }
        return true
    }
}
