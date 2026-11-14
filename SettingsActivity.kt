package com.studioerow.glasskb

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("glasskb", Context.MODE_PRIVATE)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(20f), dp(36f), dp(20f), dp(24f))

        fun title(t: String, size: Float, bold: Boolean = false): TextView {
            val v = TextView(this)
            v.text = t
            v.textSize = size
            if (bold) v.setTypeface(null, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(14f)
            root.addView(v, lp)
            return v
        }

        title("Glass Keyboard", 26f, true)
        title("1. Keyboard chalu koro", 16f, true)
        root.addView(Button(this).apply {
            text = "Enable keyboard"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        title("2. Ei keyboard select koro", 16f, true)
        root.addView(Button(this).apply {
            text = "Choose keyboard"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })

        title("Size", 16f, true)
        val sizeGroup = RadioGroup(this)
        sizeGroup.orientation = RadioGroup.HORIZONTAL
        val sizes = listOf("Small", "Medium", "Big")
        for ((i, n) in sizes.withIndex()) {
            val rb = RadioButton(this)
            rb.id = 100 + i
            rb.text = n
            sizeGroup.addView(rb)
        }
        sizeGroup.check(100 + prefs.getInt("size", 1))
        sizeGroup.setOnCheckedChangeListener { _, id -> prefs.edit().putInt("size", id - 100).apply() }
        root.addView(sizeGroup)

        title("Style", 16f, true)
        val styleGroup = RadioGroup(this)
        styleGroup.orientation = RadioGroup.HORIZONTAL
        val styles = listOf("iOS standard", "Glass")
        for ((i, n) in styles.withIndex()) {
            val rb = RadioButton(this)
            rb.id = 300 + i
            rb.text = n
            styleGroup.addView(rb)
        }
        styleGroup.check(300 + prefs.getInt("style", 0))
        styleGroup.setOnCheckedChangeListener { _, id -> prefs.edit().putInt("style", id - 300).apply() }
        root.addView(styleGroup)

        title("Theme", 16f, true)
        val themeGroup = RadioGroup(this)
        themeGroup.orientation = RadioGroup.HORIZONTAL
        val themes = listOf("Auto (phone)", "Dark", "Light")
        for ((i, n) in themes.withIndex()) {
            val rb = RadioButton(this)
            rb.id = 200 + i
            rb.text = n
            themeGroup.addView(rb)
        }
        themeGroup.check(200 + prefs.getInt("theme", 0))
        themeGroup.setOnCheckedChangeListener { _, id -> prefs.edit().putInt("theme", id - 200).apply() }
        root.addView(themeGroup)

        root.addView(Switch(this).apply {
            text = "Auto capitalize"
            isChecked = prefs.getBoolean("autocap", true)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("autocap", on).apply() }
        })
        root.addView(Switch(this).apply {
            text = "Word suggestions and spelling help"
            isChecked = prefs.getBoolean("suggest", true)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("suggest", on).apply() }
        })
        root.addView(Switch(this).apply {
            text = "Number row"
            isChecked = prefs.getBoolean("numrow", false)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("numrow", on).apply() }
        })
        root.addView(Switch(this).apply {
            text = "Vibration"
            isChecked = prefs.getBoolean("haptic", true)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("haptic", on).apply() }
        })

        title("Test koro", 16f, true)
        val test = EditText(this)
        test.hint = "Ekhane type kore dekho"
        test.gravity = Gravity.TOP
        test.minLines = 3
        root.addView(test)

        val sv = ScrollView(this)
        sv.addView(root)
        setContentView(sv)
    }

    private fun dp(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
}
