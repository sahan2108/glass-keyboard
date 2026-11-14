package com.studioerow.glasskb

/** Offline English suggestions: completions while typing, corrections for misspelled words. */
object Spell {
    private val words: List<String> by lazy { Words.all() }
    private val set: HashSet<String> by lazy { HashSet(words) }

    private fun cap(x: String, w: String): String =
        if (x.isNotEmpty() && w.isNotEmpty() && w[0].isUpperCase()) x.replaceFirstChar { it.uppercase() } else x

    private fun dist(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        var p2 = IntArray(n + 1)
        var p1 = IntArray(n + 1) { it }
        var cur = IntArray(n + 1)
        for (i in 1..m) {
            cur[0] = i
            for (j in 1..n) {
                val c = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(p1[j] + 1, cur[j - 1] + 1, p1[j - 1] + c)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, p2[j - 2] + 1)
                cur[j] = v
            }
            val t = p2
            p2 = p1
            p1 = cur
            cur = t
        }
        return p1[n]
    }

    private fun shared(a: String, b: String): Int {
        var p = 0
        while (p < a.length && p < b.length && a[p] == b[p]) p++
        var q = 0
        while (q < a.length - p && q < b.length - p && a[a.length - 1 - q] == b[b.length - 1 - q]) q++
        return p + q
    }

    private fun corrections(l: String, maxD: Int): List<String> {
        val found = ArrayList<Triple<Int, Int, String>>()
        for ((i, x) in words.withIndex()) {
            if (x.length < 2 || Math.abs(x.length - l.length) > maxD) continue
            val d = dist(l, x)
            if (d <= maxD) found.add(Triple(d * 100 - shared(l, x) * 10 - maxOf(0, 40 - i / 10), i, x))
        }
        found.sortWith(compareBy({ it.first }, { it.second }))
        return found.take(3).map { it.third }
    }

    /** Returns [left, center, right]. Center is the best suggestion (shown bold). */
    fun suggest(w: String): List<String> {
        val l = w.lowercase()
        if (l.isEmpty()) return listOf("I", "The", "And")
        val q = "\u201C" + w + "\u201D"
        if (set.contains(l)) {
            val p = words.asSequence().filter { it.length > l.length && it.startsWith(l) }.take(2).toList()
            return listOf(cap(p.getOrElse(1) { "" }, w), w, cap(p.getOrElse(0) { "" }, w))
        }
        val pre = words.asSequence().filter { it.startsWith(l) }.take(2).toList()
        if (pre.isNotEmpty()) {
            if (l.length >= 5) {
                val c1 = corrections(l, 1)
                if (c1.isNotEmpty()) {
                    val r = if (c1.size > 1) c1[1] else if (pre[0] != c1[0]) pre[0] else ""
                    return listOf(q, cap(c1[0], w), cap(r, w))
                }
            }
            return listOf(q, cap(pre[0], w), cap(pre.getOrElse(1) { "" }, w))
        }
        val c = corrections(l, if (l.length <= 3) 1 else 2)
        if (c.isEmpty()) return listOf("", q, "")
        return listOf(q, cap(c[0], w), cap(c.getOrElse(1) { "" }, w))
    }
}
