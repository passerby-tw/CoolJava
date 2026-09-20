package io.bgplayground.cooljava.guest;

import java.lang.reflect.*;

/** Loaded and executed inside Espresso, never invoked by the host JVM. */
public final class SourceEntrypoint {
    public static void run(String className) throws Exception {
        Class<?> type = Class.forName(className);
        Method main;
        Object[] args;
        try { main = type.getDeclaredMethod("main", String[].class); args = new Object[]{new String[0]}; }
        catch (NoSuchMethodException e) { main = type.getDeclaredMethod("main"); args = new Object[0]; }
        if (Modifier.isPrivate(main.getModifiers()) || main.getReturnType() != void.class)
            throw new IllegalArgumentException("Entry must have a non-private void main() or main(String[])");
        main.setAccessible(true);
        Object instance = null;
        if (!Modifier.isStatic(main.getModifiers())) {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        }
        try { main.invoke(instance, args); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) throw cause;
            if (e.getCause() instanceof Error cause) throw cause;
            throw e;
        }
    }
}
