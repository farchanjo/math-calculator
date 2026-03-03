#!/usr/bin/env python3
"""
MCP Tool Integration Tests — math-calculator

Validates all 30 MCP tools via SSE transport:
  - Success cases with precision assertions
  - Error cases (invalid input, domain errors, edge cases)

Usage:
    python3 scripts/mcp_test.py                  # default localhost:44321
    python3 scripts/mcp_test.py --base http://host:port
"""
import argparse
import json
import queue
import sys
import threading
import time
import urllib.error
import urllib.request

# ---------------------------------------------------------------------------
# MCP client
# ---------------------------------------------------------------------------
RESP_QUEUE = queue.Queue()
MSG_URL = None
REQ_ID = 0
ENDPOINT_READY = threading.Event()


def sse_listener(base):
    """Open a single SSE connection, capture endpoint, buffer responses."""
    global MSG_URL
    req = urllib.request.Request(f"{base}/sse")
    with urllib.request.urlopen(req, timeout=60) as resp:
        for raw in resp:
            line = raw.decode("utf-8").strip()
            if not line.startswith("data:"):
                continue
            data = line[5:]
            if "/mcp/message?" in data:
                MSG_URL = f"{base}{data}"
                ENDPOINT_READY.set()
            else:
                try:
                    RESP_QUEUE.put(json.loads(data))
                except json.JSONDecodeError:
                    pass


def wait_response(rid, timeout=10):
    """Block until a JSON-RPC response with *rid* arrives."""
    deadline = time.time() + timeout
    pending = []
    while time.time() < deadline:
        try:
            msg = RESP_QUEUE.get(timeout=0.1)
            if msg.get("id") == rid:
                for p in pending:
                    RESP_QUEUE.put(p)
                return msg
            pending.append(msg)
        except queue.Empty:
            pass
    for p in pending:
        RESP_QUEUE.put(p)
    return None


def send(method, params=None, notification=False):
    """Send a JSON-RPC message and optionally wait for a response."""
    global REQ_ID
    body = {"jsonrpc": "2.0", "method": method}
    if not notification:
        REQ_ID += 1
        body["id"] = REQ_ID
    if params:
        body["params"] = params
    data = json.dumps(body).encode()
    req = urllib.request.Request(
        MSG_URL, data=data, headers={"Content-Type": "application/json"}
    )
    try:
        urllib.request.urlopen(req)
    except urllib.error.HTTPError:
        pass
    return None if notification else wait_response(REQ_ID)


def call_tool(name, args):
    """Invoke an MCP tool and return the raw JSON-RPC response."""
    return send("tools/call", {"name": name, "arguments": args})


def text_of(resp):
    """Extract the first text content from an MCP tool response."""
    if resp and "result" in resp:
        content = resp["result"].get("content", [])
        if content:
            return content[0].get("text", "")
    if resp and "error" in resp:
        return f"ERROR: {resp['error'].get('message', str(resp['error']))}"
    return "NO_RESPONSE"


# ---------------------------------------------------------------------------
# Test runner
# ---------------------------------------------------------------------------
PASS = 0
FAIL = 0
ERRORS = []

OK = "\033[32m\u2713\033[0m"
KO = "\033[31m\u2717\033[0m"


def check(
    desc,
    tool,
    args,
    *,
    exact=None,
    contains=None,
    numeric_delta=None,
    expected=None,
    is_error=False,
    error_contains=None,
):
    """
    Call *tool* with *args* and validate the response.

    Validation modes (pick one):
      exact          — text must equal str(exact)
      contains       — text must contain the substring
      numeric_delta  — parse as float, compare to *expected* within delta
      is_error       — response must be an MCP error OR text contains 'Error'
      error_contains — like is_error + text must also contain a substring
    """
    global PASS, FAIL
    resp = call_tool(tool, args)
    txt = text_of(resp)
    ok = True
    detail = ""

    is_mcp_err = resp and "result" in resp and resp["result"].get("isError")
    has_err_txt = any(
        k in txt.lower() for k in ("error", "exception", "undefined")
    )

    if is_error or error_contains:
        if is_mcp_err or has_err_txt:
            detail = "error as expected"
            if error_contains and error_contains not in txt:
                ok = False
                detail = f"error but missing '{error_contains}': {txt[:120]}"
        else:
            ok = False
            detail = f"expected error, got: {txt[:120]}"
    elif exact is not None:
        if txt.strip() == str(exact):
            detail = txt.strip()
        else:
            ok = False
            detail = f"expected '{exact}', got '{txt.strip()[:120]}'"
    elif numeric_delta is not None:
        try:
            val = float(txt.strip())
            if abs(val - float(expected)) <= numeric_delta:
                detail = txt.strip()
            else:
                ok = False
                detail = f"expected ~{expected} (+-{numeric_delta}), got {txt.strip()}"
        except ValueError:
            ok = False
            detail = f"expected numeric ~{expected}, got '{txt.strip()[:120]}'"
    elif contains is not None:
        if contains in txt:
            detail = f"contains '{contains}'"
        else:
            ok = False
            detail = f"missing '{contains}' in: {txt[:120]}"
    else:
        detail = txt[:120]

    icon = OK if ok else KO
    print(f"  {icon} {desc}: {detail}")
    if ok:
        PASS += 1
    else:
        FAIL += 1
        ERRORS.append(f"  {desc}: {detail}")


# ---------------------------------------------------------------------------
# Test suites
# ---------------------------------------------------------------------------

def test_basic_calculator():
    """BasicCalculatorTool — 8 tools."""
    print("\n[BasicCalculatorTool]")

    # --- success ---
    check("add(2.5, 3.7) = 6.2", "add", {"first": "2.5", "second": "3.7"}, exact="6.2")
    check("add(0, 0) = 0", "add", {"first": "0", "second": "0"}, exact="0")
    check("subtract(10, 3) = 7", "subtract", {"first": "10", "second": "3"}, exact="7")
    check("multiply(4, 5.5) = 22.0", "multiply", {"first": "4", "second": "5.5"}, exact="22.0")
    check("divide(10, 3)", "divide", {"first": "10", "second": "3"}, contains="3.3333")
    check("divide(7, 2) = 3.5", "divide", {"first": "7", "second": "2"}, contains="3.5")
    check("modulo(10, 3) = 1", "modulo", {"first": "10", "second": "3"}, exact="1")
    check("power(2, 10) = 1024", "power", {"base": "2", "exponent": "10"}, exact="1024")
    check("power(5, 0) = 1", "power", {"base": "5", "exponent": "0"}, exact="1")
    check("abs(-42.5) = 42.5", "abs", {"value": "-42.5"}, exact="42.5")
    check("abs(0) = 0", "abs", {"value": "0"}, exact="0")

    # --- errors ---
    print("  --- error cases ---")
    check("divide(1, 0) => division by zero", "divide",
          {"first": "1", "second": "0"}, is_error=True)
    check("modulo(5, 0) => division by zero", "modulo",
          {"first": "5", "second": "0"}, is_error=True)
    check("power(2, -1) => negative exponent", "power",
          {"base": "2", "exponent": "-1"}, is_error=True)
    check("add(abc, 1) => invalid number", "add",
          {"first": "abc", "second": "1"}, is_error=True)
    check("multiply(, 1) => empty string", "multiply",
          {"first": "", "second": "1"}, is_error=True)


def test_scientific_calculator():
    """ScientificCalculatorTool — 7 tools."""
    print("\n[ScientificCalculatorTool]")

    # --- success ---
    check("sqrt(144) = 12.0", "sqrt", {"number": 144}, exact="12.0")
    check("sqrt(0) = 0.0", "sqrt", {"number": 0}, exact="0.0")
    check("sqrt(2)", "sqrt", {"number": 2},
          numeric_delta=1e-10, expected=1.4142135623730951)
    check("log(e) = 1.0", "log", {"number": 2.718281828459045}, exact="1.0")
    check("log10(100) = 2.0", "log10", {"number": 100}, exact="2.0")
    check("log10(1000) = 3.0", "log10", {"number": 1000}, exact="3.0")
    check("factorial(0) = 1", "factorial", {"num": 0}, exact="1")
    check("factorial(5) = 120", "factorial", {"num": 5}, exact="120")
    check("factorial(10) = 3628800", "factorial", {"num": 10}, exact="3628800")
    check("factorial(20)", "factorial", {"num": 20}, exact="2432902008176640000")

    # --- errors (return 'Error:' string, NOT exceptions) ---
    print("  --- error cases (Error: strings) ---")
    check("sqrt(-1) => Error:", "sqrt", {"number": -1},
          error_contains="Error:")
    check("sqrt(-100) => Error:", "sqrt", {"number": -100},
          error_contains="undefined for negative")
    check("log(0) => Error:", "log", {"number": 0},
          error_contains="Error:")
    check("log(-5) => Error:", "log", {"number": -5},
          error_contains="undefined for non-positive")
    check("log10(0) => Error:", "log10", {"number": 0},
          error_contains="Error:")
    check("log10(-1) => Error:", "log10", {"number": -1},
          error_contains="undefined for non-positive")
    check("factorial(-1) => Error:", "factorial", {"num": -1},
          error_contains="Error:")
    check("factorial(21) => Error:", "factorial", {"num": 21},
          error_contains="only defined for integers 0 to 20")


def test_trig_precision():
    """Trigonometric precision — notable angles must be exact."""
    print("\n[Trig Precision — sin]")
    check("sin(0) = 0.0", "sin", {"degrees": 0}, exact="0.0")
    check("sin(30) = 0.5", "sin", {"degrees": 30}, exact="0.5")
    check("sin(45) = sqrt2/2", "sin", {"degrees": 45},
          numeric_delta=1e-15, expected=0.7071067811865476)
    check("sin(60) = sqrt3/2", "sin", {"degrees": 60},
          numeric_delta=1e-15, expected=0.8660254037844386)
    check("sin(90) = 1.0", "sin", {"degrees": 90}, exact="1.0")
    check("sin(120) = sqrt3/2", "sin", {"degrees": 120},
          numeric_delta=1e-15, expected=0.8660254037844386)
    check("sin(150) = 0.5", "sin", {"degrees": 150}, exact="0.5")
    check("sin(180) = 0.0", "sin", {"degrees": 180}, exact="0.0")
    check("sin(210) = -0.5", "sin", {"degrees": 210}, exact="-0.5")
    check("sin(270) = -1.0", "sin", {"degrees": 270}, exact="-1.0")
    check("sin(330) = -0.5", "sin", {"degrees": 330}, exact="-0.5")
    check("sin(360) = 0.0", "sin", {"degrees": 360}, exact="0.0")
    check("sin(-30) = -0.5", "sin", {"degrees": -30}, exact="-0.5")
    check("sin(390) = 0.5", "sin", {"degrees": 390}, exact="0.5")
    check("sin(720) = 0.0", "sin", {"degrees": 720}, exact="0.0")

    print("\n[Trig Precision — cos]")
    check("cos(0) = 1.0", "cos", {"degrees": 0}, exact="1.0")
    check("cos(30) = sqrt3/2", "cos", {"degrees": 30},
          numeric_delta=1e-15, expected=0.8660254037844386)
    check("cos(45) = sqrt2/2", "cos", {"degrees": 45},
          numeric_delta=1e-15, expected=0.7071067811865476)
    check("cos(60) = 0.5", "cos", {"degrees": 60}, exact="0.5")
    check("cos(90) = 0.0", "cos", {"degrees": 90}, exact="0.0")
    check("cos(120) = -0.5", "cos", {"degrees": 120}, exact="-0.5")
    check("cos(180) = -1.0", "cos", {"degrees": 180}, exact="-1.0")
    check("cos(270) = 0.0", "cos", {"degrees": 270}, exact="0.0")
    check("cos(360) = 1.0", "cos", {"degrees": 360}, exact="1.0")
    check("cos(-60) = 0.5", "cos", {"degrees": -60}, exact="0.5")
    check("cos(-360) = 1.0", "cos", {"degrees": -360}, exact="1.0")

    print("\n[Trig Precision — tan]")
    check("tan(0) = 0.0", "tan", {"degrees": 0}, exact="0.0")
    check("tan(30) = 1/sqrt3", "tan", {"degrees": 30},
          numeric_delta=1e-15, expected=0.5773502691896258)
    check("tan(45) = 1.0", "tan", {"degrees": 45}, exact="1.0")
    check("tan(60) = sqrt3", "tan", {"degrees": 60},
          numeric_delta=1e-15, expected=1.7320508075688772)
    check("tan(180) = 0.0", "tan", {"degrees": 180}, exact="0.0")
    check("tan(225) = 1.0", "tan", {"degrees": 225}, exact="1.0")
    check("tan(135) = -1.0", "tan", {"degrees": 135}, exact="-1.0")
    check("tan(315) = -1.0", "tan", {"degrees": 315}, exact="-1.0")

    # non-notable angles — StrictMath fallback
    check("sin(37) fallback", "sin", {"degrees": 37},
          numeric_delta=1e-10, expected=0.6018150231520483)
    check("cos(37) fallback", "cos", {"degrees": 37},
          numeric_delta=1e-10, expected=0.7986355100472928)
    check("tan(37) fallback", "tan", {"degrees": 37},
          numeric_delta=1e-10, expected=0.7535540501027942)

    # --- tan error cases ---
    print("  --- tan undefined angles ---")
    check("tan(90) => Error:", "tan", {"degrees": 90},
          error_contains="vertical asymptote")
    check("tan(270) => Error:", "tan", {"degrees": 270},
          error_contains="vertical asymptote")
    check("tan(-90) => Error:", "tan", {"degrees": -90},
          error_contains="Error:")
    check("tan(-270) => Error:", "tan", {"degrees": -270},
          error_contains="Error:")
    check("tan(450) => Error: (90+360)", "tan", {"degrees": 450},
          error_contains="Error:")
    check("tan(810) => Error: (90+720)", "tan", {"degrees": 810},
          error_contains="Error:")


def test_graphing_calculator():
    """GraphingCalculatorTool — 3 tools."""
    print("\n[GraphingCalculatorTool]")

    # --- plotFunction precision ---
    resp = call_tool("plotFunction", {
        "expression": "x^2", "variable": "x", "min": -1, "max": 1, "steps": 10
    })
    txt = text_of(resp)
    neg04 = '"x":-0.4,' in txt
    pos04 = '"x":0.4,' in txt
    no_drift = "-0.39999" not in txt and "0.40000" not in txt
    if neg04 and pos04 and no_drift:
        print(f"  {OK} plotFunction x-values: -0.4 and 0.4 exact (no drift)")
        global PASS
        PASS += 1
    else:
        print(f"  {KO} plotFunction drift! neg04={neg04} pos04={pos04} noDrift={no_drift}")
        global FAIL
        FAIL += 1
        ERRORS.append("plotFunction: x-value drift")

    check("solveEquation x^2-4 => 2.0", "solveEquation",
          {"expression": "x^2 - 4", "variable": "x", "initialGuess": 3.0},
          numeric_delta=1e-6, expected=2.0)
    check("solveEquation x^2-4 => -2.0", "solveEquation",
          {"expression": "x^2 - 4", "variable": "x", "initialGuess": -3.0},
          numeric_delta=1e-6, expected=-2.0)
    check("findRoots x^2-4 has -2", "findRoots",
          {"expression": "x^2 - 4", "variable": "x", "min": -5, "max": 5},
          contains="-2")
    check("findRoots x^2+1 => empty", "findRoots",
          {"expression": "x^2 + 1", "variable": "x", "min": -5, "max": 5},
          exact="[]")

    # --- errors ---
    print("  --- error cases ---")
    check("plotFunction steps=0 => error", "plotFunction",
          {"expression": "x", "variable": "x", "min": 0, "max": 1, "steps": 0},
          is_error=True)
    check("plotFunction steps=-1 => error", "plotFunction",
          {"expression": "x", "variable": "x", "min": 0, "max": 1, "steps": -1},
          is_error=True)
    check("plotFunction min>=max => error", "plotFunction",
          {"expression": "x", "variable": "x", "min": 5, "max": 2, "steps": 10},
          is_error=True)
    check("plotFunction min==max => error", "plotFunction",
          {"expression": "x", "variable": "x", "min": 3, "max": 3, "steps": 10},
          is_error=True)
    check("plotFunction bad expr => error", "plotFunction",
          {"expression": "???", "variable": "x", "min": 0, "max": 1, "steps": 5},
          is_error=True)
    check("solveEquation derivative=0 => error", "solveEquation",
          {"expression": "5", "variable": "x", "initialGuess": 1.0},
          is_error=True)


def test_financial_calculator():
    """FinancialCalculatorTool — 6 tools."""
    print("\n[FinancialCalculatorTool]")

    # --- success ---
    check("compoundInterest(1000, 5%, 3y, 12)", "compoundInterest",
          {"principal": "1000", "annualRate": "5", "years": "3", "compoundsPerYear": 12},
          contains="1161")
    check("loanPayment(100000, 6%, 30y)", "loanPayment",
          {"principal": "100000", "annualRate": "6", "years": "30"},
          contains="599")
    check("loanPayment zero rate", "loanPayment",
          {"principal": "12000", "annualRate": "0", "years": "1"},
          contains="1000")
    check("futureValueAnnuity(500, 7%, 20y)", "futureValueAnnuity",
          {"payment": "500", "annualRate": "7", "years": "20"},
          contains="20497")
    check("presentValue(100000, 5%, 10y)", "presentValue",
          {"futureValue": "100000", "annualRate": "5", "years": "10"},
          contains="6139")
    check("returnOnInvestment(1500, 1000) = 50%", "returnOnInvestment",
          {"gain": "1500", "cost": "1000"}, contains="50")
    check("amortizationSchedule(10000, 5%, 1y)", "amortizationSchedule",
          {"principal": "10000", "annualRate": "5", "years": "1"},
          contains="month")

    # --- errors ---
    print("  --- error cases ---")
    check("compoundInterest principal=0 => error", "compoundInterest",
          {"principal": "0", "annualRate": "5", "years": "3", "compoundsPerYear": 12},
          is_error=True)
    check("compoundInterest principal=-1 => error", "compoundInterest",
          {"principal": "-1", "annualRate": "5", "years": "3", "compoundsPerYear": 12},
          is_error=True)
    check("compoundInterest rate=-5 => error", "compoundInterest",
          {"principal": "1000", "annualRate": "-5", "years": "3", "compoundsPerYear": 12},
          is_error=True)
    check("compoundInterest years=0 => error", "compoundInterest",
          {"principal": "1000", "annualRate": "5", "years": "0", "compoundsPerYear": 12},
          is_error=True)
    check("compoundInterest compounds=0 => error", "compoundInterest",
          {"principal": "1000", "annualRate": "5", "years": "3", "compoundsPerYear": 0},
          is_error=True)
    check("loanPayment principal=0 => error", "loanPayment",
          {"principal": "0", "annualRate": "6", "years": "30"},
          is_error=True)
    check("loanPayment years=0 => error", "loanPayment",
          {"principal": "100000", "annualRate": "6", "years": "0"},
          is_error=True)
    check("presentValue future=0 => error", "presentValue",
          {"futureValue": "0", "annualRate": "5", "years": "10"},
          is_error=True)
    check("presentValue years=0 => error", "presentValue",
          {"futureValue": "100000", "annualRate": "5", "years": "0"},
          is_error=True)
    check("futureValueAnnuity payment=0 => error", "futureValueAnnuity",
          {"payment": "0", "annualRate": "7", "years": "20"},
          is_error=True)
    check("futureValueAnnuity years=0 => error", "futureValueAnnuity",
          {"payment": "500", "annualRate": "7", "years": "0"},
          is_error=True)
    check("returnOnInvestment cost=0 => error", "returnOnInvestment",
          {"gain": "1500", "cost": "0"}, is_error=True)
    check("amortizationSchedule principal=0 => error", "amortizationSchedule",
          {"principal": "0", "annualRate": "5", "years": "1"},
          is_error=True)
    check("amortizationSchedule years=0 => error", "amortizationSchedule",
          {"principal": "10000", "annualRate": "5", "years": "0"},
          is_error=True)


def test_vector_calculator():
    """VectorCalculatorTool — 4 tools."""
    print("\n[VectorCalculatorTool]")

    # --- success ---
    check("sumArray(1,2,3,4,5)", "sumArray", {"numbers": "1,2,3,4,5"},
          contains="15")
    check("sumArray(10)", "sumArray", {"numbers": "10"}, contains="10")
    check("dotProduct([1,2,3],[4,5,6]) = 32", "dotProduct",
          {"first": "1,2,3", "second": "4,5,6"}, contains="32")
    check("magnitudeArray(3,4) = 5", "magnitudeArray", {"numbers": "3,4"},
          contains="5")
    check("scaleArray([1,2,3], 10)", "scaleArray",
          {"numbers": "1,2,3", "scalar": "10"}, contains="10")

    # --- errors ---
    print("  --- error cases ---")
    check("sumArray('') => error", "sumArray", {"numbers": ""},
          is_error=True)
    check("sumArray(abc) => error", "sumArray", {"numbers": "abc"},
          is_error=True)
    check("dotProduct unequal length => error", "dotProduct",
          {"first": "1,2", "second": "1,2,3"}, is_error=True)
    check("dotProduct empty => error", "dotProduct",
          {"first": "", "second": "1,2"}, is_error=True)
    check("magnitudeArray('') => error", "magnitudeArray", {"numbers": ""},
          is_error=True)
    check("scaleArray empty => error", "scaleArray",
          {"numbers": "", "scalar": "10"}, is_error=True)
    check("scaleArray bad scalar => error", "scaleArray",
          {"numbers": "1,2", "scalar": "xyz"}, is_error=True)


def test_expression_evaluator():
    """ProgrammableCalculatorTool — 2 tools + ExpressionEvaluator engine."""
    print("\n[ExpressionEvaluator]")

    # --- success ---
    check("evaluate(2+3*4) = 14", "evaluate", {"expression": "2+3*4"},
          exact="14.0")
    check("evaluate((2+3)*4) = 20", "evaluate", {"expression": "(2+3)*4"},
          exact="20.0")
    check("evaluate(2^10) = 1024", "evaluate", {"expression": "2^10"},
          exact="1024.0")
    check("evaluate(sqrt(16)) = 4", "evaluate", {"expression": "sqrt(16)"},
          exact="4.0")
    check("evaluate(abs(-5)) = 5", "evaluate", {"expression": "abs(-5)"},
          exact="5.0")
    check("evaluateWithVariables(2*x+y)", "evaluateWithVariables",
          {"expression": "2*x+y", "variables": '{"x":3.0,"y":1.0}'},
          exact="7.0")

    # --- errors ---
    print("  --- error cases ---")
    check("evaluate('') => error", "evaluate",
          {"expression": ""}, is_error=True)
    check("evaluate('???') => error", "evaluate",
          {"expression": "???"}, is_error=True)
    check("evaluate unmatched paren => error", "evaluate",
          {"expression": "(2+3"}, is_error=True)
    check("evaluate trailing paren => error", "evaluate",
          {"expression": "2+3)"}, is_error=True)
    check("evaluate unknown func => error", "evaluate",
          {"expression": "foo(1)"}, is_error=True)
    check("evaluateWithVars empty expr => error", "evaluateWithVariables",
          {"expression": "", "variables": '{"x":1}'}, is_error=True)
    check("evaluateWithVars empty vars => error", "evaluateWithVariables",
          {"expression": "x+1", "variables": ""}, is_error=True)
    check("evaluateWithVars unknown var => error", "evaluateWithVariables",
          {"expression": "z+1", "variables": '{"x":1.0}'}, is_error=True)


def test_tape_calculator():
    """PrintingCalculatorTool — 1 tool."""
    print("\n[TapeCalculatorTool]")

    # --- success ---
    check("tape(+10, +20, =) => 30", "calculateWithTape",
          {"operations": '[{"op":"+","value":10},{"op":"+","value":20},{"op":"=","value":0}]'},
          contains="30")
    check("tape(+100, -25, =) => 75", "calculateWithTape",
          {"operations": '[{"op":"+","value":100},{"op":"-","value":25},{"op":"=","value":0}]'},
          contains="75")
    check("tape clear", "calculateWithTape",
          {"operations": '[{"op":"+","value":50},{"op":"C","value":0},{"op":"+","value":10},{"op":"=","value":0}]'},
          contains="10")

    # --- errors ---
    print("  --- error cases ---")
    check("tape empty string => error", "calculateWithTape",
          {"operations": ""}, is_error=True)
    check("tape not JSON array => error", "calculateWithTape",
          {"operations": "not-json"}, is_error=True)
    check("tape division by zero => error", "calculateWithTape",
          {"operations": '[{"op":"+","value":10},{"op":"/","value":0}]'},
          is_error=True)
    check("tape missing op field => error", "calculateWithTape",
          {"operations": '[{"value":10}]'}, is_error=True)
    check("tape unknown op => error", "calculateWithTape",
          {"operations": '[{"op":"X","value":10}]'}, is_error=True)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    global PASS, FAIL

    parser = argparse.ArgumentParser(description="MCP tool integration tests")
    parser.add_argument(
        "--base", default="http://localhost:44321",
        help="Base URL of the MCP server (default: http://localhost:44321)",
    )
    args = parser.parse_args()
    base = args.base.rstrip("/")

    print(f"Connecting to {base}/sse ...")
    thread = threading.Thread(target=sse_listener, args=(base,), daemon=True)
    thread.start()

    if not ENDPOINT_READY.wait(timeout=5):
        print("ERROR: could not obtain SSE endpoint. Is the server running?")
        sys.exit(2)
    print(f"Session: {MSG_URL}")

    # Initialize MCP
    resp = send("initialize", {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "mcp-test", "version": "1.0"},
    })
    if not resp:
        print("ERROR: MCP initialize failed")
        sys.exit(2)
    info = resp["result"]["serverInfo"]
    print(f"Server: {info['name']} v{info['version']}")
    send("notifications/initialized", notification=True)
    time.sleep(0.3)

    # List tools
    resp = send("tools/list")
    tools = sorted(t["name"] for t in resp["result"]["tools"])
    print(f"Tools: {len(tools)}")
    print(f"  {', '.join(tools)}")

    # Run all suites
    print("\n" + "=" * 64)
    print("  MCP TOOL INTEGRATION TESTS")
    print("=" * 64)

    test_basic_calculator()
    test_scientific_calculator()
    test_trig_precision()
    test_graphing_calculator()
    test_financial_calculator()
    test_vector_calculator()
    test_expression_evaluator()
    test_tape_calculator()

    # Report
    total = PASS + FAIL
    print("\n" + "=" * 64)
    if FAIL == 0:
        print(f"  \033[32mALL {total} TESTS PASSED\033[0m")
    else:
        print(f"  \033[32m{PASS} passed\033[0m, \033[31m{FAIL} failed\033[0m / {total}")
    print("=" * 64)

    if ERRORS:
        print("\nFailed:")
        for e in ERRORS:
            print(e)

    sys.exit(0 if FAIL == 0 else 1)


if __name__ == "__main__":
    main()
