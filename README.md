# Cool!Java

<p align="left">
  <img src="src/main/resources/io/bgplayground/cooljava/images/cooljava-icon.png"
       alt="Cool!Java"
       width="240">
</p>

Cool!Java is a demo browser for [Polyglot Scripting](https://bgplayground.blogspot.com/2026/09/polyglot-scripting.html).

## What is it?

Polyglot Scripting lets scripts written in different languages run within one host, use host objects and events, and call one another. Like Active Scripting, it separates the host from its scripting engines. Its scope extends beyond web pages.

Cool!Java uses a browser as the demonstration host. JavaFX WebView provides the web interface, and Java exposes W3C DOM objects and bridges events. JavaScript, Python, and Java can manipulate the same page or combine their libraries to complete a task.

### Truffle Language Implementation Framework

The [Truffle Language Implementation Framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/) supports language implementation and interoperability, with JIT optimization through the Graal compiler. Cool!Java embeds three Truffle language implementations through the GraalVM Polyglot API:

| Language | Engine | Uses in Cool!Java |
| --- | --- | --- |
| JavaScript | GraalJS | DOM scripting, cross-language calls, MathJax typesetting |
| Python | GraalPy | DOM scripting, SymPy symbolic mathematics and numerical sampling |
| Java bytecode | Espresso | Java scripting, pure Java image processing |

Java source is compiled by the host JDK's JavaCompiler, then executed as bytecode in Espresso. This is Cool!Java's scripting workflow, rather than a complete implementation of the JEP 458 source launcher.

Ordinary page JavaScript, including Chart.js, still runs in WebKit's JavaScriptCore. GraalJS and JavaScriptCore are separate engines, with data exchanged through host bridges. GraalJS, GraalPy, and Espresso communicate through Truffle interop.

## How to build

Requirements:

- GraalVM JDK 25. The verified environment uses GraalVM CE 25.2.4.
- Maven. The verified environment uses Maven 3.8.7.
- A desktop environment that can display JavaFX windows. The application has been verified on Linux.

GraalVM is itself a JDK. Check that Maven uses the intended JDK:

```sh
java -version
mvn -version
```

Build and launch from the project directory:

```sh
cd CoolJava
mvn clean compile javafx:run
```

After building, launch again without source changes:

```sh
mvn javafx:run
```

Run the tests:

```sh
mvn clean test
```

The first build requires network access to download Maven dependencies. The project includes the SymPy, mpmath, MathJax, and Chart.js assets used by the demos. Normal builds and runs do not require separate npm or pip commands. Maven manages JavaFX and Polyglot dependencies; their versions are specified in `pom.xml`.

The application currently launches through Maven. A runtime-inclusive installer or executable fat JAR is not provided. See [THIRD-PARTY.md](THIRD-PARTY.md) for third-party components and license information.

## Demos

Use the navigation tabs at the top of each page to switch between the three demos.

### 1. Coffee DOM: three languages, one cup of coffee

JavaScript, Python, and Java register event listeners and manipulate the same coffee element through the Java W3C DOM bridge. Click each language's button to trigger a different CSS animation.

This demo illustrates host-object interop, event callbacks, and Java source used as a script. Each language's script contains its own DOM animation operations.

Page: `src/main/resources/io/bgplayground/cooljava/polyglot-dom.html`

### 2. Polyglot Mathematics: combining language ecosystems

Enter an expression such as `sin(x) * exp(x)` and click Calculate:

1. GraalPy runs SymPy to calculate the first and second derivatives and produce LaTeX and curve samples.
2. GraalJS runs MathJax to typeset the LaTeX as SVG.
3. WebKit's JavaScriptCore runs Chart.js to plot the function and its first derivative.

This demo combines Python's mathematics ecosystem, JavaScript typesetting tools, and a web frontend.

Page: `src/main/resources/io/bgplayground/cooljava/mathematics.html`

### 3. Espresso Image Processing: bytecode as a script

Load an image and select grayscale or edge detection. Espresso executes the Java image-processing code:

```html
<script type="application/x-espresso"
        src="guest/ImageProcessor.class"
        entry="io.bgplayground.cooljava.guest.ImageProcessor"
        export="imageProcessor"></script>
```

The web frontend decodes the image and displays it on Canvas. Espresso performs the pixel-array calculations using pure Java bytecode, without AWT, ImageIO, or an Applet lifecycle.

Page: `src/main/resources/io/bgplayground/cooljava/image-processing.html`

### Runtime behavior and scope

Each page has its own Polyglot Context. Guest initialization and computation run sequentially on a background worker for that page. DOM operations are dispatched to the JavaFX thread. Navigating or reloading creates a new Context, so libraries are initialized again.

This PoC runs trusted local scripts; it is not a security sandbox for untrusted code. It does not enforce computation timeouts. Background execution helps keep the interface responsive but does not guarantee termination of an infinite loop.
