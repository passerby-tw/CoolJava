let jsPhase = false;
document.getElementById('run-js').addEventListener('click', function(event) {
    jsPhase = !jsPhase;
    document.getElementById('coffee').setAttribute('class', jsPhase ? 'wave-a' : 'wave-b');
    document.getElementById('status').setTextContent('GraalJS → W3C DOM');
}, false);
