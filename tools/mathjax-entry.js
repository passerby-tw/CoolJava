import {mathjax} from 'mathjax-full/js/mathjax.js';
import {TeX} from 'mathjax-full/js/input/tex.js';
import {SVG} from 'mathjax-full/js/output/svg.js';
import {liteAdaptor} from 'mathjax-full/js/adaptors/liteAdaptor.js';
import {RegisterHTMLHandler} from 'mathjax-full/js/handlers/html.js';
import 'mathjax-full/js/input/tex/ams/AmsConfiguration.js';
const adaptor = liteAdaptor();
RegisterHTMLHandler(adaptor);
const math = mathjax.document('', {
  InputJax: new TeX({packages: ['base', 'ams']}),
  OutputJax: new SVG({fontCache: 'none'})
});
export function toSvg(latex) {
  const container = math.convert(String(latex), {display: true, em: 16, ex: 8, containerWidth: 800});
  return adaptor.outerHTML(adaptor.firstChild(container));
}
