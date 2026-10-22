"""전 회차 당첨 결과를 tools/data/draws.csv로 수집한다 (증분).

사용법: python tools/fetch_draws.py
"""

from __future__ import annotations

import csv
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import dhlottery  # noqa: E402

CSV_PATH = Path(__file__).resolve().parent / "data" / "draws.csv"
HEADER = ["round", "date", "n1", "n2", "n3", "n4", "n5", "n6", "bonus", "firstPrize", "firstWinners"]


def read_draws(path: Path = CSV_PATH) -> list[dhlottery.Draw]:
    if not path.exists():
        return []
    with path.open(newline="", encoding="utf-8") as f:
        return [
            dhlottery.Draw(
                round=int(row["round"]),
                date=row["date"],
                numbers=tuple(int(row[f"n{i}"]) for i in range(1, 7)),
                bonus=int(row["bonus"]),
                first_prize=int(row["firstPrize"]),
                first_winners=int(row["firstWinners"]),
            )
            for row in csv.DictReader(f)
        ]


def write_draws(draws: list[dhlottery.Draw], path: Path = CSV_PATH) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, lineterminator="\n")
        writer.writerow(HEADER)
        for d in sorted(draws, key=lambda d: d.round):
            writer.writerow([d.round, d.date, *d.numbers, d.bonus, d.first_prize, d.first_winners])


def check_continuous(draws: list[dhlottery.Draw]) -> None:
    rounds = [d.round for d in sorted(draws, key=lambda d: d.round)]
    if rounds and rounds != list(range(1, rounds[-1] + 1)):
        missing = sorted(set(range(1, rounds[-1] + 1)) - set(rounds))
        raise SystemExit(f"missing rounds: {missing[:20]}")


def main() -> None:
    existing = read_draws()
    max_round = max((d.round for d in existing), default=0)
    print(f"existing: {len(existing)} rounds (max {max_round})")

    draws = list(existing)
    # 묶음마다 저장해 중간에 실패해도 다음 실행에서 이어 받는다
    for batch in dhlottery.iter_after(max_round):
        draws.extend(batch)
        write_draws(draws)
        print(f"  ~{batch[-1].round}", flush=True)
    check_continuous(draws)
    print(f"fetched {len(draws) - len(existing)} new rounds -> {CSV_PATH} (max {max((d.round for d in draws), default=0)})")


if __name__ == "__main__":
    main()
