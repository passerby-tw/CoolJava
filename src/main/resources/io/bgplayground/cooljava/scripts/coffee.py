python_phase = False

def move_coffee(event):
    global python_phase
    python_phase = not python_phase
    document.getElementById("coffee").setAttribute(
        "class", "bounce-a" if python_phase else "bounce-b")
    document.getElementById("status").setTextContent("GraalPy → W3C DOM")

document.getElementById("run-python").addEventListener("click", move_coffee, False)
