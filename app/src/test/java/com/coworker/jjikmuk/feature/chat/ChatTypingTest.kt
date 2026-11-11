package com.coworker.jjikmuk.feature.chat

import com.coworker.jjikmuk.feature.chat.presentation.typingFrameEnds
import org.junit.Assert.*
import org.junit.Test

class ChatTypingTest {
    @Test fun `frame boundaries keep emoji surrogate pairs intact and finish with the complete text`() {
        val text="안녕🙂 우유🥛를 확인해요."
        val ends=typingFrameEnds(text)
        assertEquals(text.length,ends.last())
        assertTrue(ends.zipWithNext().all { (a,b) -> a < b })
        ends.forEach { end -> assertFalse(text[end-1].isHighSurrogate()) }
    }
    @Test fun `long answers have bounded frame counts and empty answers require no frames`() {
        assertTrue(typingFrameEnds("").isEmpty())
        assertTrue(typingFrameEnds("문장".repeat(10000)).size <= 200)
        assertEquals(listOf(1),typingFrameEnds("가"))
    }
}
