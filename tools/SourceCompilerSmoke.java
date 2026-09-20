import io.bgplayground.cooljava.JavaSourceCompiler;
import java.nio.file.*;

public final class SourceCompilerSmoke {
    public static void main(String[] args) throws Exception {
        Path output;
        try (var compiler = new JavaSourceCompiler()) {
            output = compiler.output();
            compiler.inline("Helper.java", "class Helper { static int n() { return 7; } }");
            var main = compiler.inline("Main.java", "public class Main { public static void main(String[] args) { System.out.println(Helper.n()); } }");
            compiler.compile(main);
            if (!Files.exists(output.resolve("Helper.class"))) throw new AssertionError("multi-file compilation failed");
            compiler.compile(compiler.inline("Instance.java", "public class Instance { void main() {} }"));
            try {
                compiler.compile(compiler.inline("Broken.java", "class Broken { invalid syntax here; }"));
                throw new AssertionError("invalid source accepted");
            } catch (IllegalArgumentException expected) {
                if (!expected.getMessage().contains("Broken.java:1:")) throw expected;
            }
            try {
                compiler.inline("../Escape.java", "");
                throw new AssertionError("path traversal accepted");
            } catch (IllegalArgumentException expected) { }
        }
        if (Files.exists(output)) throw new AssertionError("page compilation cache leaked");
        System.out.println("PASS: multi-file source, instance main compilation, diagnostics, path validation, cleanup");
    }
}
