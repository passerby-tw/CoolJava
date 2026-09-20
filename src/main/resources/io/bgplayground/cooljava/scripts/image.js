// Runs in JavaScriptCore. Espresso receives pixel data, never a Canvas object.
const input = document.getElementById('image-input');
const output = document.getElementById('image-output');
const imageStatus = document.getElementById('status');
let ready = false, busy = false, started = 0;
function controls() {
    for (const id of ['gray', 'sobel']) document.getElementById(id).disabled = !ready || busy;
    document.getElementById('image-file').disabled = busy;
    document.getElementById('reset').disabled = busy;
}
function imageReady() { ready = true; controls(); imageStatus.textContent = 'Espresso bytecode ready'; }
function imageFailed(message) { busy = false; controls(); imageStatus.textContent = message; }
function imageDone(csv, width, height) {
    try {
        const bytes = Uint8ClampedArray.from(String(csv).split(',').map(Number));
        if (bytes.length !== width * height * 4) throw new Error('Invalid output pixels');
        output.width = width; output.height = height;
        output.getContext('2d').putImageData(new ImageData(bytes, width, height), 0, 0);
        imageStatus.textContent = `Espresso completed · ${Math.round(performance.now() - started)} ms (including transfer)`;
    } catch (e) { imageStatus.textContent = String(e); }
    busy = false; controls();
}
function runFilter(effect) {
    if (!ready || busy) return;
    busy = true; controls(); started = performance.now();
    imageStatus.textContent = 'Computing in Espresso…';
    // Let WebKit paint the busy state before collecting the bounded pixel buffer.
    setTimeout(() => {
        try {
            const pixels = input.getContext('2d').getImageData(0, 0, input.width, input.height).data;
            imageHost.apply(Array.from(pixels).join(','), input.width, input.height, effect);
        } catch (e) { imageFailed(String(e)); }
    }, 0);
}
document.getElementById('gray').addEventListener('click', () => runFilter('gray'));
document.getElementById('sobel').addEventListener('click', () => runFilter('sobel'));
document.getElementById('reset').addEventListener('click', () => {
    output.width = input.width; output.height = input.height;
    output.getContext('2d').drawImage(input, 0, 0);
});
document.getElementById('image-file').addEventListener('change', event => {
    const file = event.target.files[0]; if (!file) return;
    const url = URL.createObjectURL(file), img = new Image();
    img.onload = () => {
        const scale = Math.min(1, 512 / img.width, 512 / img.height);
        input.width = Math.max(1, Math.round(img.width * scale));
        input.height = Math.max(1, Math.round(img.height * scale));
        input.getContext('2d').drawImage(img, 0, 0, input.width, input.height);
        document.getElementById('reset').click(); URL.revokeObjectURL(url);
    };
    img.onerror = () => { URL.revokeObjectURL(url); imageFailed('Cannot decode image'); };
    img.src = url;
});
const ctx = input.getContext('2d');
const gradient = ctx.createLinearGradient(0, 0, 320, 200);
gradient.addColorStop(0, '#ff7b45'); gradient.addColorStop(1, '#087cc9');
ctx.fillStyle = gradient; ctx.fillRect(0, 0, 320, 200);
ctx.fillStyle = '#fff'; ctx.beginPath(); ctx.arc(100, 95, 55, 0, 2*Math.PI); ctx.fill();
ctx.fillStyle = '#17354c'; ctx.fillRect(180, 45, 95, 110);
document.getElementById('reset').click();
