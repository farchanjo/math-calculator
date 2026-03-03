#!/usr/bin/env python3
"""
MCP Tool Integration Tests + Benchmark — math-calculator (asyncio)

Validates all 30 MCP tools via SSE transport using async I/O:
  - Success cases with precision assertions
  - Error cases (invalid input, domain errors, edge cases)
  - Latency metrics collected for every call
  - Concurrent benchmark with native asyncio concurrency

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
# Async MCP client — dict-based response routing with asyncio.Event
# ---------------------------------------------------------------------------
MSG_URL = None
REQ_ID = 0
ENDPOINT_READY = asyncio.Event()

# Response store: {request_id: (asyncio.Event, response_data)}
RESP_STORE: dict[int, list] = {}


async def sse_listener(session: aiohttp.ClientSession, base: str):
    """Open SSE connection, capture endpoint, route responses by ID."""
    global MSG_URL
    async with session.get(f"{base}/sse", timeout=aiohttp.ClientTimeout(total=600)) as resp:
        buffer = ""
        async for chunk in resp.content.iter_any():
            buffer += chunk.decode("utf-8")
            while "\n" in buffer:
                line, buffer = buffer.split("\n", 1)
                line = line.strip()
                if not line.startswith("data:"):
                    continue
                data = line[5:]
                if "/mcp/message?" in data:
                    MSG_URL = f"{base}{data}"
                    ENDPOINT_READY.set()
                else:
                    try:
                        parsed = json.loads(data)
                        rid = parsed.get("id")
                        if rid is not None and rid in RESP_STORE:
                            entry = RESP_STORE[rid]
                            entry[1] = parsed
                            entry[0].set()
                    except json.JSONDecodeError:
                        pass


async def send(session: aiohttp.ClientSession, method: str, params=None, notification=False):
    """Send a JSON-RPC message and optionally wait for a response."""
    global REQ_ID
    body: dict = {"jsonrpc": "2.0", "method": method}
    if not notification:
        REQ_ID += 1
        rid = REQ_ID
        body["id"] = rid
        evt = asyncio.Event()
        RESP_STORE[rid] = [evt, None]
    else:
        rid = None
    if params:
        body["params"] = params
    try:
        async with session.post(
            MSG_URL,
            json=body,
            timeout=aiohttp.ClientTimeout(total=10),
        ) as _:
            pass
    except aiohttp.ClientError:
        pass
    if notification:
        return None
    try:
        await asyncio.wait_for(evt.wait(), timeout=10)
    except asyncio.TimeoutError:
        pass
    entry = RESP_STORE.pop(rid, None)
    return entry[1] if entry else None


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
    global PASS, FAIL

    parser = argparse.ArgumentParser(description="MCP tool integration tests + benchmark (asyncio)")
    parser.add_argument("--base", default="http://localhost:44321",
                        help="Base URL of the MCP server (default: http://localhost:44321)")
    parser.add_argument("--benchmark", action="store_true", help="Run benchmark after tests pass")
    parser.add_argument("-n", "--rounds", type=int, default=100, help="Benchmark rounds (default: 100)")
    parser.add_argument("-w", "--workers", type=int, default=8, help="Concurrent workers (default: 8)")
    args = parser.parse_args()
    base = args.base.rstrip("/")

    async with aiohttp.ClientSession() as session:
        print(f"Connecting to {base}/sse ...")
        listener_task = asyncio.create_task(sse_listener(session, base))

        try:
            await asyncio.wait_for(ENDPOINT_READY.wait(), timeout=5)
        except asyncio.TimeoutError:
            print("ERROR: could not obtain SSE endpoint. Is the server running?")
            sys.exit(2)
        print(f"Session: {MSG_URL}")

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
        await send(session, "notifications/initialized", notification=True)
        await asyncio.sleep(0.3)

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

        listener_task.cancel()

    sys.exit(0 if FAIL == 0 else 1)


def main():
    asyncio.run(async_main())


if __name__ == "__main__":
    main()
