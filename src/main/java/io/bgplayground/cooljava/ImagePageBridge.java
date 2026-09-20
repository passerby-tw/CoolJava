package io.bgplayground.cooljava;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import org.graalvm.polyglot.HostAccess;

public final class ImagePageBridge {
    private final PageThread thread;
    private final PolyglotPageRuntime runtime;
    private final WebEngine engine;
    public ImagePageBridge(PageThread thread, PolyglotPageRuntime runtime, WebEngine engine) {
        this.thread=thread; this.runtime=runtime; this.engine=engine;
    }
    // JavaScriptCore Java bridge requires a public method, not HostAccess permission.
    @HostAccess.Export public void apply(String pixels, int width, int height, String effect) {
        thread.submit(() -> {
            try {
                String output=runtime.plugin("imageProcessor").invokeMember("apply", pixels, width, height, effect).asString();
                thread.fx(() -> ((JSObject)engine.executeScript("window")).call("imageDone", output, width, height));
            } catch (Throwable e) {
                e.printStackTrace();
                thread.fx(() -> ((JSObject)engine.executeScript("window")).call("imageFailed", e.toString()));
            }
        });
    }
}
