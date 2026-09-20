package io.bgplayground.cooljava;

/** Plain launcher avoids the Java launcher special case for Application subclasses. */
public final class Launcher {
    private Launcher() { }
    public static void main(String[] args) {
        javafx.application.Application.launch(CoolJavaApp.class, args);
    }
}
