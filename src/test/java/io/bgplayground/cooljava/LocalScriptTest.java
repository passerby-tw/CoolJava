package io.bgplayground.cooljava;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalScriptTest {
    @TempDir Path temp;
    @Test void confinesExternalSourcesToPageDirectory() throws Exception {
        Path pageDir = Files.createDirectory(temp.resolve("page"));
        Path page = Files.writeString(pageDir.resolve("index.html"), "");
        Path source = Files.writeString(pageDir.resolve("script.js"), "");
        Files.writeString(temp.resolve("outside.js"), "");
        assertEquals(source.toRealPath(), PolyglotScriptLoader.localSource(page.toUri().toString(), "script.js"));
        assertThrows(IllegalArgumentException.class, () -> PolyglotScriptLoader.localSource(page.toUri().toString(), "../outside.js"));
        assertThrows(IllegalArgumentException.class, () -> PolyglotScriptLoader.localSource(page.toUri().toString(), "https://example.com/x.js"));
    }
    @Test void javaSourceTypesAreRouted() {
        assertEquals("java", new ScriptTypeRouter().languageFor("application/x-java").orElseThrow());
        assertEquals("java", new ScriptTypeRouter().languageFor("text/x-java").orElseThrow());
    }
}
