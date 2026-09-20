const calculateMath = Polyglot.import('calculate_math');
let calculating = false;
function calculate(event) {
    if (calculating) return;
    calculating = true;
    const status = document.getElementById('status');
    const button = document.getElementById('calculate');
    button.setAttribute('disabled', 'disabled');
    status.setTextContent('Calculating in GraalPy / GraalJS…');
    try {
        const source = document.getElementById('expression').getValue();
        const result = JSON.parse(String(calculateMath(source)));
        ['formula', 'derivative', 'second'].forEach((id, index) => {
            page.formula(id, CoolMath.toSvg(result.latex[index]));
        });
        page.chart(JSON.stringify(result.curves));
        status.setTextContent('SymPy / GraalPy ✓   MathJax / GraalJS ✓   Chart.js / JavaScriptCore ✓');
    } catch (error) {
        status.setTextContent(String(error));
    } finally {
        calculating = false;
        button.removeAttribute('disabled');
    }
}
document.getElementById('calculate').addEventListener('click', calculate, false);
document.getElementById('status').setTextContent('Ready — press Calculate');
