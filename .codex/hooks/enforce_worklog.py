#!/usr/bin/env python3
"""Block turn completion until the current turn is present in WORKLOG.md."""

from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Sequence


MAX_TURN_ID_LENGTH = 512
MAX_GIT_OUTPUT = 12_000


def emit(payload: dict[str, Any]) -> None:
    json.dump(payload, sys.stdout, ensure_ascii=False)
    sys.stdout.write("\n")


def repository_root() -> Path:
    return Path(__file__).resolve().parents[2]


def read_event() -> dict[str, Any] | None:
    try:
        value = json.load(sys.stdin)
    except (json.JSONDecodeError, OSError):
        return None
    return value if isinstance(value, dict) else None


def valid_turn_id(value: Any) -> str | None:
    if not isinstance(value, str) or not value or len(value) > MAX_TURN_ID_LENGTH:
        return None
    if not value.isprintable() or "-->" in value:
        return None
    return value


def contains_marker(worklog_path: Path, marker: str) -> bool:
    try:
        with worklog_path.open("r", encoding="utf-8", errors="replace") as worklog:
            return any(marker in line for line in worklog)
    except OSError:
        return False


def sanitize_git_output(value: str) -> str:
    safe_characters = []
    for character in value:
        code_point = ord(character)
        if character in {"\n", "\t"} or (code_point >= 32 and code_point != 127):
            safe_characters.append(character)
        else:
            safe_characters.append("?")

    sanitized = "".join(safe_characters).strip()
    if len(sanitized) > MAX_GIT_OUTPUT:
        sanitized = f"{sanitized[:MAX_GIT_OUTPUT]}\n... (truncated)"
    return sanitized or "(변경 없음)"


def run_git(root: Path, arguments: Sequence[str]) -> str:
    try:
        result = subprocess.run(
            ["git", *arguments],
            cwd=root,
            check=False,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=5,
        )
    except (OSError, subprocess.SubprocessError):
        return "(조회 실패)"

    if result.returncode != 0:
        return "(조회 실패)"
    return sanitize_git_output(result.stdout)


def indent_block(value: str) -> str:
    return "\n".join(f"    {line}" for line in value.splitlines())


def append_fallback(worklog_path: Path, marker: str, root: Path) -> bool:
    branch = run_git(root, ["branch", "--show-current"])
    status = run_git(root, ["status", "--short"])
    diff_stat = run_git(root, ["diff", "--stat"])

    record = (
        "\n## Codex Stop Hook 안전 fallback\n\n"
        f"{marker}\n\n"
        "- 현재 브랜치:\n\n"
        f"{indent_block(branch)}\n\n"
        "- `git status --short`:\n\n"
        f"{indent_block(status)}\n\n"
        "- `git diff --stat`:\n\n"
        f"{indent_block(diff_stat)}\n"
    )

    try:
        worklog_path.parent.mkdir(parents=True, exist_ok=True)
        with worklog_path.open("a", encoding="utf-8") as worklog:
            worklog.write(record)
            worklog.flush()
            os.fsync(worklog.fileno())
    except OSError:
        return False
    return True


def main() -> None:
    event = read_event()
    stop_hook_active = bool(event and event.get("stop_hook_active") is True)
    turn_id = valid_turn_id(event.get("turn_id")) if event else None

    if turn_id is None:
        if stop_hook_active:
            emit(
                {
                    "continue": True,
                    "systemMessage": (
                        "Stop Hook이 유효한 turn_id를 받지 못했습니다. "
                        "무한 반복을 피하기 위해 종료를 허용합니다."
                    ),
                }
            )
        else:
            emit(
                {
                    "decision": "block",
                    "reason": (
                        "유효한 turn_id가 없어 WORKLOG 기록을 확인할 수 없습니다. "
                        "WORKLOG와 CURRENT_STATE를 갱신한 뒤 다시 종료하세요."
                    ),
                }
            )
        return

    marker = f"<!-- codex-turn:{turn_id} -->"
    root = repository_root()
    worklog_path = root / "docs" / "codex" / "WORKLOG.md"

    if contains_marker(worklog_path, marker):
        emit({"continue": True})
        return

    if not stop_hook_active:
        emit(
            {
                "decision": "block",
                "reason": (
                    "현재 turn의 WORKLOG 기록이 없습니다. "
                    "docs/codex/WORKLOG.md 끝에 이번 작업 항목을 append하고 "
                    "docs/codex/CURRENT_STATE.md를 최신 상태로 갱신하세요. "
                    f"새 WORKLOG 항목에 {marker} 를 포함하고 Secret은 기록하지 "
                    "마세요. 완료 후 다시 종료하세요."
                ),
            }
        )
        return

    appended = append_fallback(worklog_path, marker, root)
    if appended:
        emit({"continue": True})
    else:
        emit(
            {
                "continue": True,
                "systemMessage": (
                    "Stop Hook의 안전 fallback 기록을 append하지 못했지만 "
                    "무한 반복을 피하기 위해 종료를 허용합니다."
                ),
            }
        )


if __name__ == "__main__":
    main()
