import vm from 'node:vm';
import fs from 'node:fs';
import assert from 'node:assert/strict';
const context = vm.createContext({}); // No document, window, require or process.
vm.runInContext(fs.readFileSync('src/main/resources/io/bgplayground/cooljava/vendor/mathjax-graal.js', 'utf8'), context);
for (const tex of ['x^2', '\\frac{1}{2}', '\\sin(x)e^x']) {
    const svg = context.CoolMath.toSvg(tex);
    assert.match(svg, /^<svg /);
    assert.match(svg, /<path /);
    assert.doesNotMatch(svg, /<script/);
}
console.log('PASS: three MathJax formulas in isolated JS context (not a GraalJS execution test)');
