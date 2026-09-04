package com.bookmer.browser

import com.bookmer.browser.data.BookmerItem
import com.bookmer.browser.data.BookmerUrls
import com.bookmer.browser.data.ContentViewMode
import com.bookmer.browser.data.SortMode
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmerModelTest {
    @Test fun normalizesPlatformRootAliases() {
        listOf("", "collection", "root", "null").forEach {
            assertEquals(BookmerUrls.ROOT, BookmerItem.normalizedParent(it))
        }
    }

    @Test fun preservesRealFolderIds() = assertEquals("folder-123", BookmerItem.normalizedParent(" folder-123 "))

    @Test fun sortModeLabelsMatchIos() {
        assertEquals("A → Z", SortMode.NAME_AZ.label)
        assertEquals("Z → A", SortMode.NAME_ZA.label)
        assertEquals("New → Old", SortMode.NEWEST.label)
        assertEquals("Old → New", SortMode.OLDEST.label)
    }

    @Test fun viewStyleLabelsMatchIos() {
        assertEquals("Grid", ContentViewMode.GRID.label)
        assertEquals("List", ContentViewMode.LIST.label)
        assertEquals("Thumbnail", ContentViewMode.THUMBNAIL.label)
    }
}
