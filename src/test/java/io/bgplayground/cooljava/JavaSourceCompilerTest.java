package io.bgplayground.cooljava;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class JavaSourceCompilerTest {
    @Test void compilesHelperSourceAndCleansOutput() throws Exception {
        java.nio.file.Path output;
        try (var compiler = new JavaSourceCompiler()) {
            output = compiler.output();
            compiler.inline("Helper.java", "class Helper { static int n() { return 3; } }");
            compiler.compile(compiler.inline("Entry.java", "public class Entry { public static int n() { return Helper.n(); } }"));
            assertTrue(Files.exists(output.resolve("Entry.class")));
            assertTrue(Files.exists(output.resolve("Helper.class")));
        }
        assertFalse(Files.exists(output));
    }
    @Test void rejectsTraversalAndReportsLineNumbers() {
        try (var compiler = new JavaSourceCompiler()) {
            assertThrows(IllegalArgumentException.class, () -> compiler.inline("../Oops.java", ""));
            var error = assertThrows(IllegalArgumentException.class, () ->
                    compiler.compile(compiler.inline("Broken.java", "class Broken { not valid; }")));
            assertTrue(error.getMessage().contains("Broken.java:1:"));
        }
    }
    @Test void compilesCompactSourceOnJdk25() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Runtime.version().feature() >= 25);
        try (var compiler = new JavaSourceCompiler()) {
            compiler.compile(compiler.inline("Compact.java", "void main() { System.out.println(42); }"));
            assertTrue(Files.exists(compiler.output().resolve("Compact.class")));
        }
    }
}
