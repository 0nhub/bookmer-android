package com.bookmer.browser

import com.bookmer.browser.ui.addressFieldValueOnFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressFieldFocusTest {
    @Test
    fun emptyStaysEmpty() {
        val value = addressFieldValueOnFocus("")
        assertEquals("", value.text)
        assertTrue(value.selection.collapsed)
    }

    @Test
    fun urlIsFullySelected() {
        val url = "https://example.com/path"
        val value = addressFieldValueOnFocus(url)
        assertEquals(url, value.text)
        assertEquals(0, value.selection.start)
        assertEquals(url.length, value.selection.end)
    }
}
