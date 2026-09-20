package io.bgplayground.cooljava;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.lang.reflect.Method;

/** Owns the polyglot context for one loaded page. */
public final class PolyglotPageRuntime implements AutoCloseable {
    private static final String[] LANGUAGES = {"js", "python", "java"};

    private final Context context;
    private final JavaSourceCompiler compiler = new JavaSourceCompiler();
    private final java.util.List<Runnable> listenerCleanup = new java.util.ArrayList<>();
    private final java.util.Set<String> initialized = new java.util.HashSet<>();
    private final BrowserApi browser;
    private final Document document;

    public PolyglotPageRuntime(BrowserApi browser) {
        this(browser, null);
    }

    public PolyglotPageRuntime(BrowserApi browser, Document document) {
        this.browser = browser;
        this.document = document;
        HostAccess.Builder hostAccessBuilder = HostAccess.newBuilder(HostAccess.NONE)
                .allowAccessAnnotatedBy(HostAccess.Export.class)
                .allowAccessInheritance(true)
                .allowImplementations(EventListener.class);
        allowInterface(hostAccessBuilder, Node.class);
        allowInterface(hostAccessBuilder, Element.class);
        allowInterface(hostAccessBuilder, Document.class);
        allowInterface(hostAccessBuilder, EventTarget.class);
        allowInterface(hostAccessBuilder, Event.class);
        allowInterface(hostAccessBuilder, org.w3c.dom.html.HTMLTextAreaElement.class);
        allowInterface(hostAccessBuilder, org.w3c.dom.NodeList.class);
        HostAccess hostAccess = hostAccessBuilder.build();

        try {
        this.context = Context.newBuilder(LANGUAGES)
                .allowExperimentalOptions(true)
                .option("java.Polyglot", "true")
                .option("java.Classpath", compiler.output() + java.io.File.pathSeparator + System.getProperty("java.class.path"))
                .option("python.PythonPath", System.getProperty("cooljava.pythonPath", "deps/python"))
                // Espresso must read the guest JDK runtime image while booting.
                // This PoC runs trusted local scripts. A production sandbox
                // should replace ALL with a deliberately restricted FileSystem.
                .allowIO(IOAccess.ALL)
                // Espresso is a guest JVM and starts its Reference Handler,
                // Finalizer and Common Cleaner threads during initialization.
                .allowCreateThread(true)
                // No AWT/ImageIO in the two demonstrations.
                .allowNativeAccess(false)
                .allowHostAccess(hostAccess)
                .allowHostClassLookup(className -> false)
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .build();
        } catch (RuntimeException | Error e) {
            compiler.close();
            throw e;
        }

        if (document != null) context.getPolyglotBindings().putMember("document", document);
        context.getPolyglotBindings().putMember("events", new Events());
    }

    private static void allowInterface(HostAccess.Builder builder, Class<?> type) {
        for (Method method : type.getMethods()) {
            builder.allowAccess(method);
        }
    }

    public Value execute(String language, String sourceCode, String sourceName) {
        if ("java".equals(language)) {
            java.nio.file.Path path = compiler.inline(sourceName, sourceCode);
            executeJava(path, sourceName.substring(0, sourceName.length() - 5));
            return context.asValue(null);
        }
        initialize(language);
        Source source = Source.newBuilder(language, sourceCode, sourceName).buildLiteral();
        return context.eval(source);
    }

    private void initialize(String language) {
        if (initialized.add(language)) {
            context.getBindings(language).putMember("browser", new ExportedBrowserApi(browser));
            if (document != null) context.getBindings(language).putMember("document", document);
        }
    }

    public void bind(String language, String name, Object value) {
        initialize(language);
        context.getBindings(language).putMember(name, value);
    }

    public void executeJava(java.nio.file.Path source, String entry) {
        compiler.compile(source);
        context.getBindings("java").getMember("io.bgplayground.cooljava.guest.SourceEntrypoint")
                .invokeMember("run", entry);
    }

    /** Callback adapter only: guest Java contains all DOM manipulation logic. */
    public final class Events {
        @HostAccess.Export public void listen(String id, Value callback) {
            if (document == null || !(document.getElementById(id) instanceof EventTarget target))
                throw new IllegalArgumentException("No event target: " + id);
            EventListener listener = event -> {
                try { callback.invokeMember("handleEvent", event); }
                catch (RuntimeException e) {
                    e.printStackTrace();
                    browser.setText("status", e.getMessage());
                }
            };
            target.addEventListener("click", listener, false);
            listenerCleanup.add(() -> target.removeEventListener("click", listener, false));
        }
    }

    @Override
    public void close() {
        try {
            for (Runnable remove : listenerCleanup) {
                try { remove.run(); } catch (java.util.concurrent.CancellationException ignored) { /* PageThread already detached listeners. */ }
            }
            listenerCleanup.clear();
        } finally {
            try { context.close(true); } finally { compiler.close(); }
        }
    }

    /** Keeps the exported surface separate from the internal Java interface. */
    public static final class ExportedBrowserApi {
        private final BrowserApi delegate;

        private ExportedBrowserApi(BrowserApi delegate) {
            this.delegate = delegate;
        }

        @HostAccess.Export
        public void setText(String selector, String text) {
            delegate.setText(selector, text);
        }

        @HostAccess.Export
        public void animate(String selector, String animationClass) {
            delegate.animate(selector, animationClass);
        }

        @HostAccess.Export
        public String getAttribute(String selector, String name) {
            return delegate.getAttribute(selector, name);
        }

        @HostAccess.Export
        public void setAttribute(String selector, String name, String value) {
            delegate.setAttribute(selector, name, value);
        }

        @HostAccess.Export
        public void log(String message) {
            delegate.log(message);
        }
    }


    public Value newJavaObject(String className) {
        Value type = context.getBindings("java").getMember(className);
        if (type == null || !type.canInstantiate()) {
            throw new IllegalArgumentException("Espresso class cannot be instantiated: " + className);
        }
        return type.newInstance();
    }

    public void export(String name, Object value) {
        context.getPolyglotBindings().putMember(name, value);
    }

    private final java.util.Map<String, Value> plugins = new java.util.HashMap<>();
    public void loadBytecode(java.nio.file.Path path, String entry, String name) throws java.io.IOException {
        if (!entry.matches("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*") || name.isBlank())
            throw new IllegalArgumentException("Bytecode script needs entry and export attributes");
        byte[] bytes = java.nio.file.Files.readAllBytes(path);
        if (bytes.length < 4 || java.nio.ByteBuffer.wrap(bytes).getInt() != 0xCAFEBABE)
            throw new IllegalArgumentException("x-espresso src must be a compiled .class file");
        java.nio.file.Path dest = compiler.output().resolve(entry.replace('.', '/') + ".class");
        java.nio.file.Files.createDirectories(dest.getParent());
        java.nio.file.Files.write(dest, bytes);
        Value value = newJavaObject(entry);
        plugins.put(name, value);
        context.getPolyglotBindings().putMember(name, value);
    }
    public Value plugin(String name) {
        Value value = plugins.get(name);
        if (value == null) throw new IllegalStateException("Plugin not loaded: " + name);
        return value;
    }
}
