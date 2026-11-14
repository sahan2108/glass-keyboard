package com.studioerow.glasskb

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.view.View

fun Context.dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun View.dp(v: Float): Int = context.dp(v)

private fun c(v: Long): Int = v.toInt()

class Pal(val dark: Boolean, val blur: Boolean) {
    val text = c(if (dark) 0xFFFFFFFFL else 0xFF111114L)
    val sub = c(if (dark) 0xAAFFFFFFL else 0x99000000L)
    val panel = c(
        if (dark) (if (blur) 0x66202228L else 0xF01C1C1FL)
        else (if (blur) 0x99E9EBF1L else 0xF0E3E5ECL)
    )
    val panelRim = c(if (dark) 0x33FFFFFFL else 0xCCFFFFFFL)
    val strip = c(if (dark) 0x1AFFFFFFL else 0x66FFFFFFL)
    val keyTop = c(if (dark) 0x45FFFFFFL else 0xF5FFFFFFL)
    val keyBot = c(if (dark) 0x14FFFFFFL else 0xBFFFFFFFL)
    val fnTop = c(if (dark) 0x2AFFFFFFL else 0x99FFFFFFL)
    val fnBot = c(if (dark) 0x0AFFFFFFL else 0x55FFFFFFL)
    val rim = c(if (dark) 0x70FFFFFFL else 0xFFFFFFFFL)
    val spec = c(if (dark) 0xAAFFFFFFL else 0xFFFFFFFFL)
    val shadow = c(if (dark) 0x44000000L else 0x28000000L)
    val accTop = c(0xDD0A84FFL)
    val accBot = c(0xAA0A84FFL)
    val activeTop = c(if (dark) 0xF2FFFFFFL else 0xFF111114L)
    val activeText = c(if (dark) 0xFF111114L else 0xFFFFFFFFL)

    companion object {
        fun make(ctx: Context, p: SharedPreferences): Pal {
            val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            val dark = when (p.getInt("theme", 0)) {
                1 -> true
                2 -> false
                else -> night
            }
            return Pal(dark, Build.VERSION.SDK_INT >= 31)
        }
    }
}
