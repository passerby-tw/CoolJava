package io.bgplayground.cooljava;

/** A deliberately small bridge between polyglot scripts and the web view. */
public interface BrowserApi {
    void setText(String selector, String text);

    void animate(String selector, String animationClass);

    String getAttribute(String selector, String name);

    void setAttribute(String selector, String name, String value);

    void log(String message);
}
