package io.bgplayground.cooljava;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptTypeRouterTest {
    private final ScriptTypeRouter router = new ScriptTypeRouter();

    @Test
    void routesCustomGraalJsTypeToJavaScriptLanguage() {
        assertEquals("js", router.languageFor("application/x-graaljs").orElseThrow());
    }

    @Test
    void routesCustomGraalPyTypeToPythonLanguage() {
        assertEquals("python",
                router.languageFor("application/x-graalpy").orElseThrow());
    }

    @Test
    void treatsTheTypeCaseInsensitivelyAndIgnoresOuterWhitespace() {
        assertEquals("js", router.languageFor("  Application/X-GraalJS  ").orElseThrow());
    }

    @Test
    void leavesBrowserJavaScriptToTheEmbeddedWebEngine() {
        assertTrue(router.languageFor("text/javascript").isEmpty());
        assertTrue(router.languageFor("").isEmpty());
        assertTrue(router.languageFor(null).isEmpty());
    }
}
