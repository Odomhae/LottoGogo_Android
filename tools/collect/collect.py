"""Firestore `draws` 컬렉션에 신규 회차를 기록한다 (GitHub Actions에서 실행).

1. Firestore의 최대 회차 확인
2. 그 이후 회차를 동행복권에서 수집 (tools/dhlottery.py)
3. Admin SDK로 기록 (문서 ID = 회차 문자열)

환경 변수 FIREBASE_SERVICE_ACCOUNT: 서비스 계정 키 JSON 문자열 (GitHub Secrets)
사용법: python tools/collect/collect.py [--dry-run]
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import dhlottery  # noqa: E402

COLLECTION = "draws"
BATCH_LIMIT = 500   # Firestore 배치 쓰기 최대 개수


def to_document(draw: dhlottery.Draw) -> dict:
    """앱의 Draw 엔티티와 같은 필드 (date는 yyyy-MM-dd 문자열)."""
    return {
        "round": draw.round,
        "date": draw.date,
        **{f"n{i + 1}": n for i, n in enumerate(draw.numbers)},
        "bonus": draw.bonus,
        "firstPrize": draw.first_prize,
        "firstWinners": draw.first_winners,
    }


def init_firestore():
    import firebase_admin
    from firebase_admin import credentials, firestore

    raw = os.environ.get("FIREBASE_SERVICE_ACCOUNT")
    if not raw:
        raise SystemExit("FIREBASE_SERVICE_ACCOUNT is not set")
    firebase_admin.initialize_app(credentials.Certificate(json.loads(raw)))
    return firestore.client()


def max_round(db) -> int:
    from firebase_admin import firestore

    docs = db.collection(COLLECTION).order_by("round", direction=firestore.Query.DESCENDING).limit(1).get()
    return int(docs[0].get("round")) if docs else 0


def write_draws(db, draws: list[dhlottery.Draw]) -> None:
    for start in range(0, len(draws), BATCH_LIMIT):
        batch = db.batch()
        for draw in draws[start:start + BATCH_LIMIT]:
            batch.set(db.collection(COLLECTION).document(str(draw.round)), to_document(draw))
        batch.commit()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="수집만 하고 Firestore에 쓰지 않음")
    parser.add_argument("--after", type=int, help="dry-run에서 기준 회차 (기본: Firestore 최대 회차)")
    args = parser.parse_args()

    db = None if args.dry_run else init_firestore()
    current = args.after if args.dry_run else max_round(db)
    current = current or 0
    print(f"firestore max round: {current}")

    draws = dhlottery.fetch_after(current)
    if not draws:
        print("no new rounds")
        return
    if db is not None:
        write_draws(db, draws)
    print(f"{'(dry-run) ' if db is None else ''}wrote rounds {draws[0].round}..{draws[-1].round} ({len(draws)})")


if __name__ == "__main__":
    main()
