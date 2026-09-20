"""No eval/sympify of user input: a small explicitly permitted expression DSL."""
import ast
import json
import math
import sympy as sp
import polyglot

x = sp.Symbol("x", real=True)
functions = {"sin": sp.sin, "cos": sp.cos, "exp": sp.exp, "log": sp.log, "sqrt": sp.sqrt}

def parse_expression(source):
    if len(source) > 160:
        raise ValueError("Expression is limited to 160 characters")
    tree = ast.parse(source, mode="eval")
    if len(list(ast.walk(tree))) > 64:
        raise ValueError("Expression is too complex")

    def convert(node):
        if isinstance(node, ast.Name) and node.id in ("x", "pi", "E"):
            return {"x": x, "pi": sp.pi, "E": sp.E}[node.id]
        if isinstance(node, ast.Constant) and type(node.value) in (int, float):
            if not math.isfinite(node.value) or abs(node.value) > 10000:
                raise ValueError("Number out of range")
            return sp.Rational(str(node.value))
        if isinstance(node, ast.UnaryOp) and isinstance(node.op, (ast.UAdd, ast.USub)):
            value = convert(node.operand)
            return value if isinstance(node.op, ast.UAdd) else -value
        if isinstance(node, ast.BinOp):
            left, right = convert(node.left), convert(node.right)
            if isinstance(node.op, ast.Add): return left + right
            if isinstance(node.op, ast.Sub): return left - right
            if isinstance(node.op, ast.Mult): return left * right
            if isinstance(node.op, ast.Div): return left / right
            if isinstance(node.op, ast.Pow) and right.is_Integer and abs(right) <= 8:
                return left ** right
        if (isinstance(node, ast.Call) and isinstance(node.func, ast.Name)
                and node.func.id in functions and len(node.args) == 1 and not node.keywords):
            return functions[node.func.id](convert(node.args[0]))
        raise ValueError("Use x, pi, E, + - * / ** and sin/cos/exp/log/sqrt; integer powers -8…8")
    return convert(tree.body)

@polyglot.export_value
def calculate_math(source):
    expression = parse_expression(str(source))
    first = sp.diff(expression, x)
    second = sp.diff(first, x)
    def sample(expr):
        points = []
        for i in range(101):
            px = -5 + i / 10
            try:
                value = float(expr.subs(x, px).evalf())
                if not math.isfinite(value) or abs(value) > 1e8: value = None
            except (ValueError, TypeError, OverflowError, ZeroDivisionError):
                value = None
            points.append({"x": px, "y": value})
        return points
    return json.dumps({"latex": [sp.latex(e) for e in (expression, first, second)],
                       "curves": [sample(expression), sample(first)]}, allow_nan=False)
