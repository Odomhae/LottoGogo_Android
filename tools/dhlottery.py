"""동행복권 로또 6/45 당첨 결과 수집.

공식 API가 없어 웹사이트(당첨결과 페이지)가 쓰는 JSON 엔드포인트를 사용한다.
수집 소스가 바뀌면 이 모듈만 수정한다. (fetch_draws.py, collect/collect.py 공용)
"""

from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Iterator
from dataclasses import dataclass

ENDPOINT = "https://www.dhlottery.co.kr/lt645/selectPstLt645InfoNew.do"
REFERER = "https://www.dhlottery.co.kr/lt645/result"
USER_AGENT = "Mozilla/5.0 (LottoGogo data collector)"
REQUEST_DELAY_SEC = 1.0
TIMEOUT_SEC = 30
MAX_RETRIES = 4


@dataclass(frozen=True)
class Draw:
    round: int
    date: str            # yyyy-MM-dd
    numbers: tuple[int, ...]   # 오름차순 6개
    bonus: int
    first_prize: int     # 1등 1인당 당첨금(원)
    first_winners: int


def _request(params: dict[str, str]) -> list[dict]:
    """일시적 네트워크 오류는 지수 백오프로 재시도한다."""
    for attempt in range(MAX_RETRIES):
        try:
            return _request_once(params)
        except (urllib.error.URLError, TimeoutError) as e:
            if attempt == MAX_RETRIES - 1:
                raise
            wait = 5 * 2 ** attempt
            print(f"request failed ({e}); retry in {wait}s", file=sys.stderr)
            time.sleep(wait)
    raise AssertionError("unreachable")


def _request_once(params: dict[str, str]) -> list[dict]:
    url = f"{ENDPOINT}?{urllib.parse.urlencode(params)}"
    req = urllib.request.Request(
        url,
        headers={"User-Agent": USER_AGENT, "Accept": "application/json", "Referer": REFERER},
    )
    with urllib.request.urlopen(req, timeout=TIMEOUT_SEC) as resp:
        body = json.load(resp)
    data = body.get("data") or {}
    return data.get("list") or []


def parse(item: dict) -> Draw:
    numbers = tuple(sorted(int(item[f"tm{i}WnNo"]) for i in range(1, 7)))
    bonus = int(item["bnsWnNo"])
    ymd = str(item["ltRflYmd"])
    draw = Draw(
        round=int(item["ltEpsd"]),
        date=f"{ymd[0:4]}-{ymd[4:6]}-{ymd[6:8]}",
        numbers=numbers,
        bonus=bonus,
        first_prize=int(item["rnk1WnAmt"]),
        first_winners=int(item["rnk1WnNope"]),
    )
    validate(draw)
    return draw


def validate(draw: Draw) -> None:
    all_numbers = set(draw.numbers) | {draw.bonus}
    if len(draw.numbers) != 6 or len(all_numbers) != 7:
        raise ValueError(f"duplicate numbers: {draw}")
    if not all(1 <= n <= 45 for n in all_numbers):
        raise ValueError(f"number out of range: {draw}")
    if draw.round <= 0 or draw.first_prize < 0 or draw.first_winners < 0:
        raise ValueError(f"invalid values: {draw}")


def fetch_around(round_: int) -> list[Draw]:
    """round_ 주변 회차 묶음. 아직 추첨 전 회차면 빈 리스트."""
    items = _request({"srchDir": "center", "srchLtEpsd": str(round_)})
    return [parse(item) for item in items]


def iter_after(max_round: int) -> Iterator[list[Draw]]:
    """max_round 이후 회차를 오름차순 묶음으로 내보낸다.

    조회 창의 범위는 사이트 구현에 맡기고, 돌려받은 회차 중 start 이상만 취한 뒤 다음 회차로 진행한다.
    """
    start = max_round + 1
    while True:
        new = sorted((d for d in fetch_around(start) if d.round >= start), key=lambda d: d.round)
        if not new:
            return
        yield new
        start = new[-1].round + 1
        time.sleep(REQUEST_DELAY_SEC)


def fetch_after(max_round: int) -> list[Draw]:
    """max_round 이후의 모든 회차를 오름차순으로 반환한다."""
    return [d for batch in iter_after(max_round) for d in batch]
