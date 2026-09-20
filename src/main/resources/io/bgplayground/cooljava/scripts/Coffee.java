import com.oracle.truffle.espresso.polyglot.Interop;
import com.oracle.truffle.espresso.polyglot.Polyglot;

// Compact source: compiled on load, executed only inside Espresso.
void main() throws Exception {
    Object events = Polyglot.importObject("events");
    Interop.invokeMember(events, "listen", "run-java", new Listener());
}

public class Listener {
    private boolean phase;
    public void handleEvent(Object event) throws Exception {
        Object document = Polyglot.importObject("document");
        Object coffee = Interop.invokeMember(document, "getElementById", "coffee");
        phase = !phase;
        Interop.invokeMember(coffee, "setAttribute", "class", phase ? "spin-a" : "spin-b");
        Object status = Interop.invokeMember(document, "getElementById", "status");
        Interop.invokeMember(status, "setTextContent", "Java source → Espresso → W3C DOM");
    }
}
