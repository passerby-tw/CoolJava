"""Portable algorithm test. CPython passing does NOT prove GraalPy compatibility."""
import importlib.util
import json
from pathlib import Path
import sys
import types
import unittest

polyglot = types.ModuleType("polyglot")
polyglot.export_value = lambda f: f
sys.modules["polyglot"] = polyglot
path = Path(__file__).resolve().parents[1] / "src/main/resources/io/bgplayground/cooljava/scripts/mathematics.py"
spec = importlib.util.spec_from_file_location("mathematics", path)
math = importlib.util.module_from_spec(spec)
spec.loader.exec_module(math)

class MathTest(unittest.TestCase):
    def test_derivative_and_points(self):
        result = json.loads(math.calculate_math("x**2"))
        self.assertEqual(result["latex"], ["x^{2}", "2 x", "2"])
        self.assertEqual(len(result["curves"][0]), 101)
        self.assertEqual(result["curves"][0][50], {"x": 0.0, "y": 0.0})

    def test_domain_gaps(self):
        result = json.loads(math.calculate_math("log(x)"))
        self.assertIsNone(result["curves"][0][0]["y"])

    def test_reject_non_dsl(self):
        for source in ["__import__('os')", "x.__class__", "[x for x in []]", "x**100", "True", "1e999"]:
            with self.subTest(source=source), self.assertRaises((ValueError, SyntaxError)):
                math.parse_expression(source)

if __name__ == "__main__": unittest.main()
