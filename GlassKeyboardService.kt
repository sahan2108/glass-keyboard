package com.studioerow.glasskb

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

class GlassKeyboardService : InputMethodService() {

    lateinit var prefs: SharedPreferences
    var lang = Lang.EN
    var shift = 0          // 0 off, 1 once, 2 caps lock
    var page = 0           // 0 letters, 1 symbols, 2 more symbols

    private var view: GlassKeyboardView? = null
    private val avro = StringBuilder()
    private var lastShiftTap = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("glasskb", Context.MODE_PRIVATE)
        lang = Lang.values().getOrElse(prefs.getInt("lang", 0)) { Lang.EN }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val v = GlassKeyboardView(this, this)
        view = v
        return v
    }

    override fun onWindowShown() {
        super.onWindowShown()
        applyWindowGlass()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        avro.setLength(0)
        page = 0
        if (shift == 2) shift = 0
        updateAutoCaps()
        view?.rebuild()
        applyWindowGlass()
        updateSuggestions()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        avro.setLength(0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setInputView(onCreateInputView())
        applyWindowGlass()
    }

    /** Frosted glass panel. Real blur-behind works on Android 12+ devices that allow it. */
    private fun applyWindowGlass() {
        try {
            val w = window?.window ?: return
            val pal = Pal.make(this, prefs)
            val r = dp(28f).toFloat()
            val bg = GradientDrawable().apply {
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setColor(pal.panel)
                setStroke(dp(1f), pal.panelRim)
            }
            w.setFormat(PixelFormat.TRANSLUCENT)
            w.setBackgroundDrawable(bg)
            if (Build.VERSION.SDK_INT >= 31) w.setBackgroundBlurRadius(prefs.getInt("blur", 80))
        } catch (_: Throwable) {
        }
    }

    // ---------- key handling ----------

    fun onKey(k: K) {
        when (k.type) {
            KT.CHAR -> typeText(k.out)
            KT.SPACE -> { flushAvro(); commit(" "); afterCommit() }
            KT.DEL -> del()
            KT.ENTER -> enter()
            KT.SHIFT -> {
                val now = SystemClock.uptimeMillis()
                shift = when {
                    shift == 2 -> 0
                    shift == 1 && now - lastShiftTap < 400 -> 2
                    shift == 1 -> 0
                    else -> 1
                }
                lastShiftTap = now
                view?.post { view?.rebuild() }
            }
            KT.SYM -> { page = if (page == 0) 1 else 0; view?.post { view?.rebuild() } }
            KT.PAGE -> { page = if (page == 1) 2 else 1; view?.post { view?.rebuild() } }
            KT.LANG -> {
                flushAvro()
                lang = Lang.values()[(lang.ordinal + 1) % Lang.values().size]
                prefs.edit().putInt("lang", lang.ordinal).apply()
                shift = 0
                page = 0
                updateSuggestions()
                view?.post { view?.rebuild() }
            }
            KT.SPACER -> {}
        }
    }

    private fun typeText(out: String) {
        val ic = currentInputConnection ?: return
        if (lang == Lang.AVRO) {
            val ch = out[0]
            val roman = out.length == 1 && ((ch in 'a'..'z') || (ch in 'A'..'Z') || ch == '^' || ch == ':' || ch == '`')
            if (roman) {
                avro.append(ch)
                showComposing()
                consumeShift()
                return
            }
            flushAvro()
            ic.commitText(if (out == ".") "।" else out, 1)
        } else {
            ic.commitText(out, 1)
        }
        consumeShift()
        afterCommit()
    }

    fun commit(text: String) {
        flushAvro()
        currentInputConnection?.commitText(text, 1)
        afterCommit()
    }

    private fun showComposing() {
        val ic = currentInputConnection ?: return
        val bn = AvroEngine.convert(avro.toString())
        ic.setComposingText(bn, 1)
        view?.setSuggestions(listOf(avro.toString(), bn, ""))
    }

    private fun flushAvro() {
        if (avro.isNotEmpty()) {
            currentInputConnection?.finishComposingText()
            avro.setLength(0)
        }
    }

    private fun del() {
        val ic = currentInputConnection ?: return
        if (avro.isNotEmpty()) {
            avro.setLength(avro.length - 1)
            if (avro.isEmpty()) {
                ic.setComposingText("", 1)
                ic.finishComposingText()
                updateSuggestions()
            } else {
                showComposing()
            }
            return
        }
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        updateSuggestions()
    }

    private fun enter() {
        flushAvro()
        val ei = currentInputEditorInfo
        val opts = ei?.imeOptions ?: 0
        val action = opts and EditorInfo.IME_MASK_ACTION
        val noAction = (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            currentInputConnection?.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        afterCommit()
    }

    private fun consumeShift() {
        if (shift == 1) {
            shift = 0
            view?.post { view?.rebuild() }
        }
    }

    private fun afterCommit() {
        updateAutoCaps()
        updateSuggestions()
    }

    private fun updateAutoCaps() {
        if (lang != Lang.EN || shift == 2) return
        val ei = currentInputEditorInfo ?: return
        val ic = currentInputConnection ?: return
        val want = if (ei.inputType != 0 && ic.getCursorCapsMode(ei.inputType) != 0) 1 else 0
        if (want != shift) {
            shift = want
            view?.post { view?.rebuild() }
        }
    }

    // ---------- suggestions ----------

    private fun wordBeforeCursor(): String {
        val before = currentInputConnection?.getTextBeforeCursor(24, 0)?.toString() ?: ""
        return before.takeLastWhile { it.isLetter() || it == '\'' }
    }

    fun updateSuggestions() {
        if (lang != Lang.EN) {
            if (avro.isEmpty()) view?.setSuggestions(listOf("", "", ""))
            return
        }
        val w = wordBeforeCursor()
        val list = if (w.isEmpty()) {
            listOf("I", "The", "And")
        } else {
            val m = Layouts.words.filter { it.startsWith(w, ignoreCase = true) && !it.equals(w, ignoreCase = true) }
            val cap = w[0].isUpperCase()
            val fixed = m.take(2).map { if (cap) it.replaceFirstChar { ch -> ch.uppercase() } else it }
            listOf(fixed.getOrElse(1) { "" }, w, fixed.getOrElse(0) { "" })
        }
        view?.setSuggestions(list)
    }

    fun pickSuggestion(text: String) {
        if (text.isBlank()) return
        val ic = currentInputConnection ?: return
        if (lang == Lang.AVRO && avro.isNotEmpty()) {
            ic.setComposingText(text, 1)
            ic.finishComposingText()
            avro.setLength(0)
            ic.commitText(" ", 1)
            afterCommit()
            return
        }
        val w = wordBeforeCursor()
        if (w.isNotEmpty()) ic.deleteSurroundingText(w.length, 0)
        ic.commitText("$text ", 1)
        afterCommit()
    }

    // ---------- tools ----------

    fun showPicker() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    fun pasteClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text ?: return
        commit(t.toString())
    }

    fun cycleSize() {
        prefs.edit().putInt("size", (prefs.getInt("size", 1) + 1) % 3).apply()
    }

    fun cycleTheme() {
        prefs.edit().putInt("theme", (prefs.getInt("theme", 0) + 1) % 3).apply()
        applyWindowGlass()
    }

    fun toggleNumRow() {
        prefs.edit().putBoolean("numrow", !prefs.getBoolean("numrow", false)).apply()
    }

    fun openSettings() {
        val i = Intent(this, SettingsActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }
}
