package io.bgplayground.cooljava;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaFxBrowserApiTest {
    @Test
    void acceptsPlainIdsAndLegacyHashIds() {
        assertEquals("output", JavaFxBrowserApi.elementId("output"));
        assertEquals("output", JavaFxBrowserApi.elementId("#output"));
    }

    @Test
    void rejectsCssSelectorsBecauseTheBridgeIsIdOnly() {
        assertThrows(IllegalArgumentException.class,
                () -> JavaFxBrowserApi.elementId("#panel .output"));
    }

    @Test
    void handlesJavaFxDomElementsWithoutAClassAttribute() {
        assertEquals("", JavaFxBrowserApi.removeClass(null, "wave-a"));
        assertEquals("wave-a", JavaFxBrowserApi.addClass(null, "wave-a"));
    }
}
