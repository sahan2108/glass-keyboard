package com.studioerow.glasskb

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
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
    private val onLong: ((K) -> Unit)?,
    private val hot: Boolean = false
) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val ip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
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

    private fun isFn() = k.type == KT.SHIFT || k.type == KT.DEL || k.type == KT.SYM ||
            k.type == KT.PAGE || k.type == KT.LANG || k.type == KT.ENTER

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val hh = height.toFloat()
        val mx = dp(3f).toFloat()
        val r = RectF(mx, dp(5f).toFloat(), w - mx, hh - dp(6f))
        val rad = if (pal.glass) dp(12f).toFloat() else dp(9f).toFloat()

        var fill = when {
            k.type == KT.ENTER && hot -> pal.act
            k.type == KT.SHIFT && active -> pal.activeBg
            isFn() -> pal.fn
            else -> pal.key
        }
        if (down && !(k.type == KT.ENTER && hot)) fill = pal.press
        var txt = pal.text
        if (k.type == KT.ENTER && hot) txt = 0xFFFFFFFF.toInt()
        if (k.type == KT.SHIFT && active) txt = pal.activeText

        // thin bottom shadow line, drawn only outside the key
        val path = Path()
        path.addRoundRect(r, rad, rad, Path.Direction.CW)
        c.save()
        c.clipPath(path, Region.Op.DIFFERENCE)
        p.style = Paint.Style.FILL
        p.color = pal.line
        c.drawRoundRect(RectF(r.left, r.top + dp(1f), r.right, r.bottom + dp(1f)), rad, rad, p)
        c.restore()

        // key body
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawRoundRect(r, rad, rad, p)

        if (pal.glass) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = dp(0.7f).toFloat()
            p.color = pal.rim
            c.drawRoundRect(r, rad, rad, p)
        }

        val cx = w / 2f
        val cy = (r.top + r.bottom) / 2f
        val kh = r.height()
        when (k.type) {
            KT.SHIFT, KT.DEL, KT.ENTER, KT.LANG -> drawIcon(c, cx, cy, kh * 0.24f, txt)
            else -> {
                val label = k.label
                if (label.isNotEmpty()) {
                    tp.color = if (k.type == KT.SPACE) pal.sub else txt
                    tp.textSize = when {
                        k.type == KT.SPACE -> kh * 0.34f
                        k.type == KT.SYM || k.type == KT.PAGE -> kh * 0.36f
                        label.length > 2 -> kh * 0.36f
                        else -> kh * 0.56f
                    }
                    val ty = cy - (tp.descent() + tp.ascent()) / 2f
                    c.drawText(label, cx, ty, tp)
                }
            }
        }
    }

    private fun drawIcon(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        ip.color = color
        ip.strokeWidth = dp(1.7f).toFloat()
        val pa = Path()
        when (k.type) {
            KT.SHIFT -> {
                pa.moveTo(cx, cy - s)
                pa.lineTo(cx + s, cy + s * 0.05f)
                pa.lineTo(cx + s * 0.45f, cy + s * 0.05f)
                pa.lineTo(cx + s * 0.45f, cy + s * 0.85f)
                pa.lineTo(cx - s * 0.45f, cy + s * 0.85f)
                pa.lineTo(cx - s * 0.45f, cy + s * 0.05f)
                pa.lineTo(cx - s, cy + s * 0.05f)
                pa.close()
                ip.style = if (active) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
                c.drawPath(pa, ip)
                if (k.label == "⇪") {
                    ip.style = Paint.Style.STROKE
                    c.drawLine(cx - s * 0.45f, cy + s * 1.25f, cx + s * 0.45f, cy + s * 1.25f, ip)
                }
            }
            KT.DEL -> {
                ip.style = if (down) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
                pa.moveTo(cx - s * 1.15f, cy)
                pa.lineTo(cx - s * 0.45f, cy - s * 0.8f)
                pa.lineTo(cx + s * 1.1f, cy - s * 0.8f)
                pa.lineTo(cx + s * 1.1f, cy + s * 0.8f)
                pa.lineTo(cx - s * 0.45f, cy + s * 0.8f)
                pa.close()
                c.drawPath(pa, ip)
                ip.style = Paint.Style.STROKE
                ip.color = fillOrContrast()
                val x = cx + s * 0.3f
                val d = s * 0.32f
                c.drawLine(x - d, cy - d, x + d, cy + d, ip)
                c.drawLine(x - d, cy + d, x + d, cy - d, ip)
            }
            KT.ENTER -> {
                ip.style = Paint.Style.STROKE
                pa.moveTo(cx + s, cy - s * 0.75f)
                pa.lineTo(cx + s, cy + s * 0.1f)
                pa.lineTo(cx - s, cy + s * 0.1f)
                c.drawPath(pa, ip)
                val a = Path()
                a.moveTo(cx - s * 0.45f, cy - s * 0.45f)
                a.lineTo(cx - s, cy + s * 0.1f)
                a.lineTo(cx - s * 0.45f, cy + s * 0.65f)
                c.drawPath(a, ip)
            }
            KT.LANG -> {
                ip.style = Paint.Style.STROKE
                c.drawCircle(cx, cy, s, ip)
                c.drawOval(RectF(cx - s * 0.45f, cy - s, cx + s * 0.45f, cy + s), ip)
                c.drawLine(cx - s, cy, cx + s, cy, ip)
                c.drawLine(cx - s * 0.85f, cy - s * 0.5f, cx + s * 0.85f, cy - s * 0.5f, ip)
                c.drawLine(cx - s * 0.85f, cy + s * 0.5f, cx + s * 0.85f, cy + s * 0.5f, ip)
            }
            else -> {}
        }
    }

    private fun fillOrContrast(): Int = if (down) pal.press else pal.fn

    private fun showPop() {
        if (k.type != KT.CHAR || k.label.isEmpty() || k.label.length > 2) return
        try {
            val tv = TextView(context)
            tv.text = k.label
            tv.textSize = 30f
            tv.gravity = Gravity.CENTER
            tv.setTextColor(pal.text)
            val bg = GradientDrawable()
            bg.cornerRadius = dp(12f).toFloat()
            bg.setColor(pal.popBg)
            if (pal.glass) bg.setStroke(dp(0.7f), pal.rim)
            tv.background = bg
            val pw = (width * 1.3f).toInt()
            val ph = (height * 1.15f).toInt()
            val pp = PopupWindow(tv, pw, ph)
            pp.isTouchable = false
            pp.isFocusable = false
            pp.elevation = dp(6f).toFloat()
            pp.showAsDropDown(this, -(pw - width) / 2, -(height + ph) + dp(10f))
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
