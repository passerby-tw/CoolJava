package io.bgplayground.cooljava;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.*;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.w3c.dom.Document;

public final class CoolJavaApp extends Application {
    private Session session;
    private static final class Session {
        final PageThread thread = new PageThread();
        PolyglotPageRuntime runtime;
        ImagePageBridge image; // Strong reference: WebKit Java bindings are weak.
        void close() { thread.close(() -> { if (runtime != null) runtime.close(); }); }
    }
    @Override public void start(Stage stage) {
        WebView view = new WebView(); WebEngine engine = view.getEngine();
        TextField location = new TextField(); location.setEditable(false);
        location.textProperty().bind(engine.locationProperty()); HBox.setHgrow(location, Priority.ALWAYS);
        Label status = new Label("Loading…"); Button reload = new Button("Reload");
        reload.setOnAction(e -> engine.reload());
        HBox toolbar = new HBox(8, new Label("Cool!Java"), location, reload, status);
        toolbar.setPadding(new Insets(10));
        engine.getLoadWorker().stateProperty().addListener((o, old, state) -> {
            if (state == Worker.State.SCHEDULED) {
                if (session != null) { session.close(); session=null; }
                status.setText("Loading…");
            }
            if (state != Worker.State.SUCCEEDED) return;
            if (!engine.getLocation().startsWith("file:")) { status.setText("Local files only"); return; }
            Session page = new Session(); session=page;
            Document document = (Document)page.thread.wrap(engine.getDocument());
            BrowserApi raw = new JavaFxBrowserApi(engine);
            BrowserApi api = (BrowserApi)java.lang.reflect.Proxy.newProxyInstance(
                BrowserApi.class.getClassLoader(), new Class<?>[]{BrowserApi.class}, (p,m,a) -> page.thread.fx(() -> {
                    try { return m.invoke(raw,a); } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                }));
            status.setText("Initializing languages…");
            page.thread.submit(() -> {
                try {
                    page.runtime=new PolyglotPageRuntime(api, document);
                    if (document.getElementById("calculate") != null)
                        page.runtime.bind("js", "page", new MathPageBridge(engine, page.thread));
                    int count=new PolyglotScriptLoader(new ScriptTypeRouter(), System.err::println).executeAll(document, page.runtime);
                    if (document.getElementById("image-output") != null) {
                        page.image=new ImagePageBridge(page.thread,page.runtime,engine);
                        page.thread.fx(() -> {
                            ((JSObject)engine.executeScript("window")).setMember("imageHost",page.image);
                            engine.executeScript("imageReady()"); return null;
                        });
                    }
                    page.thread.fx(() -> { status.setText(count+" scripts loaded"); return null; });
                } catch (Throwable e) {
                    e.printStackTrace();
                    page.thread.fx(() -> {
                        status.setText("Failed — see terminal");
                        var output=engine.getDocument().getElementById("status");
                        if(output!=null) output.setTextContent(e.toString()); return null;
                    });
                }
            });
        });
        BorderPane root=new BorderPane(view); root.setTop(toolbar);
        stage.setTitle("Cool!Java — Polyglot Scripting");
        stage.setScene(new Scene(root,1180,850)); stage.show();
        engine.load(CoolJavaApp.class.getResource("/io/bgplayground/cooljava/polyglot-dom.html").toExternalForm());
    }
    @Override public void stop() { if(session!=null) session.close(); }
    public static void main(String[] args) { launch(args); }
}
