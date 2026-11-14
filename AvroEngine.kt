package com.studioerow.glasskb

/** Phonetic (Avro style) Roman to Bangla converter. Pure Kotlin, no Android deps. */
object AvroEngine {
    private class Tok(val out: String, val kind: Int, val sign: String = "")

    private val map = HashMap<String, Tok>()

    private fun c(k: String, v: String) { map[k] = Tok(v, 0) }
    private fun v(k: String, ind: String, sign: String) { map[k] = Tok(ind, 1, sign) }
    private fun o(k: String, v: String) { map[k] = Tok(v, 2) }

    init {
        // consonants
        c("kkh", "ক্ষ"); c("GG", "জ্ঞ")
        c("Ng", "ঙ"); c("NG", "ঞ"); c("Th", "ঠ"); c("Dh", "ঢ"); c("Rh", "ঢ়")
        c("kh", "খ"); c("gh", "ঘ"); c("ch", "ছ"); c("jh", "ঝ"); c("th", "থ")
        c("dh", "ধ"); c("bh", "ভ"); c("ph", "ফ"); c("sh", "শ"); c("Sh", "ষ")
        c("k", "ক"); c("g", "গ"); c("c", "চ"); c("j", "জ"); c("T", "ট")
        c("D", "ড"); c("N", "ণ"); c("t", "ত"); c("d", "দ"); c("n", "ন")
        c("p", "প"); c("f", "ফ"); c("b", "ব"); c("v", "ভ"); c("m", "ম")
        c("z", "য"); c("r", "র"); c("l", "ল"); c("s", "স"); c("S", "শ")
        c("h", "হ"); c("R", "ড়"); c("y", "য়"); c("q", "ক"); c("x", "ক্স")
        // vowels: independent form, kar (sign)
        v("OI", "ঐ", "ৈ"); v("OU", "ঔ", "ৌ"); v("rri", "ঋ", "ৃ")
        v("ii", "ঈ", "ী"); v("ee", "ঈ", "ী"); v("I", "ঈ", "ী")
        v("oo", "উ", "ু"); v("uu", "ঊ", "ূ"); v("U", "ঊ", "ূ"); v("u", "উ", "ু")
        v("e", "এ", "ে"); v("i", "ই", "ি"); v("a", "আ", "া")
        v("o", "অ", ""); v("O", "ও", "ো")
        // others
        o("ng", "ং"); o("^", "ঁ"); o(":", "ঃ"); o("t`", "ৎ"); o("`", "")
        val bn = "০১২৩৪৫৬৭৮৯"
        for (d in 0..9) o(d.toString(), bn[d].toString())
    }

    private val maxLen = map.keys.maxOf { it.length }

    fun convert(src: String): String {
        val sb = StringBuilder()
        var prevCons = false
        var i = 0
        while (i < src.length) {
            var tok: Tok? = null
            var len = 0
            var l = minOf(maxLen, src.length - i)
            while (l > 0) {
                val t = map[src.substring(i, i + l)]
                if (t != null) { tok = t; len = l; break }
                l--
            }
            if (tok == null) {
                sb.append(src[i]); prevCons = false; i++
                continue
            }
            when (tok.kind) {
                0 -> {
                    if (prevCons && src.substring(i, i + len) == "y") {
                        sb.append("্য")
                    } else {
                        if (prevCons) sb.append('\u09CD')
                        sb.append(tok.out)
                    }
                    prevCons = true
                }
                1 -> {
                    if (prevCons) sb.append(tok.sign) else sb.append(tok.out)
                    prevCons = false
                }
                else -> { sb.append(tok.out); prevCons = false }
            }
            i += len
        }
        return sb.toString()
    }
}
