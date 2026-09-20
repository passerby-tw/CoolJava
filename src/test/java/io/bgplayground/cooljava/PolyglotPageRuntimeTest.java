package io.bgplayground.cooljava;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolyglotPageRuntimeTest {
    @Test
    void espressoBytecodeScriptLoadsAndRuns() throws Exception {
        var page = getClass().getResource("/io/bgplayground/cooljava/image-processing.html");
        var document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        document.setDocumentURI(page.toExternalForm());
        var script = document.createElement("script");
        script.setAttribute("type", "application/x-espresso");
        script.setAttribute("src", "guest/ImageProcessor.class");
        script.setAttribute("entry", "io.bgplayground.cooljava.guest.ImageProcessor");
        script.setAttribute("export", "imageProcessor");
        document.appendChild(script);
        try (var runtime = new PolyglotPageRuntime(new RecordingBrowserApi())) {
            assertEquals(1, new PolyglotScriptLoader(new ScriptTypeRouter(), System.err::println).executeAll(document, runtime));
            assertEquals("76,76,76,255", runtime.plugin("imageProcessor")
                .invokeMember("apply", "255,0,0,255", 1, 1, "gray").asString());
        }
    }
    @Test
    void javaSourceIsExecutedByEspressoAndExportedToGraalJs() {
        try (PolyglotPageRuntime runtime = new PolyglotPageRuntime(new RecordingBrowserApi())) {
            runtime.execute("java", """
                    import com.oracle.truffle.espresso.polyglot.Polyglot;
                    public class SourceProbe {
                        public static void main(String[] args) {
                            Polyglot.exportObject("sourceResult", "Espresso source OK");
                        }
                    }
                    """, "SourceProbe.java");
            assertEquals("Espresso source OK", runtime.execute("js",
                    "Polyglot.import('sourceResult')", "probe.js").asString());
        }
    }

    @Test
    void mathjaxActuallyRunsInsideGraalJs() throws Exception {
        try (PolyglotPageRuntime runtime = new PolyglotPageRuntime(new RecordingBrowserApi());
             var input = getClass().getResourceAsStream("/io/bgplayground/cooljava/vendor/mathjax-graal.js")) {
            runtime.execute("js", new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), "mathjax.js");
            String svg = runtime.execute("js", "CoolMath.toSvg('x^2')", "formula.js").asString();
            org.junit.jupiter.api.Assertions.assertTrue(svg.startsWith("<svg"));
        }
    }

    @Test
    void sympyFunctionIsCalledFromGraalJs() throws Exception {
        try (PolyglotPageRuntime runtime = new PolyglotPageRuntime(new RecordingBrowserApi());
             var input = getClass().getResourceAsStream("/io/bgplayground/cooljava/scripts/mathematics.py")) {
            runtime.execute("python", new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), "mathematics.py");
            String derivative = runtime.execute("js", """
                    JSON.parse(String(Polyglot.import('calculate_math')('x**2'))).latex[1]
                    """, "math-test.js").asString();
            assertEquals("2 x", derivative);
        }
    }

    @Test
    void graalJsCallsTheExportedJavaBrowserApi() {
        RecordingBrowserApi browser = new RecordingBrowserApi();

        try (PolyglotPageRuntime runtime = new PolyglotPageRuntime(browser)) {
            runtime.execute("js", """
                    const values = [1, 2, 3, 4];
                    const mean = values.reduce((a, b) => a + b, 0) / values.length;
                    browser.setText("#output", `Mean: ${mean}`);
                    """, "test.js");
        }

        assertEquals(List.of("#output=Mean: 2.5"), browser.writes);
    }

    @Test
    void graalPyCallsTheSameExportedJavaBrowserApi() {
        RecordingBrowserApi browser = new RecordingBrowserApi();

        try (PolyglotPageRuntime runtime = new PolyglotPageRuntime(browser)) {
            runtime.execute("python", """
                    values = [1, 2, 3, 4]
                    mean = sum(values) / len(values)
                    browser.setText("#output", f"Mean: {mean}")
                    """, "test.py");
        }

        assertEquals(List.of("#output=Mean: 2.5"), browser.writes);
    }

    @Test
    void graalPyAndGraalJsCallFunctionsExportedByEachOther() {
        RecordingBrowserApi browser = new RecordingBrowserApi();

        try (PolyglotPageRuntime runtime = new PolyglotPageRuntime(browser)) {
            runtime.execute("python", """
                    import polyglot

                    @polyglot.export_value
                    def python_mean(*values):
                        return sum(values) / len(values)
                    """, "export.py");

            runtime.execute("js", """
                    const pythonMean = Polyglot.import("python_mean");
                    const mean = pythonMean(12, 19, 3, 5, 2, 3);
                    Polyglot.export("js_summary",
                        label => `${label}: ${mean.toFixed(2)}`);
                    """, "import.js");

            runtime.execute("python", """
                    import polyglot

                    js_summary = polyglot.import_value("js_summary")
                    message = js_summary("Cross-language mean")
                    browser.setText("#output", str(message))
                    """, "import-back.py");
        }

        assertEquals(List.of("#output=Cross-language mean: 7.33"), browser.writes);
    }

    private static final class RecordingBrowserApi implements BrowserApi {
        private final List<String> writes = new ArrayList<>();

        @Override
        public void setText(String selector, String text) {
            writes.add(selector + "=" + text);
        }

        @Override
        public void animate(String selector, String animationClass) {
            writes.add(selector + "~" + animationClass);
        }

        @Override
        public String getAttribute(String selector, String name) {
            return "";
        }

        @Override
        public void setAttribute(String selector, String name, String value) {
            writes.add(selector + "@" + name + "=" + value);
        }

        @Override
        public void log(String message) {
        }
    }
}
