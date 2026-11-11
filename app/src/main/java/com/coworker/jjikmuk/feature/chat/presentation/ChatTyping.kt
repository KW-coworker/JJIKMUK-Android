package com.coworker.jjikmuk.feature.chat.presentation

import java.text.BreakIterator
import java.util.Locale

/** Bounded animation frames at character boundaries, keeping surrogate pairs intact. */
internal fun typingFrameEnds(text: String, maxFrames: Int = 200): List<Int> {
    require(maxFrames > 0)
    if (text.isEmpty()) return emptyList()
    val iterator = BreakIterator.getCharacterInstance(Locale.KOREAN).apply { setText(text) }
    val step = ((text.length + maxFrames - 1) / maxFrames).coerceAtLeast(1)
    val ends = mutableListOf<Int>()
    var end = 0
    while (end < text.length) {
        val next = iterator.following((end + step - 1).coerceAtMost(text.length - 1))
        end = if (next == BreakIterator.DONE) text.length else next
        ends += end
    }
    return ends
}
