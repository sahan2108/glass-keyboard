package com.studioerow.glasskb

enum class Lang(val title: String) {
    EN("English"),
    AVRO("বাংলা · অভ্র"),
    BIJOY("বাংলা · বিজয়")
}

enum class KT { CHAR, SHIFT, DEL, ENTER, SPACE, SYM, PAGE, LANG, SPACER }

data class K(val label: String, val out: String = label, val type: KT = KT.CHAR, val w: Float = 1f)

object Layouts {
    private const val BN_DIGITS = "১২৩৪৫৬৭৮৯০"

    private fun lab(s: String): String =
        if (s.isNotEmpty() && "ািীুূৃেৈোৌ্ৗ".indexOf(s[0]) >= 0) "\u25CC" + s else s

    private fun plain(s: String): List<K> = s.map { K(it.toString()) }
    private fun bijoy(a: List<String>): List<K> = a.map { K(lab(it), it) }
    private fun spacer(w: Float) = K("", "", KT.SPACER, w)

    // Bijoy layout (normal / shift). Vowel signs are typed AFTER the consonant (Unicode order).
    private val bjN = listOf(
        listOf("ঙ", "য", "ড", "প", "ট", "চ", "জ", "হ", "গ", "ড়"),
        listOf("ৃ", "ু", "ি", "া", "্", "ব", "ক", "ত", "দ"),
        listOf("্র", "ও", "ে", "র", "ন", "স", "ম")
    )
    private val bjS = listOf(
        listOf("ং", "য়", "ঢ", "ফ", "ঠ", "ছ", "ঝ", "ঞ", "ঘ", "ঢ়"),
        listOf("ঋ", "ূ", "ী", "অ", "।", "ভ", "খ", "থ", "ধ"),
        listOf("্য", "ৗ", "ৈ", "ল", "ণ", "ষ", "শ")
    )

    fun rows(lang: Lang, shift: Boolean, page: Int, numRow: Boolean): List<List<K>> {
        val out = ArrayList<List<K>>()
        val del = K("⌫", "", KT.DEL, 1.4f)
        if (page == 0) {
            if (numRow) out.add(plain(if (lang == Lang.EN) "1234567890" else BN_DIGITS))
            if (lang == Lang.BIJOY) {
                val r = if (shift) bjS else bjN
                out.add(bijoy(r[0]))
                out.add(listOf(spacer(0.5f)) + bijoy(r[1]) + spacer(0.5f))
                out.add(listOf(K("⇧", "", KT.SHIFT, 1.4f)) + bijoy(r[2]) + del)
            } else {
                fun up(s: String) = if (shift) s.uppercase() else s
                out.add(plain(up("qwertyuiop")))
                out.add(listOf(spacer(0.5f)) + plain(up("asdfghjkl")) + spacer(0.5f))
                out.add(listOf(K("⇧", "", KT.SHIFT, 1.4f)) + plain(up("zxcvbnm")) + del)
            }
        } else {
            val bn = lang != Lang.EN
            if (page == 1) {
                out.add(plain(if (bn) BN_DIGITS else "1234567890"))
                out.add(plain(if (bn) "ঁঃৎ৳।,;:?!" else "-/:;()\$&@\""))
                out.add(listOf(K("#+=", "", KT.PAGE, 1.4f), spacer(0.5f)) +
                        plain(if (bn) "-/()\"'@" else ".,?!'") + spacer(0.5f) + del)
            } else {
                out.add(plain("[]{}#%^*+="))
                out.add(plain("_\\|~<>€£•`"))
                out.add(listOf(K("123", "", KT.PAGE, 1.4f), spacer(0.5f)) +
                        plain(".,?!'") + spacer(0.5f) + del)
            }
        }
        val bn = lang != Lang.EN
        out.add(listOf(
            K(if (page == 0) "123" else "ABC", "", KT.SYM, 1.4f),
            K("🌐", "", KT.LANG, 1.1f),
            K(",", ",", KT.CHAR, 1f),
            K(lang.title, "", KT.SPACE, 3.3f),
            K(if (bn) "।" else ".", if (bn) "।" else ".", KT.CHAR, 1f),
            K("↵", "", KT.ENTER, 1.6f)
        ))
        return out
    }

    val words = listOf(
        "the", "this", "that", "there", "then", "they", "them", "thank", "thanks", "to", "too",
        "hello", "hi", "how", "have", "has", "had", "here", "he", "her", "his", "help",
        "you", "your", "yes", "yeah", "year", "what", "when", "where", "which", "who", "why", "will", "with", "would",
        "please", "project", "client", "design", "video", "editing", "send", "sent", "sure", "okay",
        "and", "are", "about", "after", "again", "all", "also", "any", "as", "at", "be", "because",
        "been", "but", "by", "can", "could", "do", "does", "done", "for", "from", "get", "give",
        "go", "good", "great", "if", "in", "is", "it", "just", "know", "let", "like", "love",
        "make", "me", "more", "my", "need", "no", "not", "now", "of", "on", "one", "or", "our",
        "out", "so", "some", "tell", "time", "today", "tomorrow", "up", "us", "want", "was", "we",
        "well", "were", "work", "working", "message", "meeting", "morning", "night", "number",
        "payment", "price", "ready", "revision", "review", "right", "see", "soon", "still", "take"
    )
}
