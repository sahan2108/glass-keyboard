package com.studioerow.glasskb

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.view.View

fun Context.dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun View.dp(v: Float): Int = context.dp(v)

private fun c(v: Long): Int = v.toInt()

/** Colors for both styles. glass = false is the flat iOS standard look. */
class Pal(val dark: Boolean, val blur: Boolean, val glass: Boolean) {
    val text = c(if (dark) 0xFFFFFFFFL else 0xFF000000L)
    val sub = c(if (dark) 0x99FFFFFFL else 0x80000000L)
    val sep = c(if (dark) 0x33FFFFFFL else 0x24000000L)
    val act = c(0xFF0A84FFL)
    val activeBg = c(if (glass && dark) 0xF2FFFFFFL else 0xFFFFFFFFL)
    val activeText = c(0xFF111114L)

    val panel: Int
    val key: Int
    val fn: Int
    val press: Int
    val rim: Int
    val line: Int
    val popBg: Int

    init {
        if (!glass) {
            panel = c(if (dark) 0xFF2B2B2EL else 0xFFD1D4DBL)
            key = c(if (dark) 0xFF6B6B6FL else 0xFFFFFFFFL)
            fn = c(if (dark) 0xFF46464AL else 0xFFADB3BCL)
            press = c(if (dark) 0xFF8A8A8FL else 0xFFA9AEB7L)
            rim = 0
            line = c(if (dark) 0x8C000000L else 0x52000000L)
            popBg = key
        } else {
            panel = c(
                if (dark) (if (blur) 0x8C2C2C32L else 0xF02C2C32L)
                else (if (blur) 0x8CD6D9E2L else 0xF2D6D9E2L)
            )
            key = c(if (dark) 0x33FFFFFFL else 0x9EFFFFFFL)
            fn = c(if (dark) 0x17FFFFFFL else 0x47FFFFFFL)
            press = c(if (dark) 0x66FFFFFFL else 0xF2FFFFFFL)
            rim = c(if (dark) 0x52FFFFFFL else 0xE6FFFFFFL)
            line = c(if (dark) 0x59000000L else 0x24000000L)
            popBg = c(if (dark) 0xF2464850L else 0xF2FFFFFFL)
        }
    }

    companion object {
        fun make(ctx: Context, p: SharedPreferences): Pal {
            val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            val dark = when (p.getInt("theme", 0)) {
                1 -> true
                2 -> false
                else -> night
            }
            return Pal(dark, Build.VERSION.SDK_INT >= 31, p.getInt("style", 0) == 1)
        }
    }
}
