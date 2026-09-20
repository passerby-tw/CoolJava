package io.bgplayground.cooljava;

import java.util.Locale;
import java.util.Optional;

/** Maps inert HTML script types to Truffle language identifiers. */
public final class ScriptTypeRouter {
    public static final String GRAAL_JS = "application/x-graaljs";
    public static final String GRAAL_PYTHON = "application/x-graalpy";

    public Optional<String> languageFor(String scriptType) {
        if (scriptType == null) {
            return Optional.empty();
        }

        return switch (scriptType.trim().toLowerCase(Locale.ROOT)) {
            case GRAAL_JS -> Optional.of("js");
            case GRAAL_PYTHON -> Optional.of("python");
            case "application/x-java", "text/x-java" -> Optional.of("java");
            default -> Optional.empty();
        };
    }
}
