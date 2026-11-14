package com.studioerow.glasskb

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView

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
    private var pop: PopupWindow? = null
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
        val m = dp(3f).toFloat()
        val r = RectF(m, m, w - m, hh - m - dp(1.5f))
        val rad = hh * 0.26f

        val isFn = k.type == KT.SHIFT || k.type == KT.DEL || k.type == KT.SYM ||
                k.type == KT.PAGE || k.type == KT.LANG
        var top: Int
        var bot: Int
        var txt = pal.text
        when {
            k.type == KT.ENTER -> { top = pal.accTop; bot = pal.accBot; txt = 0xFFFFFFFF.toInt() }
            k.type == KT.SHIFT && active -> { top = pal.activeTop; bot = pal.activeTop; txt = pal.activeText }
            isFn -> { top = pal.fnTop; bot = pal.fnBot }
            else -> { top = pal.keyTop; bot = pal.keyBot }
        }
        if (down && k.type != KT.ENTER) { top = pal.pressTop; bot = pal.pressBot }

        // soft shadow, drawn only outside the key so the glass body stays clean
        val path = Path()
        path.addRoundRect(r, rad, rad, Path.Direction.CW)
        c.save()
        c.clipPath(path, Region.Op.DIFFERENCE)
        p.shader = null
        p.style = Paint.Style.FILL
        val sa = Color.alpha(pal.shadow)
        for (i in 1..3) {
            val g = dp(0.8f).toFloat() * i
            val dy = dp(1.2f).toFloat()
            p.color = pal.shadow
            p.alpha = sa * (4 - i) / 5
            c.drawRoundRect(RectF(r.left - g, r.top + dy, r.right + g, r.bottom + dy + g), rad + g, rad + g, p)
        }
        c.restore()

        // glass body
        p.style = Paint.Style.FILL
        p.color = 0xFFFFFFFF.toInt()
        p.shader = LinearGradient(0f, r.top, 0f, r.bottom, top, bot, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, rad, rad, p)
        p.shader = null

        // thin dark outer edge, then bright rim inside it
        p.style = Paint.Style.STROKE
        p.strokeWidth = dp(0.7f).toFloat()
        p.color = pal.edge
        c.drawRoundRect(r, rad, rad, p)
        val ins = dp(0.7f).toFloat()
        p.color = pal.rim
        c.drawRoundRect(RectF(r.left + ins, r.top + ins, r.right - ins, r.bottom - ins), rad - ins, rad - ins, p)

        // top specular edge
        if (!down) {
            p.strokeWidth = dp(1.2f).toFloat()
            p.color = pal.spec
            val y = r.top + dp(1.8f)
            c.drawLine(r.left + rad, y, r.right - rad, y, p)
        }

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

    private fun showPop() {
        if (k.type != KT.CHAR || k.label.isEmpty() || k.label.length > 2) return
        try {
            val tv = TextView(context)
            tv.text = k.label
            tv.textSize = 28f
            tv.gravity = Gravity.CENTER
            tv.setTextColor(pal.text)
            val bg = GradientDrawable()
            bg.cornerRadius = dp(14f).toFloat()
            bg.setColor(pal.popBg)
            bg.setStroke(dp(0.7f), pal.edge)
            tv.background = bg
            val pw = (width * 1.35f).toInt()
            val ph = (height * 1.45f).toInt()
            val pp = PopupWindow(tv, pw, ph)
            pp.isTouchable = false
            pp.isFocusable = false
            pp.showAsDropDown(this, -(pw - width) / 2, -(height + ph) + dp(6f))
            pop = pp
        } catch (_: Throwable) {
        }
    }

    private fun hidePop() {
        try { pop?.dismiss() } catch (_: Throwable) { }
        pop = null
    }

    private fun release() {
        down = false
        hidePop()
        animate().scaleX(1f).scaleY(1f).setDuration(90).start()
        invalidate()
        h.removeCallbacks(repeat)
        h.removeCallbacks(longRun)
    }

    override fun onDetachedFromWindow() {
        h.removeCallbacksAndMessages(null)
        hidePop()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = true
                longFired = false
                animate().scaleX(0.9f).scaleY(0.9f).setDuration(60).start()
                invalidate()
                if (haptic) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                showPop()
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
                val fire = !fireOnDown && !longFired
                release()
                if (fire) onTap(k)
            }
            MotionEvent.ACTION_CANCEL -> release()
        }
        return true
    }
}
