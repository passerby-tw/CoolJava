# Third-party components

This source tree includes the following demo dependencies. Preserve their accompanying
license files when redistributing these components.

| Component | Version | Included license |
| --- | --- | --- |
| SymPy | 1.14.0 | `deps/python/sympy-1.14.0.dist-info/licenses/LICENSE` |
| mpmath | 1.3.0 | `deps/python/mpmath-1.3.0.dist-info/LICENSE` |
| MathJax | 3.2.2 | `src/main/resources/io/bgplayground/cooljava/vendor/MathJax-LICENSE` |
| Chart.js | 4.4.8 | `src/main/resources/io/bgplayground/cooljava/vendor/Chart-LICENSE.md` |

SymPy modification: `sympy/external/gmpy.py` imports `struct.calcsize` instead of
`ctypes.c_long` and `ctypes.sizeof`, and computes LONG_MAX using `calcsize('@l')`.
This avoids the unconditional ctypes extension import in the demonstrated GraalPy setup.
The installed package metadata identifies the upstream version, not a separate upstream release of this patch.

MathJax is bundled through `tools/mathjax-entry.js` and `tools/build-assets.mjs`.
Chart.js is copied into the demo vendor directory by the same asset build process.
Build-tool and Java runtime dependency versions are recorded in `package-lock.json` and `pom.xml`.
Their original distribution terms continue to apply. This file does not assign a license to Cool!Java itself.
