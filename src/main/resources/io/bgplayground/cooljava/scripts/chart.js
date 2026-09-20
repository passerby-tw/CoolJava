// Ordinary WebKit JavaScriptCore, not GraalJS.
let chart;
function renderChartJson(json) {
    const curves = JSON.parse(json);
    const colors = ['#20b7c8', '#b58aff'];
    const datasets = curves.map((points, index) => ({
        label: index ? "f′(x)" : 'f(x)', data: points,
        borderColor: colors[index], pointRadius: 0, borderWidth: 2,
        showLine: true, spanGaps: false
    }));
    if (chart) { chart.data.datasets = datasets; chart.update('none'); return; }
    chart = new Chart(document.getElementById('plot'), {
        type: 'scatter', data: {datasets}, options: {
            animation: false, responsive: true, maintainAspectRatio: false,
            scales: {x: {type: 'linear', min: -5, max: 5}}
        }
    });
}

function renderFormulaSvg(id, source) {
    const parsed = new DOMParser().parseFromString(source, 'image/svg+xml');
    if (parsed.getElementsByTagName('parsererror').length || parsed.documentElement.localName !== 'svg')
        throw new Error('Invalid formula SVG: ' + id);
    const previous = document.getElementById(id);
    if (!previous) throw new Error('Missing formula: ' + id);
    const svg = document.importNode(parsed.documentElement, true);
    svg.setAttribute('id', id);
    const box = svg.getAttribute('viewBox').trim().split(/[\s,]+/).map(Number);
    if (box.length !== 4 || !box.every(Number.isFinite) || box[2] <= 0 || box[3] <= 0)
        throw new Error('Invalid formula viewBox');
    const height = 32, width = height * box[2] / box[3];
    svg.setAttribute('width', String(width)); svg.setAttribute('height', String(height));
    svg.style.cssText = `width:${width}px;height:${height}px;max-width:none;display:block;color:#111;vertical-align:0`;
    previous.parentNode.replaceChild(svg, previous);
    return true;
}
