package io.bgplayground.cooljava;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.graalvm.polyglot.HostAccess;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/** Fixed rendering endpoints. Never evaluates user-provided JavaScript. */
public final class MathPageBridge {
    private final WebEngine engine;
    private final PageThread thread;
    public MathPageBridge(WebEngine engine, PageThread thread) { this.engine = engine; this.thread = thread; }
    @HostAccess.Export public void formula(String id, String svg) {
        if (!Set.of("formula", "derivative", "second").contains(id))
            throw new IllegalArgumentException("Unknown formula output");
        thread.fx(() -> ((JSObject) engine.executeScript("window")).call("renderFormulaSvg", id, svg));
    }
    @HostAccess.Export public void chart(String json) {
        thread.fx(() -> ((JSObject) engine.executeScript("window")).call("renderChartJson", json));
    }
}
