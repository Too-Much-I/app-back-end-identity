#!/usr/bin/env python3
"""Inject docs/codex/CURRENT_STATE.md at session startup or resume."""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any


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


def main() -> None:
    event = read_event()
    if event is None:
        emit(
            {
                "systemMessage": (
                    "SessionStart Hook 입력을 읽지 못해 CURRENT_STATE를 "
                    "개발자 컨텍스트에 추가하지 못했습니다."
                )
            }
        )
        return

    state_path = repository_root() / "docs" / "codex" / "CURRENT_STATE.md"
    try:
        current_state = state_path.read_text(encoding="utf-8")
    except OSError:
        emit(
            {
                "systemMessage": (
                    "docs/codex/CURRENT_STATE.md를 읽지 못했습니다. "
                    "저장소 상태 문서를 확인하세요."
                )
            }
        )
        return

    additional_context = (
        "다음은 Identity Service의 현재 저장소 상태입니다. "
        "현재 사용자 요청과 함께 작업 기준으로 사용하세요.\n\n"
        f"{current_state}"
    )
    emit(
        {
            "hookSpecificOutput": {
                "hookEventName": "SessionStart",
                "additionalContext": additional_context,
            }
        }
    )


if __name__ == "__main__":
    main()

