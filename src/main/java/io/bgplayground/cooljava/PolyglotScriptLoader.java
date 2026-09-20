package io.bgplayground.cooljava;

import org.w3c.dom.*;
import java.net.URI;
import java.nio.file.*;
import java.util.function.Consumer;

/** Local scripts only, in document order. Failures stop dependent scripts. */
public final class PolyglotScriptLoader {
    private final ScriptTypeRouter router;
    private final Consumer<String> diagnostics;
    public PolyglotScriptLoader(ScriptTypeRouter router, Consumer<String> diagnostics) {
        this.router = router;
        this.diagnostics = diagnostics;
    }
    public int executeAll(Document document, PolyglotPageRuntime runtime) {
        NodeList scripts = document.getElementsByTagName("script");
        int count = 0;
        for (int i = 0; i < scripts.getLength(); i++) {
            Element script = (Element) scripts.item(i);
            String outputId = script.getAttribute("data-source-output");
            if (outputId != null && !outputId.isBlank()) {
                try {
                    Path source = localSource(document.getDocumentURI(), script.getAttribute("src"));
                    document.getElementById(outputId).setTextContent(Files.readString(source));
                } catch (Exception e) { throw new IllegalStateException("Cannot display script source", e); }
            }
            if ("application/x-espresso".equals(script.getAttribute("type"))) {
                try {
                    runtime.loadBytecode(localSource(document.getDocumentURI(), script.getAttribute("src")),
                            script.getAttribute("entry"), script.getAttribute("export"));
                    script.setAttribute("data-cooljava-status", "executed");
                    count++;
                    continue;
                } catch (Exception e) { throw new IllegalStateException("Cannot load Espresso bytecode", e); }
            }
            var language = router.languageFor(script.getAttribute("type"));
            if (language.isEmpty()) continue;
            String name = "InlineScript" + (i + 1) + "." + language.get();
            try {
                String src = script.getAttribute("src");
                if (src != null && !src.isBlank()) {
                    Path path = localSource(document.getDocumentURI(), src);
                    name = path.getFileName().toString();
                    if (language.get().equals("java")) {
                        String entry = script.getAttribute("data-entry");
                        runtime.executeJava(path, entry == null || entry.isBlank()
                                ? name.substring(0, name.lastIndexOf('.')) : entry);
                    } else runtime.execute(language.get(), Files.readString(path), name);
                } else runtime.execute(language.get(), script.getTextContent(), name);
                script.setAttribute("data-cooljava-status", "executed");
                count++;
            } catch (Exception error) {
                script.setAttribute("data-cooljava-status", "failed");
                diagnostics.accept(name + ": " + error.getMessage());
                throw new IllegalStateException(name + ": " + error.getMessage(), error);
            }
        }
        return count;
    }
    static Path localSource(String base, String src) throws Exception {
        URI page = URI.create(base);
        if (!"file".equals(page.getScheme())) throw new IllegalArgumentException("Local file pages only");
        Path root = Path.of(page).toRealPath().getParent();
        URI resolved = page.resolve(src);
        if (!"file".equals(resolved.getScheme())) throw new IllegalArgumentException("Remote guest scripts are disabled");
        Path source = Path.of(resolved).toRealPath();
        if (!source.startsWith(root)) throw new IllegalArgumentException("Script must be inside the page directory");
        return source;
    }
}
