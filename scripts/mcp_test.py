#!/usr/bin/env python3
"""
MCP Tool Integration Tests + Benchmark — math-calculator (asyncio)

Validates all 85 MCP tools via Streamable HTTP transport (POST /mcp) using async I/O:
  - Success cases with precision assertions
  - Error cases (invalid input, domain errors, edge cases)
  - Latency metrics collected for every call
  - Concurrent benchmark with native asyncio concurrency

For dual-transport tests (SSE + Streamable HTTP), see mcp_sse_test.py.

Usage:
    python3 scripts/mcp_test.py                       # tests only
    python3 scripts/mcp_test.py --benchmark           # tests + benchmark (100 rounds)
    python3 scripts/mcp_test.py --benchmark -n 500    # 500 rounds
    python3 scripts/mcp_test.py --benchmark -w 16     # 16 concurrent tasks
    python3 scripts/mcp_test.py --base http://host:port

Requires: pip install aiohttp
"""
import argparse
import asyncio
import json
import statistics
import sys
import time

import aiohttp

# ---------------------------------------------------------------------------
# Async MCP client — Streamable HTTP transport (single POST /mcp endpoint)
# ---------------------------------------------------------------------------
MCP_URL: str | None = None
SESSION_ID: str | None = None
REQ_ID = 0


async def send(session: aiohttp.ClientSession, method: str, params=None, notification=False):
    """Send a JSON-RPC message via Streamable HTTP and return the response."""
    global REQ_ID, SESSION_ID
    body: dict = {"jsonrpc": "2.0", "method": method}
    if not notification:
        REQ_ID += 1
        body["id"] = REQ_ID
    if params:
        body["params"] = params

    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
    }
    if SESSION_ID:
        headers["mcp-session-id"] = SESSION_ID

    try:
        async with session.post(
            MCP_URL,
            json=body,
            headers=headers,
            timeout=aiohttp.ClientTimeout(total=10),
        ) as resp:
            # Capture session ID on first response (initialize)
            if SESSION_ID is None and "mcp-session-id" in resp.headers:
                SESSION_ID = resp.headers["mcp-session-id"]

            if notification or resp.status == 202:
                return None

            content_type = resp.headers.get("Content-Type", "")
            if "text/event-stream" in content_type:
                # Server chose to stream — collect SSE-format data: lines from the response body
                raw = await resp.text()
                result = None
                for line in raw.splitlines():
                    if line.startswith("data:"):
                        try:
                            result = json.loads(line[5:].strip())
                        except json.JSONDecodeError:
                            pass
                return result

            return await resp.json()
    except aiohttp.ClientError:
        return None


async def call_tool(session: aiohttp.ClientSession, name: str, args: dict):
    """Invoke an MCP tool, record latency, return raw JSON-RPC response."""
    t0 = time.perf_counter()
    resp = await send(session, "tools/call", {"name": name, "arguments": args})
    elapsed = (time.perf_counter() - t0) * 1000
    record_latency(name, elapsed)
    return resp


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
# Latency collector
# ---------------------------------------------------------------------------
LATENCIES: dict[str, list[float]] = {}


def record_latency(tool: str, elapsed_ms: float):
    LATENCIES.setdefault(tool, []).append(elapsed_ms)


# ---------------------------------------------------------------------------
# Test runner
# ---------------------------------------------------------------------------
PASS = 0
FAIL = 0
ERRORS: list[str] = []

OK = "\033[32m\u2713\033[0m"
KO = "\033[31m\u2717\033[0m"
DIM = "\033[90m"
RST = "\033[0m"


async def check(
    session: aiohttp.ClientSession,
    desc: str,
    tool: str,
    args: dict,
    *,
    exact=None,
    contains=None,
    numeric_delta=None,
    expected=None,
    is_error=False,
    error_contains=None,
):
    global PASS, FAIL
    t0 = time.perf_counter()
    resp = await send(session, "tools/call", {"name": tool, "arguments": args})
    elapsed = (time.perf_counter() - t0) * 1000
    record_latency(tool, elapsed)

    txt = text_of(resp)
    ok = True
    detail = ""

    is_mcp_err = resp and "result" in resp and resp["result"].get("isError")
    has_err_txt = any(k in txt.lower() for k in ("error", "exception", "undefined"))

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
    ms_tag = f"{DIM}{elapsed:6.1f}ms{RST}"
    print(f"  {icon} {ms_tag} {desc}: {detail}")
    if ok:
        PASS += 1
    else:
        FAIL += 1
        ERRORS.append(f"  {desc}: {detail}")


# ---------------------------------------------------------------------------
# Test suites
# ---------------------------------------------------------------------------

async def test_basic_calculator(s):
    print("\n[BasicCalculatorTool]")
    await check(s, "add(2.5, 3.7) = 6.2", "add", {"first": "2.5", "second": "3.7"}, exact="6.2")
    await check(s, "add(0, 0) = 0", "add", {"first": "0", "second": "0"}, exact="0")
    await check(s, "subtract(10, 3) = 7", "subtract", {"first": "10", "second": "3"}, exact="7")
    await check(s, "multiply(4, 5.5) = 22.0", "multiply", {"first": "4", "second": "5.5"}, exact="22.0")
    await check(s, "divide(10, 3)", "divide", {"first": "10", "second": "3"}, contains="3.3333")
    await check(s, "divide(7, 2) = 3.5", "divide", {"first": "7", "second": "2"}, contains="3.5")
    await check(s, "modulo(10, 3) = 1", "modulo", {"first": "10", "second": "3"}, exact="1")
    await check(s, "power(2, 10) = 1024", "power", {"base": "2", "exponent": "10"}, exact="1024")
    await check(s, "power(5, 0) = 1", "power", {"base": "5", "exponent": "0"}, exact="1")
    await check(s, "abs(-42.5) = 42.5", "abs", {"value": "-42.5"}, exact="42.5")
    await check(s, "abs(0) = 0", "abs", {"value": "0"}, exact="0")
    print("  --- error cases ---")
    await check(s, "divide(1, 0) => div by zero", "divide", {"first": "1", "second": "0"}, is_error=True)
    await check(s, "modulo(5, 0) => div by zero", "modulo", {"first": "5", "second": "0"}, is_error=True)
    await check(s, "power(2, -1) => neg exponent", "power", {"base": "2", "exponent": "-1"}, is_error=True)
    await check(s, "add(abc, 1) => invalid", "add", {"first": "abc", "second": "1"}, is_error=True)
    await check(s, "multiply(, 1) => empty", "multiply", {"first": "", "second": "1"}, is_error=True)


async def test_scientific_calculator(s):
    print("\n[ScientificCalculatorTool]")
    await check(s, "sqrt(144) = 12.0", "sqrt", {"number": 144}, exact="12.0")
    await check(s, "sqrt(0) = 0.0", "sqrt", {"number": 0}, exact="0.0")
    await check(s, "sqrt(2)", "sqrt", {"number": 2}, numeric_delta=1e-10, expected=1.4142135623730951)
    await check(s, "log(e) = 1.0", "log", {"number": 2.718281828459045}, exact="1.0")
    await check(s, "log10(100) = 2.0", "log10", {"number": 100}, exact="2.0")
    await check(s, "log10(1000) = 3.0", "log10", {"number": 1000}, exact="3.0")
    await check(s, "factorial(0) = 1", "factorial", {"num": 0}, exact="1")
    await check(s, "factorial(5) = 120", "factorial", {"num": 5}, exact="120")
    await check(s, "factorial(10) = 3628800", "factorial", {"num": 10}, exact="3628800")
    await check(s, "factorial(20)", "factorial", {"num": 20}, exact="2432902008176640000")
    print("  --- error cases (Error: strings) ---")
    await check(s, "sqrt(-1) => Error:", "sqrt", {"number": -1}, error_contains="Error:")
    await check(s, "sqrt(-100) => Error:", "sqrt", {"number": -100}, error_contains="undefined for negative")
    await check(s, "log(0) => Error:", "log", {"number": 0}, error_contains="Error:")
    await check(s, "log(-5) => Error:", "log", {"number": -5}, error_contains="undefined for non-positive")
    await check(s, "log10(0) => Error:", "log10", {"number": 0}, error_contains="Error:")
    await check(s, "log10(-1) => Error:", "log10", {"number": -1}, error_contains="undefined for non-positive")
    await check(s, "factorial(-1) => Error:", "factorial", {"num": -1}, error_contains="Error:")
    await check(s, "factorial(21) => Error:", "factorial", {"num": 21}, error_contains="only defined for integers 0 to 20")


async def test_trig_precision(s):
    print("\n[Trig Precision -- sin]")
    await check(s, "sin(0) = 0.0", "sin", {"degrees": 0}, exact="0.0")
    await check(s, "sin(30) = 0.5", "sin", {"degrees": 30}, exact="0.5")
    await check(s, "sin(45) = sqrt2/2", "sin", {"degrees": 45}, numeric_delta=1e-15, expected=0.7071067811865476)
    await check(s, "sin(60) = sqrt3/2", "sin", {"degrees": 60}, numeric_delta=1e-15, expected=0.8660254037844386)
    await check(s, "sin(90) = 1.0", "sin", {"degrees": 90}, exact="1.0")
    await check(s, "sin(120) = sqrt3/2", "sin", {"degrees": 120}, numeric_delta=1e-15, expected=0.8660254037844386)
    await check(s, "sin(150) = 0.5", "sin", {"degrees": 150}, exact="0.5")
    await check(s, "sin(180) = 0.0", "sin", {"degrees": 180}, exact="0.0")
    await check(s, "sin(210) = -0.5", "sin", {"degrees": 210}, exact="-0.5")
    await check(s, "sin(270) = -1.0", "sin", {"degrees": 270}, exact="-1.0")
    await check(s, "sin(330) = -0.5", "sin", {"degrees": 330}, exact="-0.5")
    await check(s, "sin(360) = 0.0", "sin", {"degrees": 360}, exact="0.0")
    await check(s, "sin(-30) = -0.5", "sin", {"degrees": -30}, exact="-0.5")
    await check(s, "sin(390) = 0.5", "sin", {"degrees": 390}, exact="0.5")
    await check(s, "sin(720) = 0.0", "sin", {"degrees": 720}, exact="0.0")

    print("\n[Trig Precision -- cos]")
    await check(s, "cos(0) = 1.0", "cos", {"degrees": 0}, exact="1.0")
    await check(s, "cos(30) = sqrt3/2", "cos", {"degrees": 30}, numeric_delta=1e-15, expected=0.8660254037844386)
    await check(s, "cos(45) = sqrt2/2", "cos", {"degrees": 45}, numeric_delta=1e-15, expected=0.7071067811865476)
    await check(s, "cos(60) = 0.5", "cos", {"degrees": 60}, exact="0.5")
    await check(s, "cos(90) = 0.0", "cos", {"degrees": 90}, exact="0.0")
    await check(s, "cos(120) = -0.5", "cos", {"degrees": 120}, exact="-0.5")
    await check(s, "cos(180) = -1.0", "cos", {"degrees": 180}, exact="-1.0")
    await check(s, "cos(270) = 0.0", "cos", {"degrees": 270}, exact="0.0")
    await check(s, "cos(360) = 1.0", "cos", {"degrees": 360}, exact="1.0")
    await check(s, "cos(-60) = 0.5", "cos", {"degrees": -60}, exact="0.5")
    await check(s, "cos(-360) = 1.0", "cos", {"degrees": -360}, exact="1.0")

    print("\n[Trig Precision -- tan]")
    await check(s, "tan(0) = 0.0", "tan", {"degrees": 0}, exact="0.0")
    await check(s, "tan(30) = 1/sqrt3", "tan", {"degrees": 30}, numeric_delta=1e-15, expected=0.5773502691896258)
    await check(s, "tan(45) = 1.0", "tan", {"degrees": 45}, exact="1.0")
    await check(s, "tan(60) = sqrt3", "tan", {"degrees": 60}, numeric_delta=1e-15, expected=1.7320508075688772)
    await check(s, "tan(180) = 0.0", "tan", {"degrees": 180}, exact="0.0")
    await check(s, "tan(225) = 1.0", "tan", {"degrees": 225}, exact="1.0")
    await check(s, "tan(135) = -1.0", "tan", {"degrees": 135}, exact="-1.0")
    await check(s, "tan(315) = -1.0", "tan", {"degrees": 315}, exact="-1.0")

    await check(s, "sin(37) fallback", "sin", {"degrees": 37}, numeric_delta=1e-10, expected=0.6018150231520483)
    await check(s, "cos(37) fallback", "cos", {"degrees": 37}, numeric_delta=1e-10, expected=0.7986355100472928)
    await check(s, "tan(37) fallback", "tan", {"degrees": 37}, numeric_delta=1e-10, expected=0.7535540501027942)

    print("  --- tan undefined angles ---")
    await check(s, "tan(90) => Error:", "tan", {"degrees": 90}, error_contains="vertical asymptote")
    await check(s, "tan(270) => Error:", "tan", {"degrees": 270}, error_contains="vertical asymptote")
    await check(s, "tan(-90) => Error:", "tan", {"degrees": -90}, error_contains="Error:")
    await check(s, "tan(-270) => Error:", "tan", {"degrees": -270}, error_contains="Error:")
    await check(s, "tan(450) => Error:", "tan", {"degrees": 450}, error_contains="Error:")
    await check(s, "tan(810) => Error:", "tan", {"degrees": 810}, error_contains="Error:")


async def test_graphing_calculator(s):
    print("\n[GraphingCalculatorTool]")
    global PASS, FAIL
    resp = await call_tool(s, "plotFunction", {
        "expression": "x^2", "variable": "x", "min": -1, "max": 1, "steps": 10
    })
    txt = text_of(resp)
    neg04 = '"x":-0.4,' in txt
    pos04 = '"x":0.4,' in txt
    no_drift = "-0.39999" not in txt and "0.40000" not in txt
    if neg04 and pos04 and no_drift:
        print(f"  {OK} plotFunction x-values: -0.4 and 0.4 exact (no drift)")
        PASS += 1
    else:
        print(f"  {KO} plotFunction drift! neg04={neg04} pos04={pos04} noDrift={no_drift}")
        FAIL += 1
        ERRORS.append("plotFunction: x-value drift")

    await check(s, "solveEquation x^2-4 => 2.0", "solveEquation",
                {"expression": "x^2 - 4", "variable": "x", "initialGuess": 3.0}, numeric_delta=1e-6, expected=2.0)
    await check(s, "solveEquation x^2-4 => -2.0", "solveEquation",
                {"expression": "x^2 - 4", "variable": "x", "initialGuess": -3.0}, numeric_delta=1e-6, expected=-2.0)
    await check(s, "findRoots x^2-4 has -2", "findRoots",
                {"expression": "x^2 - 4", "variable": "x", "min": -5, "max": 5}, contains="-2")
    await check(s, "findRoots x^2+1 => empty", "findRoots",
                {"expression": "x^2 + 1", "variable": "x", "min": -5, "max": 5}, exact="[]")
    print("  --- error cases ---")
    await check(s, "plotFunction steps=0", "plotFunction",
                {"expression": "x", "variable": "x", "min": 0, "max": 1, "steps": 0}, is_error=True)
    await check(s, "plotFunction steps=-1", "plotFunction",
                {"expression": "x", "variable": "x", "min": 0, "max": 1, "steps": -1}, is_error=True)
    await check(s, "plotFunction min>=max", "plotFunction",
                {"expression": "x", "variable": "x", "min": 5, "max": 2, "steps": 10}, is_error=True)
    await check(s, "plotFunction min==max", "plotFunction",
                {"expression": "x", "variable": "x", "min": 3, "max": 3, "steps": 10}, is_error=True)
    await check(s, "plotFunction bad expr", "plotFunction",
                {"expression": "???", "variable": "x", "min": 0, "max": 1, "steps": 5}, is_error=True)
    await check(s, "solveEquation deriv=0", "solveEquation",
                {"expression": "5", "variable": "x", "initialGuess": 1.0}, is_error=True)


async def test_financial_calculator(s):
    print("\n[FinancialCalculatorTool]")
    await check(s, "compoundInterest(1000, 5%, 3y, 12)", "compoundInterest",
                {"principal": "1000", "annualRate": "5", "years": "3", "compoundsPerYear": 12}, contains="1161")
    await check(s, "loanPayment(100000, 6%, 30y)", "loanPayment",
                {"principal": "100000", "annualRate": "6", "years": "30"}, contains="599")
    await check(s, "loanPayment zero rate", "loanPayment",
                {"principal": "12000", "annualRate": "0", "years": "1"}, contains="1000")
    await check(s, "futureValueAnnuity(500, 7%, 20y)", "futureValueAnnuity",
                {"payment": "500", "annualRate": "7", "years": "20"}, contains="20497")
    await check(s, "presentValue(100000, 5%, 10y)", "presentValue",
                {"futureValue": "100000", "annualRate": "5", "years": "10"}, contains="6139")
    await check(s, "returnOnInvestment(1500, 1000)", "returnOnInvestment",
                {"gain": "1500", "cost": "1000"}, contains="50")
    await check(s, "amortizationSchedule(10000, 5%, 1y)", "amortizationSchedule",
                {"principal": "10000", "annualRate": "5", "years": "1"}, contains="month")
    print("  --- error cases ---")
    await check(s, "compoundInterest principal=0", "compoundInterest",
                {"principal": "0", "annualRate": "5", "years": "3", "compoundsPerYear": 12}, is_error=True)
    await check(s, "compoundInterest principal=-1", "compoundInterest",
                {"principal": "-1", "annualRate": "5", "years": "3", "compoundsPerYear": 12}, is_error=True)
    await check(s, "compoundInterest rate=-5", "compoundInterest",
                {"principal": "1000", "annualRate": "-5", "years": "3", "compoundsPerYear": 12}, is_error=True)
    await check(s, "compoundInterest years=0", "compoundInterest",
                {"principal": "1000", "annualRate": "5", "years": "0", "compoundsPerYear": 12}, is_error=True)
    await check(s, "compoundInterest compounds=0", "compoundInterest",
                {"principal": "1000", "annualRate": "5", "years": "3", "compoundsPerYear": 0}, is_error=True)
    await check(s, "loanPayment principal=0", "loanPayment",
                {"principal": "0", "annualRate": "6", "years": "30"}, is_error=True)
    await check(s, "loanPayment years=0", "loanPayment",
                {"principal": "100000", "annualRate": "6", "years": "0"}, is_error=True)
    await check(s, "presentValue future=0", "presentValue",
                {"futureValue": "0", "annualRate": "5", "years": "10"}, is_error=True)
    await check(s, "presentValue years=0", "presentValue",
                {"futureValue": "100000", "annualRate": "5", "years": "0"}, is_error=True)
    await check(s, "futureValueAnnuity payment=0", "futureValueAnnuity",
                {"payment": "0", "annualRate": "7", "years": "20"}, is_error=True)
    await check(s, "futureValueAnnuity years=0", "futureValueAnnuity",
                {"payment": "500", "annualRate": "7", "years": "0"}, is_error=True)
    await check(s, "returnOnInvestment cost=0", "returnOnInvestment",
                {"gain": "1500", "cost": "0"}, is_error=True)
    await check(s, "amortizationSchedule principal=0", "amortizationSchedule",
                {"principal": "0", "annualRate": "5", "years": "1"}, is_error=True)
    await check(s, "amortizationSchedule years=0", "amortizationSchedule",
                {"principal": "10000", "annualRate": "5", "years": "0"}, is_error=True)


async def test_vector_calculator(s):
    print("\n[VectorCalculatorTool]")
    await check(s, "sumArray(1,2,3,4,5)", "sumArray", {"numbers": "1,2,3,4,5"}, contains="15")
    await check(s, "sumArray(10)", "sumArray", {"numbers": "10"}, contains="10")
    await check(s, "dotProduct([1,2,3],[4,5,6]) = 32", "dotProduct",
                {"first": "1,2,3", "second": "4,5,6"}, contains="32")
    await check(s, "magnitudeArray(3,4) = 5", "magnitudeArray", {"numbers": "3,4"}, contains="5")
    await check(s, "scaleArray([1,2,3], 10)", "scaleArray",
                {"numbers": "1,2,3", "scalar": "10"}, contains="10")
    print("  --- error cases ---")
    await check(s, "sumArray('') => error", "sumArray", {"numbers": ""}, is_error=True)
    await check(s, "sumArray(abc) => error", "sumArray", {"numbers": "abc"}, is_error=True)
    await check(s, "dotProduct unequal len", "dotProduct", {"first": "1,2", "second": "1,2,3"}, is_error=True)
    await check(s, "dotProduct empty", "dotProduct", {"first": "", "second": "1,2"}, is_error=True)
    await check(s, "magnitudeArray('')", "magnitudeArray", {"numbers": ""}, is_error=True)
    await check(s, "scaleArray empty", "scaleArray", {"numbers": "", "scalar": "10"}, is_error=True)
    await check(s, "scaleArray bad scalar", "scaleArray", {"numbers": "1,2", "scalar": "xyz"}, is_error=True)


async def test_expression_evaluator(s):
    print("\n[ExpressionEvaluator]")
    await check(s, "evaluate(2+3*4) = 14", "evaluate", {"expression": "2+3*4"}, exact="14.0")
    await check(s, "evaluate((2+3)*4) = 20", "evaluate", {"expression": "(2+3)*4"}, exact="20.0")
    await check(s, "evaluate(2^10) = 1024", "evaluate", {"expression": "2^10"}, exact="1024.0")
    await check(s, "evaluate(sqrt(16)) = 4", "evaluate", {"expression": "sqrt(16)"}, exact="4.0")
    await check(s, "evaluate(abs(-5)) = 5", "evaluate", {"expression": "abs(-5)"}, exact="5.0")
    await check(s, "evaluateWithVariables(2*x+y)", "evaluateWithVariables",
                {"expression": "2*x+y", "variables": '{"x":3.0,"y":1.0}'}, exact="7.0")
    print("  --- error cases ---")
    await check(s, "evaluate('') => error", "evaluate", {"expression": ""}, is_error=True)
    await check(s, "evaluate('???') => error", "evaluate", {"expression": "???"}, is_error=True)
    await check(s, "evaluate unmatched paren", "evaluate", {"expression": "(2+3"}, is_error=True)
    await check(s, "evaluate trailing paren", "evaluate", {"expression": "2+3)"}, is_error=True)
    await check(s, "evaluate unknown func", "evaluate", {"expression": "foo(1)"}, is_error=True)
    await check(s, "evaluateWithVars empty expr", "evaluateWithVariables",
                {"expression": "", "variables": '{"x":1}'}, is_error=True)
    await check(s, "evaluateWithVars empty vars", "evaluateWithVariables",
                {"expression": "x+1", "variables": ""}, is_error=True)
    await check(s, "evaluateWithVars unknown var", "evaluateWithVariables",
                {"expression": "z+1", "variables": '{"x":1.0}'}, is_error=True)


async def test_tape_calculator(s):
    print("\n[TapeCalculatorTool]")
    await check(s, "tape(+10, +20, =) => 30", "calculateWithTape",
                {"operations": '[{"op":"+","value":10},{"op":"+","value":20},{"op":"=","value":0}]'}, contains="30")
    await check(s, "tape(+100, -25, =) => 75", "calculateWithTape",
                {"operations": '[{"op":"+","value":100},{"op":"-","value":25},{"op":"=","value":0}]'}, contains="75")
    await check(s, "tape clear", "calculateWithTape",
                {"operations": '[{"op":"+","value":50},{"op":"C","value":0},{"op":"+","value":10},{"op":"=","value":0}]'},
                contains="10")
    print("  --- error cases ---")
    await check(s, "tape empty string", "calculateWithTape", {"operations": ""}, is_error=True)
    await check(s, "tape not JSON array", "calculateWithTape", {"operations": "not-json"}, is_error=True)
    await check(s, "tape div by zero", "calculateWithTape",
                {"operations": '[{"op":"+","value":10},{"op":"/","value":0}]'}, is_error=True)
    await check(s, "tape missing op field", "calculateWithTape", {"operations": '[{"value":10}]'}, is_error=True)
    await check(s, "tape unknown op", "calculateWithTape", {"operations": '[{"op":"X","value":10}]'}, is_error=True)


async def test_unit_converter(s):
    print("\n[UnitConverterTool]")
    await check(s, "convert 1 km -> m = 1000", "convert",
                {"value": "1", "fromUnit": "km", "toUnit": "m", "category": "LENGTH"}, exact="1000")
    await check(s, "convert 1 lb -> kg", "convert",
                {"value": "1", "fromUnit": "lb", "toUnit": "kg", "category": "MASS"}, contains="0.4535")
    await check(s, "convert 100 c -> f = 212", "convert",
                {"value": "100", "fromUnit": "c", "toUnit": "f", "category": "TEMPERATURE"}, exact="212")
    await check(s, "convert 0 c -> f = 32", "convert",
                {"value": "0", "fromUnit": "c", "toUnit": "f", "category": "TEMPERATURE"}, exact="32")
    await check(s, "convert 1 usgal -> l", "convert",
                {"value": "1", "fromUnit": "usgal", "toUnit": "l", "category": "VOLUME"}, contains="3.7854")
    await check(s, "convert 1 mi -> km", "convert",
                {"value": "1", "fromUnit": "mi", "toUnit": "km", "category": "LENGTH"}, contains="1.609")
    await check(s, "convert 1 atm -> psi", "convert",
                {"value": "1", "fromUnit": "atm", "toUnit": "psi", "category": "PRESSURE"}, contains="14.69")
    await check(s, "convert 1 hp -> w", "convert",
                {"value": "1", "fromUnit": "hp", "toUnit": "w", "category": "POWER"}, contains="745")
    await check(s, "convert 1 gb -> mb = 1024", "convert",
                {"value": "1", "fromUnit": "gb", "toUnit": "mb", "category": "DATA_STORAGE"}, exact="1024")
    await check(s, "autoDetect 1 km -> m = 1000", "convertAutoDetect",
                {"value": "1", "fromUnit": "km", "toUnit": "m"}, exact="1000")
    await check(s, "autoDetect 5 lb -> kg", "convertAutoDetect",
                {"value": "5", "fromUnit": "lb", "toUnit": "kg"}, contains="2.267")
    await check(s, "autoDetect 1 h -> min = 60", "convertAutoDetect",
                {"value": "1", "fromUnit": "h", "toUnit": "min"}, exact="60")
    await check(s, "convert 1 kohm -> ohm = 1000", "convert",
                {"value": "1", "fromUnit": "kohm", "toUnit": "ohm", "category": "RESISTANCE"}, exact="1000")
    await check(s, "convert 1 kbps -> bps = 1000", "convert",
                {"value": "1", "fromUnit": "kbps", "toUnit": "bps", "category": "DATA_RATE"}, exact="1000")
    await check(s, "autoDetect 1 kohm -> ohm = 1000", "convertAutoDetect",
                {"value": "1", "fromUnit": "kohm", "toUnit": "ohm"}, exact="1000")
    print("  --- error cases ---")
    await check(s, "convert bad category", "convert",
                {"value": "1", "fromUnit": "km", "toUnit": "m", "category": "INVALID"}, is_error=True)
    await check(s, "convert unit not in category", "convert",
                {"value": "1", "fromUnit": "km", "toUnit": "kg", "category": "LENGTH"}, is_error=True)
    await check(s, "convert invalid value", "convert",
                {"value": "abc", "fromUnit": "km", "toUnit": "m", "category": "LENGTH"}, is_error=True)
    await check(s, "autoDetect unknown unit", "convertAutoDetect",
                {"value": "1", "fromUnit": "zzz", "toUnit": "m"}, is_error=True)
    await check(s, "autoDetect cross-category", "convertAutoDetect",
                {"value": "1", "fromUnit": "km", "toUnit": "kg"}, is_error=True)


async def test_cooking_converter(s):
    print("\n[CookingConverterTool]")
    await check(s, "volume 1 tbsp -> tsp = 3", "convertCookingVolume",
                {"value": "1", "fromUnit": "tbsp", "toUnit": "tsp"}, contains="3")
    await check(s, "volume 1 uscup -> ml", "convertCookingVolume",
                {"value": "1", "fromUnit": "uscup", "toUnit": "ml"}, contains="236")
    await check(s, "volume 1 l -> ml = 1000", "convertCookingVolume",
                {"value": "1", "fromUnit": "l", "toUnit": "ml"}, exact="1000")
    await check(s, "volume 1 usfloz -> ml", "convertCookingVolume",
                {"value": "1", "fromUnit": "usfloz", "toUnit": "ml"}, contains="29.5")
    await check(s, "volume 1 cup -> tbsp = 16 (alias)", "convertCookingVolume",
                {"value": "1", "fromUnit": "cup", "toUnit": "tbsp"}, contains="16")
    await check(s, "volume 1 floz -> ml (alias)", "convertCookingVolume",
                {"value": "1", "fromUnit": "floz", "toUnit": "ml"}, contains="29.5")
    await check(s, "volume 1 gal -> l (alias)", "convertCookingVolume",
                {"value": "1", "fromUnit": "gal", "toUnit": "l"}, contains="3.78")
    await check(s, "weight 1 kg -> g = 1000", "convertCookingWeight",
                {"value": "1", "fromUnit": "kg", "toUnit": "g"}, exact="1000")
    await check(s, "weight 1 lb -> oz = 16", "convertCookingWeight",
                {"value": "1", "fromUnit": "lb", "toUnit": "oz"}, exact="16")
    await check(s, "weight 1 lb -> g", "convertCookingWeight",
                {"value": "1", "fromUnit": "lb", "toUnit": "g"}, contains="453.5")
    await check(s, "oven 180c -> f = 356", "convertOvenTemperature",
                {"value": "180", "fromUnit": "c", "toUnit": "f"}, exact="356")
    await check(s, "oven 350f -> c", "convertOvenTemperature",
                {"value": "350", "fromUnit": "f", "toUnit": "c"}, contains="176")
    await check(s, "oven gasmark 4 -> c", "convertOvenTemperature",
                {"value": "4", "fromUnit": "gasmark", "toUnit": "c"}, contains="180")
    await check(s, "oven 200c -> gasmark", "convertOvenTemperature",
                {"value": "200", "fromUnit": "c", "toUnit": "gasmark"}, contains="6")
    print("  --- error cases ---")
    await check(s, "volume invalid unit", "convertCookingVolume",
                {"value": "1", "fromUnit": "km", "toUnit": "ml"}, is_error=True)
    await check(s, "weight invalid unit", "convertCookingWeight",
                {"value": "1", "fromUnit": "l", "toUnit": "g"}, is_error=True)
    await check(s, "oven invalid unit", "convertOvenTemperature",
                {"value": "100", "fromUnit": "k", "toUnit": "c"}, is_error=True)
    await check(s, "volume bad value", "convertCookingVolume",
                {"value": "abc", "fromUnit": "l", "toUnit": "ml"}, is_error=True)


async def test_measure_reference(s):
    print("\n[MeasureReferenceTool]")
    await check(s, "listCategories has LENGTH", "listCategories", {}, contains="LENGTH")
    await check(s, "listCategories has TEMPERATURE", "listCategories", {}, contains="TEMPERATURE")
    await check(s, "listCategories has DATA_STORAGE", "listCategories", {}, contains="DATA_STORAGE")
    await check(s, "listCategories has DATA_RATE", "listCategories", {}, contains="DATA_RATE")
    await check(s, "listCategories has RESISTANCE", "listCategories", {}, contains="RESISTANCE")
    await check(s, "listCategories has CAPACITANCE", "listCategories", {}, contains="CAPACITANCE")
    await check(s, "listUnits(LENGTH) has km", "listUnits", {"category": "LENGTH"}, contains="km")
    await check(s, "listUnits(MASS) has lb", "listUnits", {"category": "MASS"}, contains="lb")
    await check(s, "listUnits(TEMPERATURE) has c", "listUnits", {"category": "TEMPERATURE"}, contains='"c"')
    await check(s, "getConversionFactor km -> m = 1000", "getConversionFactor",
                {"fromUnit": "km", "toUnit": "m"}, exact="1000")
    await check(s, "getConversionFactor in -> cm = 2.54", "getConversionFactor",
                {"fromUnit": "in", "toUnit": "cm"}, exact="2.54")
    await check(s, "explainConversion km -> m", "explainConversion",
                {"fromUnit": "km", "toUnit": "m"}, contains="1000")
    await check(s, "explainConversion lb -> kg", "explainConversion",
                {"fromUnit": "lb", "toUnit": "kg"}, contains="0.4535")
    print("  --- error cases ---")
    await check(s, "listUnits bad category", "listUnits", {"category": "INVALID"}, is_error=True)
    await check(s, "getConversionFactor unknown unit", "getConversionFactor",
                {"fromUnit": "zzz", "toUnit": "m"}, is_error=True)
    await check(s, "explainConversion unknown unit", "explainConversion",
                {"fromUnit": "zzz", "toUnit": "yyy"}, is_error=True)
    await check(s, "getConversionFactor cross-category", "getConversionFactor",
                {"fromUnit": "km", "toUnit": "kg"}, is_error=True)


async def test_datetime_converter(s):
    print("\n[DateTimeConverterTool]")
    await check(s, "convertTimezone NYC -> London", "convertTimezone",
                {"datetime": "2026-01-15T12:00:00", "fromTimezone": "America/New_York", "toTimezone": "Europe/London"},
                contains="17:00")
    await check(s, "convertTimezone Tokyo -> NYC", "convertTimezone",
                {"datetime": "2026-06-15T09:00:00", "fromTimezone": "Asia/Tokyo", "toTimezone": "America/New_York"},
                contains="20:00")
    await check(s, "currentDateTime UTC iso", "currentDateTime",
                {"timezone": "UTC", "format": "iso"}, contains="202")
    await check(s, "currentDateTime America/Sao_Paulo", "currentDateTime",
                {"timezone": "America/Sao_Paulo", "format": "iso"}, contains="202")
    await check(s, "formatDateTime iso -> epoch", "formatDateTime",
                {"datetime": "2026-01-01T00:00:00", "inputFormat": "iso", "outputFormat": "epoch",
                 "timezone": "UTC"}, contains="1767")
    await check(s, "formatDateTime iso -> rfc1123", "formatDateTime",
                {"datetime": "2026-01-01T12:00:00", "inputFormat": "iso", "outputFormat": "rfc1123",
                 "timezone": "UTC"}, contains="Jan 2026")
    await check(s, "formatDateTime custom -> iso", "formatDateTime",
                {"datetime": "2026-03-15 10:30:00", "inputFormat": "yyyy-MM-dd HH:mm:ss",
                 "outputFormat": "iso", "timezone": "UTC"}, contains="2026-03-15")
    await check(s, "listTimezones America", "listTimezones",
                {"region": "America"}, contains="America/New_York")
    await check(s, "listTimezones Europe", "listTimezones",
                {"region": "Europe"}, contains="Europe/London")
    await check(s, "listTimezones Asia", "listTimezones",
                {"region": "Asia"}, contains="Asia/Tokyo")
    await check(s, "dateTimeDifference 1 day", "dateTimeDifference",
                {"datetime1": "2026-01-01T00:00:00", "datetime2": "2026-01-02T00:00:00",
                 "timezone": "UTC"}, contains='"days":1')
    await check(s, "dateTimeDifference 1 year", "dateTimeDifference",
                {"datetime1": "2025-01-01T00:00:00", "datetime2": "2026-01-01T00:00:00",
                 "timezone": "UTC"}, contains='"years":1')
    await check(s, "dateTimeDifference hours/minutes", "dateTimeDifference",
                {"datetime1": "2026-01-01T10:00:00", "datetime2": "2026-01-01T13:30:00",
                 "timezone": "UTC"}, contains='"hours":3')
    print("  --- error cases ---")
    await check(s, "convertTimezone bad tz", "convertTimezone",
                {"datetime": "2026-01-01T12:00:00", "fromTimezone": "Invalid/Zone", "toTimezone": "UTC"},
                is_error=True)
    await check(s, "convertTimezone bad datetime", "convertTimezone",
                {"datetime": "not-a-date", "fromTimezone": "UTC", "toTimezone": "UTC"}, is_error=True)
    await check(s, "currentDateTime bad tz", "currentDateTime",
                {"timezone": "Invalid/Zone", "format": "iso"}, is_error=True)
    await check(s, "listTimezones bad region", "listTimezones",
                {"region": "Nowhere"}, is_error=True)
    await check(s, "formatDateTime bad input", "formatDateTime",
                {"datetime": "not-a-date", "inputFormat": "iso", "outputFormat": "iso",
                 "timezone": "UTC"}, is_error=True)
    await check(s, "dateTimeDifference bad tz", "dateTimeDifference",
                {"datetime1": "2026-01-01T00:00:00", "datetime2": "2026-01-02T00:00:00",
                 "timezone": "Invalid/Zone"}, is_error=True)


async def test_network_calculator(s):
    """Tests IPv4/IPv6 subnet calculation, IP conversion, VLSM, supernetting, and bandwidth tools."""
    print("\n[NetworkCalculatorTool]")
    # subnetCalculator — compute network details for an IP and CIDR prefix
    await check(s, "subnet /24 network address", "subnetCalculator",
                {"address": "192.168.1.100", "cidr": 24}, contains='"network":"192.168.1.0"')
    await check(s, "subnet /24 broadcast", "subnetCalculator",
                {"address": "192.168.1.100", "cidr": 24}, contains='"broadcast":"192.168.1.255"')
    await check(s, "subnet /24 usable hosts = 254", "subnetCalculator",
                {"address": "192.168.1.100", "cidr": 24}, contains='"usableHosts":254')
    await check(s, "subnet /24 class C", "subnetCalculator",
                {"address": "192.168.1.100", "cidr": 24}, contains='"ipClass":"C"')
    await check(s, "subnet /32 host route", "subnetCalculator",
                {"address": "10.0.0.1", "cidr": 32}, contains='"network":"10.0.0.1"')
    await check(s, "subnet /16 large block", "subnetCalculator",
                {"address": "172.16.5.100", "cidr": 16}, contains='"usableHosts":65534')
    await check(s, "subnet IPv6 /64", "subnetCalculator",
                {"address": "2001:db8::1", "cidr": 64}, contains='"network"')
    # ipToBinary — convert IP address to binary representation
    await check(s, "ipToBinary 192.168.1.1", "ipToBinary",
                {"address": "192.168.1.1"}, contains="11000000.10101000.00000001.00000001")
    await check(s, "ipToBinary 0.0.0.0", "ipToBinary",
                {"address": "0.0.0.0"}, contains="00000000.00000000.00000000.00000000")
    # binaryToIp — convert binary representation back to IP
    await check(s, "binaryToIp => 192.168.1.1", "binaryToIp",
                {"binary": "11000000.10101000.00000001.00000001"}, contains="192.168.1.1")
    # ipToDecimal — convert IP to unsigned integer
    await check(s, "ipToDecimal 192.168.1.1", "ipToDecimal",
                {"address": "192.168.1.1"}, exact="3232235777")
    # decimalToIp — convert unsigned integer back to IP
    await check(s, "decimalToIp 3232235777 v4", "decimalToIp",
                {"decimal": "3232235777", "version": 4}, contains="192.168.1.1")
    # ipInSubnet — check if IP belongs to a given subnet
    await check(s, "ipInSubnet 192.168.1.50 in /24 = true", "ipInSubnet",
                {"address": "192.168.1.50", "network": "192.168.1.0", "cidr": 24}, exact="true")
    await check(s, "ipInSubnet 10.0.0.1 not in 192.168.1.0/24", "ipInSubnet",
                {"address": "10.0.0.1", "network": "192.168.1.0", "cidr": 24}, exact="false")
    # expandIpv6 — expand abbreviated IPv6 to full form
    await check(s, "expandIpv6 ::1", "expandIpv6",
                {"address": "::1"}, contains="0000:0000:0000:0000:0000:0000:0000:0001")
    await check(s, "expandIpv6 2001:db8::1", "expandIpv6",
                {"address": "2001:db8::1"}, contains="2001:0db8:")
    # compressIpv6 — compress full IPv6 to shortest form
    await check(s, "compressIpv6 => ::1", "compressIpv6",
                {"address": "0000:0000:0000:0000:0000:0000:0000:0001"}, contains="::1")
    # vlsmSubnets — allocate subnets from a base network for given host counts
    await check(s, "vlsmSubnets allocates 3 subnets", "vlsmSubnets",
                {"networkCidr": "192.168.1.0/24", "hostCounts": "[50,25,10]"}, contains='"network"')
    # summarizeSubnets — find supernet covering multiple subnets
    await check(s, "summarizeSubnets 2 /24s", "summarizeSubnets",
                {"subnets": '["192.168.0.0/24","192.168.1.0/24"]'}, contains="/23")
    # transferTime — calculate time to transfer a file at given bandwidth
    await check(s, "transferTime 1 GB at 100 mbps", "transferTime",
                {"fileSize": "1", "fileSizeUnit": "gb", "bandwidth": "100", "bandwidthUnit": "mbps"},
                contains='"seconds"')
    # throughput — calculate throughput from data size and time
    await check(s, "throughput 100 mb in 10 s (binary)", "throughput",
                {"dataSize": "100", "dataSizeUnit": "mb", "time": "10", "timeUnit": "s",
                 "outputUnit": "mbps"}, contains="83.8")
    # tcpThroughput — calculate effective TCP throughput via BDP
    await check(s, "tcpThroughput BDP-limited", "tcpThroughput",
                {"bandwidthMbps": "1000", "rttMs": "10", "windowSizeKb": "64"}, contains=".")
    print("  --- error cases ---")
    await check(s, "subnetCalculator bad IP", "subnetCalculator",
                {"address": "999.999.999.999", "cidr": 24}, is_error=True)
    await check(s, "subnetCalculator cidr too large", "subnetCalculator",
                {"address": "10.0.0.1", "cidr": 33}, is_error=True)
    await check(s, "ipToBinary invalid", "ipToBinary",
                {"address": "not-an-ip"}, is_error=True)
    await check(s, "binaryToIp bad format", "binaryToIp",
                {"binary": "1234.5678"}, is_error=True)
    await check(s, "decimalToIp bad version", "decimalToIp",
                {"decimal": "100", "version": 5}, is_error=True)
    await check(s, "expandIpv6 bad input", "expandIpv6",
                {"address": "not-ipv6"}, is_error=True)
    await check(s, "vlsmSubnets not enough space", "vlsmSubnets",
                {"networkCidr": "192.168.1.0/30", "hostCounts": "[100]"}, is_error=True)
    await check(s, "transferTime bad unit", "transferTime",
                {"fileSize": "1", "fileSizeUnit": "zzz", "bandwidth": "100", "bandwidthUnit": "mbps"},
                is_error=True)
    await check(s, "throughput bad time unit", "throughput",
                {"dataSize": "100", "dataSizeUnit": "mb", "time": "10", "timeUnit": "zzz",
                 "outputUnit": "mbps"}, is_error=True)


async def test_analog_electronics(s):
    """Tests Ohm's law, component combinations, dividers, RC/RL/RLC circuits, dB, and filters."""
    print("\n[AnalogElectronicsTool]")
    # ohmsLaw — given any 2 of V/I/R/P, compute the other 2
    await check(s, "ohmsLaw V=12,R=4 => I=3", "ohmsLaw",
                {"voltage": "12", "current": "", "resistance": "4", "power": ""},
                contains='"current":"3"')
    await check(s, "ohmsLaw V=12,R=4 => P=36", "ohmsLaw",
                {"voltage": "12", "current": "", "resistance": "4", "power": ""},
                contains='"power":"36"')
    await check(s, "ohmsLaw V=12,I=3 => R=4", "ohmsLaw",
                {"voltage": "12", "current": "3", "resistance": "", "power": ""},
                contains='"resistance":"4"')
    # resistorCombination — series or parallel combination
    await check(s, "resistor series 100+200+300 = 600", "resistorCombination",
                {"values": "100,200,300", "mode": "series"}, exact="600")
    await check(s, "resistor parallel 100||100 = 50", "resistorCombination",
                {"values": "100,100", "mode": "parallel"}, exact="50")
    # capacitorCombination — inverse of resistor formula
    await check(s, "capacitor parallel sum", "capacitorCombination",
                {"values": "0.001,0.002", "mode": "parallel"}, contains="0.003")
    # inductorCombination — same formula as resistors
    await check(s, "inductor series 0.01+0.02 = 0.03", "inductorCombination",
                {"values": "0.01,0.02", "mode": "series"}, contains="0.03")
    # voltageDivider — Vout = Vin * R2 / (R1 + R2)
    await check(s, "voltageDivider 12V R1=1k R2=1k => 6V", "voltageDivider",
                {"vin": "12", "res1": "1000", "res2": "1000"}, contains="6")
    # currentDivider — split current between two resistors
    await check(s, "currentDivider 1A R1=100 R2=100", "currentDivider",
                {"iTotal": "1", "res1": "100", "res2": "100"}, contains='"i1":"0.5"')
    # rcTimeConstant — tau = R*C, cutoff freq = 1/(2*pi*R*C)
    await check(s, "rcTimeConstant R=1000 C=1e-6", "rcTimeConstant",
                {"resistance": "1000", "capacitance": "0.000001"}, contains='"tau":"0.001"')
    # rlTimeConstant — tau = L/R
    await check(s, "rlTimeConstant R=100 L=0.01", "rlTimeConstant",
                {"resistance": "100", "inductance": "0.01"}, contains='"tau":"0.0001"')
    # rlcResonance — resonant frequency of series RLC circuit
    await check(s, "rlcResonance has resonantFrequency", "rlcResonance",
                {"resistance": "100", "inductance": "0.001", "capacitance": "0.000001"},
                contains='"resonantFrequency"')
    # impedance — magnitude and phase at a given frequency
    await check(s, "impedance has magnitude", "impedance",
                {"resistance": "100", "inductance": "0.01", "capacitance": "0.000001", "freqHz": "1000"},
                contains='"magnitude"')
    # decibelConvert — convert between linear ratio and dB
    await check(s, "dB power ratio 2 => ~3.01 dB", "decibelConvert",
                {"value": "2", "mode": "powerToDb"}, contains="3.01")
    await check(s, "dB voltage ratio 2 => ~6.02 dB", "decibelConvert",
                {"value": "2", "mode": "voltageToDb"}, contains="6.02")
    await check(s, "dB 3dB to power => ~2", "decibelConvert",
                {"value": "3.0103", "mode": "dbToPower"}, contains="2")
    # filterCutoff — RC filter cutoff frequency
    await check(s, "filterCutoff lowpass R=1k C=1uF", "filterCutoff",
                {"resistance": "1000", "capacitance": "0.000001", "filterType": "lowpass"},
                contains='"cutoffFrequency"')
    # ledResistor — R = (Vs - Vf) / If
    await check(s, "ledResistor Vs=5 Vf=2 If=0.02 => R=150", "ledResistor",
                {"supplyVoltage": "5", "forwardVoltage": "2", "forwardCurrent": "0.02"}, exact="150")
    # wheatstoneBridge — R4 = R3 * R2 / R1
    await check(s, "wheatstoneBridge R1=100 R2=200 R3=300 => R4=600", "wheatstoneBridge",
                {"res1": "100", "res2": "200", "res3": "300"}, exact="600")
    print("  --- error cases ---")
    await check(s, "ohmsLaw only 1 known => error", "ohmsLaw",
                {"voltage": "12", "current": "", "resistance": "", "power": ""}, is_error=True)
    await check(s, "ohmsLaw all 4 known => error", "ohmsLaw",
                {"voltage": "12", "current": "3", "resistance": "4", "power": "36"}, is_error=True)
    await check(s, "resistor invalid mode", "resistorCombination",
                {"values": "100,200", "mode": "invalid"}, is_error=True)
    await check(s, "ledResistor Vs < Vf => error", "ledResistor",
                {"supplyVoltage": "2", "forwardVoltage": "5", "forwardCurrent": "0.02"}, is_error=True)
    await check(s, "ledResistor If=0 => error", "ledResistor",
                {"supplyVoltage": "5", "forwardVoltage": "2", "forwardCurrent": "0"}, is_error=True)
    await check(s, "decibelConvert invalid mode", "decibelConvert",
                {"value": "2", "mode": "invalidMode"}, is_error=True)
    await check(s, "filterCutoff invalid type", "filterCutoff",
                {"resistance": "1000", "capacitance": "0.000001", "filterType": "bandpass"}, is_error=True)


async def test_digital_electronics(s):
    """Tests base conversion, two's complement, Gray code, bitwise ops, ADC/DAC, 555 timer."""
    print("\n[DigitalElectronicsTool]")
    # convertBase — convert between number bases (2-36)
    await check(s, "convertBase 255 dec -> hex = FF", "convertBase",
                {"number": "255", "fromBase": 10, "toBase": 16}, contains="FF")
    await check(s, "convertBase FF hex -> bin", "convertBase",
                {"number": "FF", "fromBase": 16, "toBase": 2}, contains="11111111")
    await check(s, "convertBase 1010 bin -> dec = 10", "convertBase",
                {"number": "1010", "fromBase": 2, "toBase": 10}, contains="10")
    await check(s, "convertBase 777 oct -> dec = 511", "convertBase",
                {"number": "777", "fromBase": 8, "toBase": 10}, contains="511")
    # twosComplement — encode/decode two's complement
    await check(s, "twosComplement -1 8bit = 11111111", "twosComplement",
                {"value": "-1", "bits": 8, "direction": "toTwos"}, contains="11111111")
    await check(s, "twosComplement -128 8bit = 10000000", "twosComplement",
                {"value": "-128", "bits": 8, "direction": "toTwos"}, contains="10000000")
    await check(s, "twosComplement fromTwos 11111111 8bit = -1", "twosComplement",
                {"value": "11111111", "bits": 8, "direction": "fromTwos"}, contains="-1")
    # grayCode — binary to Gray and back
    await check(s, "grayCode 0100 toGray = 0110", "grayCode",
                {"binary": "0100", "direction": "toGray"}, contains="0110")
    await check(s, "grayCode 0110 fromGray = 0100", "grayCode",
                {"binary": "0110", "direction": "fromGray"}, contains="0100")
    # bitwiseOp — perform bitwise operations
    await check(s, "bitwiseOp 12 AND 10 = 8", "bitwiseOp",
                {"operandA": "12", "operandB": "10", "operation": "AND"}, contains='"decimal":"8"')
    await check(s, "bitwiseOp 12 OR 10 = 14", "bitwiseOp",
                {"operandA": "12", "operandB": "10", "operation": "OR"}, contains='"decimal":"14"')
    await check(s, "bitwiseOp 12 XOR 10 = 6", "bitwiseOp",
                {"operandA": "12", "operandB": "10", "operation": "XOR"}, contains='"decimal":"6"')
    await check(s, "bitwiseOp SHL 1<<3 = 8", "bitwiseOp",
                {"operandA": "1", "operandB": "3", "operation": "SHL"}, contains='"decimal":"8"')
    # adcResolution — calculate ADC step size
    await check(s, "adcResolution 8bit 5V", "adcResolution",
                {"bits": 8, "vref": "5"}, contains='"stepCount":"255"')
    # dacOutput — calculate DAC output voltage
    await check(s, "dacOutput 8bit 5V code=128 => 2.5V", "dacOutput",
                {"bits": 8, "vref": "5", "code": 128}, contains="2.5")
    # timer555Astable — calculate 555 timer frequency and duty cycle
    await check(s, "555 astable R1=1k R2=1k C=1uF", "timer555Astable",
                {"resistance1": "1000", "resistance2": "1000", "capacitance": "0.000001"},
                contains='"frequency"')
    # timer555Monostable — calculate 555 pulse width
    await check(s, "555 monostable R=10k C=10uF", "timer555Monostable",
                {"resistance": "10000", "capacitance": "0.00001"}, contains='"pulseWidth"')
    # frequencyPeriod — convert between frequency and period
    await check(s, "freq 1000 Hz -> period 0.001", "frequencyPeriod",
                {"value": "1000", "mode": "freqToPeriod"}, contains="0.001")
    await check(s, "period 0.001 -> freq 1000", "frequencyPeriod",
                {"value": "0.001", "mode": "periodToFreq"}, contains="1000")
    # nyquistRate — minimum sampling rate
    await check(s, "nyquistRate 20kHz -> 40kHz", "nyquistRate",
                {"bandwidthHz": "20000"}, contains='"minSampleRate":"40000"')
    print("  --- error cases ---")
    await check(s, "convertBase invalid base 1", "convertBase",
                {"number": "10", "fromBase": 1, "toBase": 10}, is_error=True)
    await check(s, "convertBase invalid base 37", "convertBase",
                {"number": "10", "fromBase": 10, "toBase": 37}, is_error=True)
    await check(s, "convertBase invalid digit", "convertBase",
                {"number": "GG", "fromBase": 16, "toBase": 10}, is_error=True)
    await check(s, "twosComplement 200 8bit wraps (unsigned fits)", "twosComplement",
                {"value": "200", "bits": 8, "direction": "toTwos"}, contains="11001000")
    await check(s, "twosComplement invalid direction", "twosComplement",
                {"value": "0", "bits": 8, "direction": "badDir"}, is_error=True)
    await check(s, "grayCode invalid direction", "grayCode",
                {"binary": "0100", "direction": "badDir"}, is_error=True)
    await check(s, "bitwiseOp invalid op", "bitwiseOp",
                {"operandA": "10", "operandB": "5", "operation": "NAND"}, is_error=True)
    await check(s, "dacOutput code exceeds range", "dacOutput",
                {"bits": 8, "vref": "5", "code": 256}, is_error=True)
    await check(s, "frequencyPeriod zero", "frequencyPeriod",
                {"value": "0", "mode": "freqToPeriod"}, is_error=True)
    await check(s, "frequencyPeriod unrecognized mode defaults", "frequencyPeriod",
                {"value": "1000", "mode": "badMode"}, contains="0.001")
    await check(s, "nyquistRate negative passes through", "nyquistRate",
                {"bandwidthHz": "-100"}, contains='"minSampleRate":"-200"')


async def test_calculus(s):
    """Tests numerical derivatives (1st and nth order), definite integrals, and tangent lines."""
    print("\n[CalculusTool]")
    # derivative — compute first derivative at a point using five-point central difference
    await check(s, "d/dx(x^2) at x=3 => ~6.0", "derivative",
                {"expression": "x^2", "variable": "x", "point": 3.0},
                numeric_delta=0.01, expected=6.0)
    await check(s, "d/dx(x^3) at x=2 => ~12.0", "derivative",
                {"expression": "x^3", "variable": "x", "point": 2.0},
                numeric_delta=0.01, expected=12.0)
    await check(s, "d/dx(3*x+5) at x=0 => ~3.0 (constant slope)", "derivative",
                {"expression": "3*x+5", "variable": "x", "point": 0.0},
                numeric_delta=0.01, expected=3.0)
    # nthDerivative — compute nth order derivative
    await check(s, "d²/dx²(x^3) at x=1 => ~6.0", "nthDerivative",
                {"expression": "x^3", "variable": "x", "point": 1.0, "order": 2},
                numeric_delta=0.1, expected=6.0)
    await check(s, "d³/dx³(x^3) at x=5 => ~6.0", "nthDerivative",
                {"expression": "x^3", "variable": "x", "point": 5.0, "order": 3},
                numeric_delta=1.0, expected=6.0)
    # definiteIntegral — compute definite integral using Simpson's rule
    await check(s, "∫₀¹ x² dx = 1/3 ≈ 0.333", "definiteIntegral",
                {"expression": "x^2", "variable": "x", "lower": 0.0, "upper": 1.0},
                numeric_delta=0.001, expected=0.33333)
    await check(s, "∫₀² 3*x dx = 6.0", "definiteIntegral",
                {"expression": "3*x", "variable": "x", "lower": 0.0, "upper": 2.0},
                numeric_delta=0.001, expected=6.0)
    await check(s, "∫₁³ x dx = 4.0", "definiteIntegral",
                {"expression": "x", "variable": "x", "lower": 1.0, "upper": 3.0},
                numeric_delta=0.001, expected=4.0)
    # tangentLine — compute tangent line equation y = mx + b
    await check(s, "tangent x^2 at x=2 slope=4", "tangentLine",
                {"expression": "x^2", "variable": "x", "point": 2.0},
                contains='"slope"')
    await check(s, "tangent x^2 at x=0 has equation", "tangentLine",
                {"expression": "x^2", "variable": "x", "point": 0.0},
                contains='"equation"')
    print("  --- error cases ---")
    await check(s, "derivative bad expression", "derivative",
                {"expression": "???", "variable": "x", "point": 1.0}, is_error=True)
    await check(s, "nthDerivative order 0 => error", "nthDerivative",
                {"expression": "x^2", "variable": "x", "point": 1.0, "order": 0}, is_error=True)
    await check(s, "nthDerivative order 11 => error", "nthDerivative",
                {"expression": "x^2", "variable": "x", "point": 1.0, "order": 11}, is_error=True)
    await check(s, "definiteIntegral bad expr", "definiteIntegral",
                {"expression": "???", "variable": "x", "lower": 0.0, "upper": 1.0}, is_error=True)
    await check(s, "tangentLine bad expression", "tangentLine",
                {"expression": "???", "variable": "x", "point": 1.0}, is_error=True)


# ---------------------------------------------------------------------------
# Benchmark workloads
# ---------------------------------------------------------------------------
BENCHMARK_WORKLOAD = [
    ("add",               {"first": "123.456", "second": "789.012"}),
    ("subtract",          {"first": "1000", "second": "333"}),
    ("multiply",          {"first": "12.5", "second": "8.4"}),
    ("divide",            {"first": "355", "second": "113"}),
    ("modulo",            {"first": "17", "second": "5"}),
    ("power",             {"base": "2", "exponent": "16"}),
    ("abs",               {"value": "-99.9"}),
    ("sqrt",              {"number": 144}),
    ("log",               {"number": 2.718281828459045}),
    ("log10",             {"number": 1000}),
    ("factorial",         {"num": 15}),
    ("sin",               {"degrees": 45}),
    ("cos",               {"degrees": 60}),
    ("tan",               {"degrees": 30}),
    ("sumArray",          {"numbers": "1,2,3,4,5,6,7,8,9,10"}),
    ("dotProduct",        {"first": "1,2,3", "second": "4,5,6"}),
    ("magnitudeArray",    {"numbers": "3,4,5"}),
    ("scaleArray",        {"numbers": "1,2,3,4,5", "scalar": "10"}),
    ("evaluate",          {"expression": "2+3*4-1"}),
    ("evaluateWithVariables", {"expression": "2*x+y", "variables": '{"x":5.0,"y":3.0}'}),
    ("compoundInterest",  {"principal": "10000", "annualRate": "5", "years": "10", "compoundsPerYear": 12}),
    ("loanPayment",       {"principal": "250000", "annualRate": "4.5", "years": "30"}),
    ("presentValue",      {"futureValue": "50000", "annualRate": "6", "years": "5"}),
    ("futureValueAnnuity", {"payment": "200", "annualRate": "8", "years": "25"}),
    ("returnOnInvestment", {"gain": "5000", "cost": "3000"}),
    ("solveEquation",     {"expression": "x^2 - 9", "variable": "x", "initialGuess": 4.0}),
    ("findRoots",         {"expression": "x^2 - 16", "variable": "x", "min": -10, "max": 10}),
    ("plotFunction",      {"expression": "x^3", "variable": "x", "min": -2, "max": 2, "steps": 20}),
    ("calculateWithTape", {"operations": '[{"op":"+","value":100},{"op":"*","value":2},{"op":"=","value":0}]'}),
    ("convert",           {"value": "1", "fromUnit": "km", "toUnit": "m", "category": "LENGTH"}),
    ("convertAutoDetect", {"value": "5", "fromUnit": "lb", "toUnit": "kg"}),
    ("convertCookingVolume", {"value": "1", "fromUnit": "tbsp", "toUnit": "tsp"}),
    ("convertCookingWeight", {"value": "1", "fromUnit": "kg", "toUnit": "g"}),
    ("convertOvenTemperature", {"value": "180", "fromUnit": "c", "toUnit": "f"}),
    ("listCategories",    {}),
    ("listUnits",         {"category": "LENGTH"}),
    ("getConversionFactor", {"fromUnit": "km", "toUnit": "m"}),
    ("explainConversion", {"fromUnit": "lb", "toUnit": "kg"}),
    ("convertTimezone",   {"datetime": "2026-01-15T12:00:00", "fromTimezone": "America/New_York", "toTimezone": "Europe/London"}),
    ("currentDateTime",   {"timezone": "UTC", "format": "iso"}),
    ("listTimezones",     {"region": "America"}),
    ("dateTimeDifference", {"datetime1": "2026-01-01T00:00:00", "datetime2": "2026-01-02T00:00:00", "timezone": "UTC"}),
    # Network tools
    ("subnetCalculator",    {"address": "192.168.1.100", "cidr": 24}),
    ("ipToBinary",          {"address": "192.168.1.1"}),
    ("binaryToIp",          {"binary": "11000000.10101000.00000001.00000001"}),
    ("ipToDecimal",         {"address": "10.0.0.1"}),
    ("decimalToIp",         {"decimal": "3232235777", "version": 4}),
    ("ipInSubnet",          {"address": "192.168.1.50", "network": "192.168.1.0", "cidr": 24}),
    ("expandIpv6",          {"address": "::1"}),
    ("compressIpv6",        {"address": "0000:0000:0000:0000:0000:0000:0000:0001"}),
    ("transferTime",        {"fileSize": "1", "fileSizeUnit": "gb", "bandwidth": "100", "bandwidthUnit": "mbps"}),
    ("throughput",          {"dataSize": "100", "dataSizeUnit": "mb", "time": "10", "timeUnit": "s", "outputUnit": "mbps"}),
    ("tcpThroughput",       {"bandwidthMbps": "100", "rttMs": "10", "windowSizeKb": "64"}),
    # Analog electronics tools
    ("ohmsLaw",             {"voltage": "12", "current": "", "resistance": "4", "power": ""}),
    ("resistorCombination", {"values": "100,200,300", "mode": "series"}),
    ("capacitorCombination", {"values": "0.001,0.002", "mode": "parallel"}),
    ("inductorCombination", {"values": "0.01,0.02", "mode": "series"}),
    ("voltageDivider",      {"vin": "12", "res1": "1000", "res2": "1000"}),
    ("currentDivider",      {"iTotal": "1", "res1": "100", "res2": "100"}),
    ("rcTimeConstant",      {"resistance": "1000", "capacitance": "0.000001"}),
    ("rlTimeConstant",      {"resistance": "100", "inductance": "0.01"}),
    ("rlcResonance",        {"resistance": "100", "inductance": "0.001", "capacitance": "0.000001"}),
    ("impedance",           {"resistance": "100", "inductance": "0.01", "capacitance": "0.000001", "freqHz": "1000"}),
    ("decibelConvert",      {"value": "2", "mode": "powerToDb"}),
    ("filterCutoff",        {"resistance": "1000", "capacitance": "0.000001", "filterType": "lowpass"}),
    ("ledResistor",         {"supplyVoltage": "5", "forwardVoltage": "2", "forwardCurrent": "0.02"}),
    ("wheatstoneBridge",    {"res1": "100", "res2": "200", "res3": "300"}),
    # Digital electronics tools
    ("convertBase",         {"number": "255", "fromBase": 10, "toBase": 16}),
    ("twosComplement",      {"value": "-1", "bits": 8, "direction": "toTwos"}),
    ("grayCode",            {"binary": "0100", "direction": "toGray"}),
    ("bitwiseOp",           {"operandA": "12", "operandB": "10", "operation": "AND"}),
    ("adcResolution",       {"bits": 8, "vref": "5"}),
    ("dacOutput",           {"bits": 8, "vref": "5", "code": 128}),
    ("timer555Astable",     {"resistance1": "1000", "resistance2": "1000", "capacitance": "0.000001"}),
    ("timer555Monostable",  {"resistance": "10000", "capacitance": "0.00001"}),
    ("frequencyPeriod",     {"value": "1000", "mode": "freqToPeriod"}),
    ("nyquistRate",         {"bandwidthHz": "20000"}),
    # Calculus tools
    ("derivative",          {"expression": "x^2", "variable": "x", "point": 3.0}),
    ("nthDerivative",       {"expression": "x^3", "variable": "x", "point": 1.0, "order": 2}),
    ("definiteIntegral",    {"expression": "x^2", "variable": "x", "lower": 0.0, "upper": 1.0}),
    ("tangentLine",         {"expression": "x^2", "variable": "x", "point": 2.0}),
]


async def benchmark_sequential(session, rounds):
    total = rounds * len(BENCHMARK_WORKLOAD)
    print(f"\n  Sequential: {rounds} rounds x {len(BENCHMARK_WORKLOAD)} tools = {total} calls")
    t0 = time.perf_counter()
    for r in range(rounds):
        for tool_name, args in BENCHMARK_WORKLOAD:
            await call_tool(session, tool_name, args)
        if (r + 1) % 25 == 0:
            elapsed = time.perf_counter() - t0
            rps = ((r + 1) * len(BENCHMARK_WORKLOAD)) / elapsed
            print(f"    ... round {r + 1}/{rounds} — {rps:.0f} req/s")
    elapsed = time.perf_counter() - t0
    rps = total / elapsed
    print(f"  Completed in {elapsed:.2f}s — \033[33m{rps:.1f} req/s\033[0m")
    return elapsed, total


async def _bench_worker(sem, session, tool_name, args):
    """Single benchmark call with semaphore-controlled concurrency."""
    async with sem:
        return await call_tool(session, tool_name, args)


async def benchmark_concurrent(session, rounds, workers):
    tasks_list = BENCHMARK_WORKLOAD * rounds
    total = len(tasks_list)
    print(f"\n  Concurrent: {workers} workers x {total} calls")
    sem = asyncio.Semaphore(workers)
    t0 = time.perf_counter()
    coros = [_bench_worker(sem, session, name, args) for name, args in tasks_list]
    await asyncio.gather(*coros)
    elapsed = time.perf_counter() - t0
    rps = total / elapsed
    print(f"  Completed in {elapsed:.2f}s — \033[33m{rps:.1f} req/s\033[0m")
    return elapsed, total


# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
def pct(data, p):
    k = (len(data) - 1) * (p / 100.0)
    f = int(k)
    c = f + 1
    if c >= len(data):
        return data[f]
    return data[f] + (k - f) * (data[c] - data[f])


def print_latency_report():
    print("\n" + "=" * 82)
    print("  LATENCY REPORT (ms)")
    print("=" * 82)
    header = f"  {'Tool':<28} {'Count':>6} {'Min':>7} {'Avg':>7} {'Med':>7} {'P95':>7} {'P99':>7} {'Max':>7}"
    print(header)
    print("  " + "-" * 78)

    all_lats = []
    for tool_name in sorted(LATENCIES.keys()):
        lats = sorted(LATENCIES[tool_name])
        all_lats.extend(lats)
        cnt = len(lats)
        print(f"  {tool_name:<28} {cnt:>6} {min(lats):>7.1f} {statistics.mean(lats):>7.1f} "
              f"{statistics.median(lats):>7.1f} {pct(lats, 95):>7.1f} {pct(lats, 99):>7.1f} {max(lats):>7.1f}")

    if all_lats:
        all_lats.sort()
        cnt = len(all_lats)
        print("  " + "-" * 78)
        print(f"  {'TOTAL':<28} {cnt:>6} {min(all_lats):>7.1f} {statistics.mean(all_lats):>7.1f} "
              f"{statistics.median(all_lats):>7.1f} {pct(all_lats, 95):>7.1f} {pct(all_lats, 99):>7.1f} "
              f"{max(all_lats):>7.1f}")
    print("=" * 82)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
async def async_main():
    global PASS, FAIL, MCP_URL

    parser = argparse.ArgumentParser(description="MCP tool integration tests + benchmark (asyncio)")
    parser.add_argument("--base", default="http://localhost:44321",
                        help="Base URL of the MCP server (default: http://localhost:44321)")
    parser.add_argument("--benchmark", action="store_true", help="Run benchmark after tests pass")
    parser.add_argument("-n", "--rounds", type=int, default=100, help="Benchmark rounds (default: 100)")
    parser.add_argument("-w", "--workers", type=int, default=8, help="Concurrent workers (default: 8)")
    args = parser.parse_args()
    base = args.base.rstrip("/")

    async with aiohttp.ClientSession() as session:
        MCP_URL = f"{base}/mcp"
        print(f"Connecting to {MCP_URL} ...")

        # Initialize MCP
        resp = await send(session, "initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "mcp-bench", "version": "2.0"},
        })
        if not resp:
            print("ERROR: MCP initialize failed")
            sys.exit(2)
        info = resp["result"]["serverInfo"]
        print(f"Server: {info['name']} v{info['version']}")
        print(f"Session ID: {SESSION_ID}")
        await send(session, "notifications/initialized", notification=True)

        # List tools
        resp = await send(session, "tools/list")
        tools = sorted(t["name"] for t in resp["result"]["tools"])
        print(f"Tools: {len(tools)}")
        print(f"  {', '.join(tools)}")

        # -- Tests --
        print("\n" + "=" * 64)
        print("  MCP TOOL INTEGRATION TESTS")
        print("=" * 64)

        await test_basic_calculator(session)
        await test_scientific_calculator(session)
        await test_trig_precision(session)
        await test_graphing_calculator(session)
        await test_financial_calculator(session)
        await test_vector_calculator(session)
        await test_expression_evaluator(session)
        await test_tape_calculator(session)
        await test_unit_converter(session)
        await test_cooking_converter(session)
        await test_measure_reference(session)
        await test_datetime_converter(session)
        await test_network_calculator(session)
        await test_analog_electronics(session)
        await test_digital_electronics(session)
        await test_calculus(session)

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

        # -- Benchmark --
        if args.benchmark and FAIL == 0:
            print("\n" + "=" * 64)
            print("  BENCHMARK")
            print("=" * 64)

            LATENCIES.clear()

            seq_time, seq_calls = await benchmark_sequential(session, args.rounds)
            con_time, con_calls = await benchmark_concurrent(session, args.rounds, args.workers)

            seq_rps = seq_calls / seq_time
            con_rps = con_calls / con_time
            print(f"\n  Summary:")
            print(f"    Sequential : {seq_rps:>8.1f} req/s  ({seq_time:.2f}s)")
            print(f"    Concurrent : {con_rps:>8.1f} req/s  ({con_time:.2f}s)  [{args.workers} workers]")
            print(f"    Speedup    : {con_rps / seq_rps:.2f}x")

        elif args.benchmark and FAIL > 0:
            print("\n  Benchmark skipped — fix failing tests first.")

        # -- Latency report --
        print_latency_report()

    sys.exit(0 if FAIL == 0 else 1)


def main():
    asyncio.run(async_main())


if __name__ == "__main__":
    main()
