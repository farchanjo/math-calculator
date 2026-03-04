#!/usr/bin/env python3
"""
MCP Dual-Transport Integration Tests — math-calculator

Validates both MCP transports on the same server:
  1. Streamable HTTP  (POST /mcp)           — response in POST body
  2. SSE              (GET /sse + POST /mcp/message) — response via SSE stream

A representative subset of tool calls is executed on each transport
to confirm that both serve the same tool registry correctly.

Usage:
    python3 scripts/mcp_sse_test.py
    python3 scripts/mcp_sse_test.py --base http://host:port

Requires: pip install aiohttp
"""
import argparse
import asyncio
import json
import sys
import time

import aiohttp

# ---------------------------------------------------------------------------
# Formatting
# ---------------------------------------------------------------------------
OK = "\033[32m\u2713\033[0m"
KO = "\033[31m\u2717\033[0m"
DIM = "\033[90m"
RST = "\033[0m"
BOLD = "\033[1m"

PASS = 0
FAIL = 0
ERRORS: list[str] = []


# ---------------------------------------------------------------------------
# Streamable HTTP transport client
# ---------------------------------------------------------------------------
async def send_streamable(session, url, method, params=None, *,
                          notification=False, state=None):
    """Send JSON-RPC via Streamable HTTP (POST /mcp). Response in body."""
    body: dict = {"jsonrpc": "2.0", "method": method}
    if not notification:
        state["req_id"] += 1
        body["id"] = state["req_id"]
    if params:
        body["params"] = params

    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
    }
    if state.get("session_id"):
        headers["mcp-session-id"] = state["session_id"]

    async with session.post(url, json=body, headers=headers,
                            timeout=aiohttp.ClientTimeout(total=10)) as resp:
        if not state.get("session_id") and "mcp-session-id" in resp.headers:
            state["session_id"] = resp.headers["mcp-session-id"]
        if notification or resp.status == 202:
            return None
        ct = resp.headers.get("Content-Type", "")
        if "text/event-stream" in ct:
            raw = await resp.text()
            result = None
            for line in raw.splitlines():
                if line.startswith("data:"):
                    try:
                        result = json.loads(line[5:].strip())
                    except json.JSONDecodeError:
                        pass
            return result
        return await resp.json(content_type=None)


# ---------------------------------------------------------------------------
# SSE transport client — responses arrive on the SSE event stream
# ---------------------------------------------------------------------------
class SseClient:
    """Manages an SSE connection for the MCP SSE transport.

    The SSE protocol sends responses as 'message' events on the GET /sse
    stream, not in the POST response body.  This class reads events in a
    background task and resolves futures keyed by JSON-RPC request id.
    """

    def __init__(self, base_url: str):
        self.base_url = base_url
        self.msg_url: str | None = None
        self._pending: dict[int, asyncio.Future] = {}
        self._reader_task: asyncio.Task | None = None
        self._sse_resp = None
        self._req_id = 0

    async def connect(self, session: aiohttp.ClientSession):
        """Open GET /sse, parse endpoint event, start reader."""
        sse_url = f"{self.base_url}/sse"
        self._sse_resp = await session.get(
            sse_url,
            headers={"Accept": "text/event-stream"},
            timeout=aiohttp.ClientTimeout(total=None, sock_read=60),
        )
        if self._sse_resp.status != 200:
            raise RuntimeError(f"SSE connect: HTTP {self._sse_resp.status}")

        # Read until we get the endpoint event
        event_type = None
        while True:
            raw = await asyncio.wait_for(
                self._sse_resp.content.readline(), timeout=10)
            line = raw.decode("utf-8").rstrip("\r\n")
            if line.startswith("event:"):
                event_type = line[6:].strip()
            elif line.startswith("data:") and event_type == "endpoint":
                data = line[5:].strip()
                self.msg_url = (f"{self.base_url}{data}"
                                if data.startswith("/") else data)
                break

        # Start background reader for message events
        self._reader_task = asyncio.create_task(self._read_events())

    async def _read_events(self):
        """Background: read SSE events and resolve pending futures."""
        event_type = None
        try:
            async for raw_line in self._sse_resp.content:
                line = raw_line.decode("utf-8").rstrip("\r\n")
                if line.startswith("event:"):
                    event_type = line[6:].strip()
                elif line.startswith("data:"):
                    if event_type == "message":
                        try:
                            msg = json.loads(line[5:].strip())
                        except json.JSONDecodeError:
                            continue
                        rid = msg.get("id")
                        if rid is not None and rid in self._pending:
                            self._pending[rid].set_result(msg)
                    event_type = None
        except (asyncio.CancelledError, aiohttp.ClientError):
            pass

    async def send(self, session, method, params=None, *,
                   notification=False):
        """POST to message endpoint; wait for response on SSE stream."""
        body: dict = {"jsonrpc": "2.0", "method": method}
        if not notification:
            self._req_id += 1
            body["id"] = self._req_id
        if params:
            body["params"] = params

        # Register a future BEFORE posting so the reader can resolve it
        future = None
        if not notification:
            future = asyncio.get_event_loop().create_future()
            self._pending[self._req_id] = future

        await session.post(
            self.msg_url,
            json=body,
            headers={"Content-Type": "application/json"},
            timeout=aiohttp.ClientTimeout(total=10),
        )

        if notification or future is None:
            return None

        try:
            return await asyncio.wait_for(future, timeout=10)
        finally:
            self._pending.pop(self._req_id, None)

    async def close(self):
        if self._reader_task:
            self._reader_task.cancel()
            try:
                await self._reader_task
            except asyncio.CancelledError:
                pass
        if self._sse_resp:
            self._sse_resp.close()


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def text_of(resp):
    """Extract first text content from an MCP tool response."""
    if resp and "result" in resp:
        content = resp["result"].get("content", [])
        if content:
            return content[0].get("text", "")
    if resp and "error" in resp:
        return f"ERROR: {resp['error'].get('message', str(resp['error']))}"
    return "NO_RESPONSE"


async def check(send_fn, desc, tool, args, *, exact=None, contains=None):
    """Call a tool via send_fn, assert result, track pass/fail."""
    global PASS, FAIL
    t0 = time.perf_counter()
    resp = await send_fn("tools/call", {"name": tool, "arguments": args})
    elapsed = (time.perf_counter() - t0) * 1000

    txt = text_of(resp)
    ok = True
    detail = ""

    if exact is not None:
        if txt.strip() == str(exact):
            detail = txt.strip()
        else:
            ok = False
            detail = f"expected '{exact}', got '{txt.strip()[:120]}'"
    elif contains is not None:
        if contains in txt:
            detail = f"contains '{contains}'"
        else:
            ok = False
            detail = f"missing '{contains}' in: {txt[:120]}"
    else:
        detail = txt[:120]

    icon = OK if ok else KO
    ms = f"{DIM}{elapsed:6.1f}ms{RST}"
    print(f"  {icon} {ms} {desc}: {detail}")
    if ok:
        PASS += 1
    else:
        FAIL += 1
        ERRORS.append(f"  {desc}: {detail}")


# ---------------------------------------------------------------------------
# Tool test suite — representative subset across categories
# ---------------------------------------------------------------------------
async def run_tool_tests(send_fn):
    print("\n-- BasicCalculatorTool --")
    await check(send_fn, "add", "add", {"first": "3", "second": "4"}, exact="7")
    await check(send_fn, "multiply", "multiply",
                {"first": "6", "second": "7"}, exact="42")
    await check(send_fn, "evaluate", "evaluate",
                {"expression": "2^10"}, contains="1024")

    print("\n-- ScientificCalculatorTool --")
    await check(send_fn, "sqrt(144)", "sqrt", {"number": 144}, contains="12")
    await check(send_fn, "sin(30)", "sin", {"degrees": 30}, exact="0.5")
    await check(send_fn, "factorial(10)", "factorial",
                {"num": 10}, exact="3628800")

    print("\n-- FinancialCalculatorTool --")
    await check(
        send_fn, "compound interest", "compoundInterest",
        {"principal": "1000", "annualRate": "5",
         "years": "10", "compoundsPerYear": 12},
        contains="1647",
    )

    print("\n-- UnitConverterTool --")
    await check(
        send_fn, "km to miles", "convert",
        {"value": "1", "fromUnit": "km", "toUnit": "mi", "category": "LENGTH"},
        contains="0.6213",
    )
    await check(send_fn, "list categories", "listCategories", {},
                contains="LENGTH")

    print("\n-- DateTimeConverterTool --")
    await check(
        send_fn, "current UTC time", "currentDateTime",
        {"timezone": "UTC", "format": "iso"}, contains="T",
    )

    print("\n-- NetworkCalculatorTool --")
    await check(
        send_fn, "subnet /24", "subnetCalculator",
        {"address": "192.168.1.0", "cidr": 24}, contains="192.168.1.255",
    )

    print("\n-- AnalogElectronicsTool --")
    await check(
        send_fn, "ohms law V=IR", "ohmsLaw",
        {"voltage": "12", "current": "2", "resistance": "", "power": ""},
        contains="6",
    )

    print("\n-- CalculusTool --")
    await check(
        send_fn, "derivative x^2 at 3", "derivative",
        {"expression": "x^2", "variable": "x", "point": 3}, contains="6",
    )


# ---------------------------------------------------------------------------
# Transport runners
# ---------------------------------------------------------------------------
async def test_streamable(session, base_url):
    """Test Streamable HTTP transport (POST /mcp)."""
    url = f"{base_url}/mcp"
    state = {"req_id": 0, "session_id": None}

    print(f"\n{BOLD}{'=' * 64}")
    print(f"  STREAMABLE HTTP \u2014 POST /mcp")
    print(f"{'=' * 64}{RST}")

    resp = await send_streamable(session, url, "initialize", {
        "protocolVersion": "2024-11-05",
        "capabilities": {},
        "clientInfo": {"name": "dual-test", "version": "1.0"},
    }, state=state)
    if not resp or "result" not in resp:
        print(f"  {KO} initialize failed: {resp}")
        return
    info = resp["result"]["serverInfo"]
    print(f"  Server : {info['name']} v{info['version']}")
    print(f"  Session: {state['session_id']}")

    await send_streamable(session, url, "notifications/initialized",
                          notification=True, state=state)

    resp = await send_streamable(session, url, "tools/list", state=state)
    tools = resp["result"]["tools"]
    print(f"  Tools  : {len(tools)}")

    async def send_fn(method, params=None):
        return await send_streamable(session, url, method, params,
                                     state=state)

    await run_tool_tests(send_fn)


async def test_sse(session, base_url):
    """Test SSE transport (GET /sse + POST /mcp/message)."""
    print(f"\n{BOLD}{'=' * 64}")
    print(f"  SSE \u2014 GET /sse + POST /mcp/message")
    print(f"{'=' * 64}{RST}")

    client = SseClient(base_url)
    try:
        await client.connect(session)
    except (asyncio.TimeoutError, RuntimeError) as exc:
        print(f"  {KO} SSE connect failed: {exc}")
        return

    display = client.msg_url.split("?")[0]
    print(f"  Endpoint: {display}")

    try:
        resp = await client.send(session, "initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "dual-test-sse", "version": "1.0"},
        })
        if not resp or "result" not in resp:
            print(f"  {KO} initialize failed: {resp}")
            return
        info = resp["result"]["serverInfo"]
        print(f"  Server : {info['name']} v{info['version']}")

        await client.send(session, "notifications/initialized",
                          notification=True)

        resp = await client.send(session, "tools/list")
        tools = resp["result"]["tools"]
        print(f"  Tools  : {len(tools)}")

        async def send_fn(method, params=None):
            return await client.send(session, method, params)

        await run_tool_tests(send_fn)
    finally:
        await client.close()


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
async def main():
    parser = argparse.ArgumentParser(description="MCP dual-transport tests")
    parser.add_argument("--base", default="http://localhost:44321",
                        help="Server base URL")
    args = parser.parse_args()
    base = args.base.rstrip("/")

    print(f"Target: {base}")

    async with aiohttp.ClientSession() as session:
        await test_streamable(session, base)
        await test_sse(session, base)

    total = PASS + FAIL
    print(f"\n{'=' * 64}")
    if FAIL == 0:
        print(f"  \033[32mALL {total} TESTS PASSED\033[0m")
    else:
        print(f"  \033[32m{PASS} passed\033[0m, "
              f"\033[31m{FAIL} failed\033[0m / {total}")
    print(f"{'=' * 64}")

    if ERRORS:
        print("\nFailed:")
        for e in ERRORS:
            print(e)

    sys.exit(1 if FAIL else 0)


if __name__ == "__main__":
    asyncio.run(main())
