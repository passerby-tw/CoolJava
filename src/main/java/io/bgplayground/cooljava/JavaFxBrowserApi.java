package io.bgplayground.cooljava;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** JavaFX implementation of the intentionally narrow browser bridge. */
public final class JavaFxBrowserApi implements BrowserApi {
    private final WebEngine webEngine;
    private final Map<String, String> activeAnimations = new HashMap<>();
    private final Map<String, Boolean> animationPhases = new HashMap<>();

    public JavaFxBrowserApi(WebEngine webEngine) {
        this.webEngine = Objects.requireNonNull(webEngine);
    }

    Document document() {
        return webEngine.getDocument();
    }

    @Override
    public void setText(String selector, String text) {
        element(selector).setTextContent(text);
    }

    @Override
    public void animate(String selector, String animationClass) {
        String id = elementId(selector);
        Element element = element(id);
        String previous = activeAnimations.get(id);
        boolean phase = !animationPhases.getOrDefault(id, false);
        String next = animationClass + (phase ? "-a" : "-b");

        String classes = element.getAttribute("class");
        if (previous != null) {
            classes = removeClass(classes, previous);
        }
        element.setAttribute("class", addClass(classes, next));
        activeAnimations.put(id, next);
        animationPhases.put(id, phase);
    }

    @Override
    public String getAttribute(String selector, String name) {
        return element(selector).getAttribute(name);
    }

    @Override
    public void setAttribute(String selector, String name, String value) {
        element(selector).setAttribute(name, value);
    }

    @Override
    public void log(String message) {
        System.out.println("[Cool!Java] " + message);
    }

    private Element element(String selector) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("DOM access must run on the JavaFX application thread");
        }
        Document document = webEngine.getDocument();
        if (document == null) {
            throw new IllegalStateException("The WebKit DOM is not available");
        }
        String id = elementId(selector);
        Element element = document.getElementById(id);
        if (element == null) {
            throw new IllegalArgumentException("DOM element not found: " + id);
        }
        return element;
    }

    static String elementId(String selector) {
        Objects.requireNonNull(selector, "selector");
        String id = selector.startsWith("#") ? selector.substring(1) : selector;
        if (id.isBlank() || id.indexOf(' ') >= 0 || id.indexOf('>') >= 0
                || id.indexOf('.') >= 0 || id.indexOf('[') >= 0) {
            throw new IllegalArgumentException("BrowserAPI accepts an element id, not a CSS selector: " + selector);
        }
        return id;
    }

    static String removeClass(String classes, String target) {
        String safeClasses = classes == null ? "" : classes;
        return String.join(" ", java.util.Arrays.stream(safeClasses.trim().split("\\s+"))
                .filter(value -> !value.isBlank() && !value.equals(target))
                .toList());
    }

    static String addClass(String classes, String value) {
        String trimmed = classes == null ? "" : classes.trim();
        return trimmed.isEmpty() ? value : trimmed + " " + value;
    }

}
