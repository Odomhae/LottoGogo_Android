"""Room 스키마 JSON과 tools/data/draws.csv로 app/src/main/assets/lotto.db를 만든다.

Room의 createFromAsset은 스키마가 정확히 일치해야 하므로 테이블/인덱스/room_master_table을
Room이 내보낸 스키마(app/schemas)의 SQL 그대로 생성한다.

사용법: python tools/build_asset_db.py
"""

from __future__ import annotations

import json
import sqlite3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from fetch_draws import check_continuous, read_draws  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
SCHEMA_DIR = ROOT / "app" / "schemas" / "com.odom.lottogogo.data.local.LottoDatabase"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "lotto.db"


def latest_schema() -> dict:
    files = sorted(SCHEMA_DIR.glob("*.json"), key=lambda p: int(p.stem))
    if not files:
        raise SystemExit(f"no schema json in {SCHEMA_DIR} — build the app once to export it")
    return json.loads(files[-1].read_text(encoding="utf-8"))["database"]


def create_schema(conn: sqlite3.Connection, schema: dict) -> None:
    for entity in schema["entities"]:
        table = entity["tableName"]
        conn.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            conn.execute(index["createSql"].replace("${TABLE_NAME}", table))
    for query in schema["setupQueries"]:
        conn.execute(query)
    conn.execute(f"PRAGMA user_version = {int(schema['version'])}")


def insert_draws(conn: sqlite3.Connection) -> int:
    draws = read_draws()
    if not draws:
        raise SystemExit("tools/data/draws.csv is empty — run tools/fetch_draws.py first")
    check_continuous(draws)
    conn.executemany(
        "INSERT INTO Draw (round, date, n1, n2, n3, n4, n5, n6, bonus, firstPrize, firstWinners) "
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        [(d.round, d.date, *d.numbers, d.bonus, d.first_prize, d.first_winners) for d in draws],
    )
    return len(draws)


def main() -> None:
    schema = latest_schema()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.unlink(missing_ok=True)

    conn = sqlite3.connect(OUTPUT)
    try:
        create_schema(conn, schema)
        count = insert_draws(conn)
        conn.commit()
        conn.execute("VACUUM")
    finally:
        conn.close()
    print(f"{OUTPUT} - schema v{schema['version']} ({schema['identityHash']}), {count} draws")


if __name__ == "__main__":
    main()
