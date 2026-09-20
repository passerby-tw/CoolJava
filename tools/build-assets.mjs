import {build} from 'esbuild';
import {mkdir, copyFile} from 'node:fs/promises';
const out = 'src/main/resources/io/bgplayground/cooljava/vendor';
await mkdir(out, {recursive: true});
await build({entryPoints: ['tools/mathjax-entry.js'], bundle: true, platform: 'browser',
  format: 'iife', globalName: 'CoolMath', outfile: `${out}/mathjax-graal.js`});
await copyFile('node_modules/chart.js/dist/chart.umd.js', `${out}/chart.umd.js`);
await copyFile('node_modules/chart.js/LICENSE.md', `${out}/Chart-LICENSE.md`);
await copyFile('node_modules/mathjax-full/LICENSE', `${out}/MathJax-LICENSE`);
