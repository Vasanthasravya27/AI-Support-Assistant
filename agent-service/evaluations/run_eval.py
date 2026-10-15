"""Run lightweight answer-grounding checks against a running agent instance."""
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

API_URL = os.getenv("AGENT_URL", "http://localhost:3000/api/chat")
CASES = Path(__file__).with_name("cases.json")


def evaluate(case: dict) -> tuple[bool, list[str]]:
    payload = json.dumps({"message": case["message"], "conversation_id": str(uuid.uuid4())}).encode()
    request = urllib.request.Request(API_URL, data=payload,
                                     headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=90) as response:
            result = json.loads(response.read())
    except (urllib.error.URLError, TimeoutError) as exc:
        return False, [f"request failed: {exc}"]
    answer = str(result.get("answer", "")).lower()
    used = set(result.get("tools_used", []))
    failures = [f"missing tool {name}" for name in case["must_use_tools"] if name not in used]
    if not any(phrase.lower() in answer for phrase in case["answer_contains_any"]):
        failures.append("expected support detail not found in answer")
    return not failures, failures


def main() -> int:
    cases = json.loads(CASES.read_text(encoding="utf-8"))
    failed = 0
    for case in cases:
        passed, reasons = evaluate(case)
        print(f"{'PASS' if passed else 'FAIL'}  {case['name']}")
        for reason in reasons:
            print(f"      {reason}")
        failed += not passed
    print(f"\n{len(cases) - failed}/{len(cases)} evaluation cases passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
