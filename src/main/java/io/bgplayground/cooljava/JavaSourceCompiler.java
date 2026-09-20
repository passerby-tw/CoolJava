package io.bgplayground.cooljava;

import javax.tools.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Compiles source without executing it on the host. Output belongs to one page. */
public final class JavaSourceCompiler implements AutoCloseable {
    private final Path output;
    public JavaSourceCompiler() {
        try { output = Files.createTempDirectory("cooljava-page-"); }
        catch (IOException e) { throw new IllegalStateException(e); }
    }
    public Path output() { return output; }

    public void compile(Path source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java source requires a full JDK, not a JRE");
        DiagnosticCollector<JavaFileObject> messages = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(messages, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<String> options = List.of("-proc:none", "-encoding", "UTF-8", "-d", output.toString(),
                    "-classpath", System.getProperty("java.class.path"),
                    "-sourcepath", source.toAbsolutePath().getParent().toString());
            boolean ok = compiler.getTask(null, files, messages, options, null,
                    files.getJavaFileObjects(source.toFile())).call();
            if (!ok) {
                StringBuilder detail = new StringBuilder("Java compilation failed:\n");
                for (Diagnostic<?> d : messages.getDiagnostics()) detail.append(source.getFileName())
                        .append(':').append(d.getLineNumber()).append(':').append(d.getColumnNumber())
                        .append(' ').append(d.getMessage(Locale.ROOT)).append('\n');
                throw new IllegalArgumentException(detail.toString());
            }
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    public Path inline(String filename, String source) {
        if (!filename.matches("[A-Za-z_$][A-Za-z0-9_$]*\\.java"))
            throw new IllegalArgumentException("Inline Java needs a simple .java filename");
        Path path = output.resolve(filename);
        try { Files.writeString(path, source, StandardCharsets.UTF_8); return path; }
        catch (IOException e) { throw new IllegalStateException(e); }
    }
    @Override public void close() {
        try (var paths = Files.walk(output)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException e) { System.err.println("Cannot remove page compilation cache: " + e.getMessage()); }
    }
}
