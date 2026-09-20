package io.bgplayground.cooljava;

import javafx.application.Platform;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.lang.reflect.*;
import java.util.*;
import org.w3c.dom.events.EventListener;

/** One serial guest worker. FX never waits for guest computation. */
public final class PageThread {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cooljava-page"); t.setDaemon(true); return t;
    });
    private volatile boolean active = true;
    private final Map<EventListener, EventListener> listeners = new IdentityHashMap<>();
    private final List<Runnable> removals = new ArrayList<>();
    private final Map<Object, Object> originals = new IdentityHashMap<>();
    public void submit(Runnable action) {
        if (!active) return;
        worker.execute(() -> { if (active) try { action.run(); } catch (Throwable e) { e.printStackTrace(); } });
    }
    public <T> T fx(Supplier<T> action) {
        if (!active) throw new CancellationException("Page closed");
        if (Platform.isFxApplicationThread()) return action.get();
        FutureTask<T> task = new FutureTask<>(() -> {
            if (!active) throw new CancellationException("Page closed");
            return action.get();
        });
        Platform.runLater(task);
        try { return task.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException(); }
        catch (ExecutionException e) { throw new IllegalStateException(e.getCause()); }
    }
    public void close(Runnable cleanup) {
        active = false;
        // Called on FX; detach listeners without entering a guest Context.
        for (Runnable remove : removals) remove.run();
        removals.clear();
        worker.execute(cleanup);
        worker.shutdown();
    }
    private static void interfaces(Class<?> type, Set<Class<?>> result) {
        if (type == null) return;
        for (Class<?> i : type.getInterfaces()) {
            if (i.getName().startsWith("org.w3c.dom.")) result.add(i);
            interfaces(i, result);
        }
        interfaces(type.getSuperclass(), result);
    }
    /** Preserve W3C interfaces while dispatching each native DOM call to FX. */
    public Object wrap(Object target) {
        if (target == null) return null;
        Set<Class<?>> types = new LinkedHashSet<>(); interfaces(target.getClass(), types);
        if (types.isEmpty()) return target;
        Object wrapped = Proxy.newProxyInstance(PageThread.class.getClassLoader(), types.toArray(Class<?>[]::new),
            (proxy, method, args) -> {
                if (method.getName().equals("preventDefault") || method.getName().equals("stopPropagation"))
                    throw new UnsupportedOperationException("Cancel browser defaults in a JavaScriptCore listener before async dispatch");
                return fx(() -> {
                    Object[] actual = args == null ? new Object[0] : args.clone();
                    for (int i=0;i<actual.length;i++) {
                        synchronized (originals) { actual[i] = originals.getOrDefault(actual[i], actual[i]); }
                    }
                    if ((method.getName().equals("addEventListener") || method.getName().equals("removeEventListener"))
                            && actual[1] instanceof EventListener callback) {
                        EventListener adapter = listeners.computeIfAbsent(callback,
                            key -> event -> submit(() -> key.handleEvent((org.w3c.dom.events.Event) wrap(event))));
                        actual[1] = adapter;
                        if (method.getName().equals("addEventListener")) {
                            var eventTarget = (org.w3c.dom.events.EventTarget) target;
                            String name = (String)actual[0]; boolean capture = (Boolean)actual[2];
                            removals.add(() -> eventTarget.removeEventListener(name, adapter, capture));
                        }
                    }
                    try { return wrap(method.invoke(target, actual)); }
                    catch (ReflectiveOperationException e) { throw new IllegalStateException(e.getCause() == null ? e : e.getCause()); }
                });
            });
        synchronized (originals) { originals.put(wrapped, target); }
        return wrapped;
    }
}
