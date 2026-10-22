"""dhlottery 파싱/검증 단위 테스트 (네트워크 없이 실행).

사용법: python -m unittest discover -s tools
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))

import dhlottery  # noqa: E402


def item(round_: int, numbers=(40, 23, 10, 29, 33, 37), bonus=16) -> dict:
    return {
        "ltEpsd": round_,
        **{f"tm{i + 1}WnNo": n for i, n in enumerate(numbers)},
        "bnsWnNo": bonus,
        "ltRflYmd": "20021207",
        "rnk1WnNope": 0,
        "rnk1WnAmt": 0,
    }


class ParseTest(unittest.TestCase):
    def test_parse_sorts_numbers_and_formats_date(self):
        draw = dhlottery.parse(item(1))
        self.assertEqual(draw.numbers, (10, 23, 29, 33, 37, 40))
        self.assertEqual(draw.date, "2002-12-07")
        self.assertEqual(draw.bonus, 16)

    def test_rejects_duplicate_and_out_of_range(self):
        with self.assertRaises(ValueError):
            dhlottery.parse(item(1, numbers=(1, 1, 2, 3, 4, 5)))
        with self.assertRaises(ValueError):
            dhlottery.parse(item(1, bonus=10))  # 보너스가 당첨번호와 중복
        with self.assertRaises(ValueError):
            dhlottery.parse(item(1, numbers=(1, 2, 3, 4, 5, 46)))


class IterAfterTest(unittest.TestCase):
    def test_collects_until_no_newer_rounds(self):
        latest = 23

        def fake_request(params):
            center = int(params["srchLtEpsd"])
            if center > latest:
                return []
            # 사이트처럼 중심 회차 앞뒤로 최대 10개
            return [item(r) for r in range(min(center + 4, latest), max(center - 6, 0), -1)]

        with mock.patch.object(dhlottery, "_request", fake_request), \
                mock.patch.object(dhlottery.time, "sleep", lambda _: None):
            draws = dhlottery.fetch_after(0)

        self.assertEqual([d.round for d in draws], list(range(1, latest + 1)))

    def test_incremental_from_max_round(self):
        with mock.patch.object(dhlottery, "_request", lambda p: [item(r) for r in (12, 11, 10)] if p["srchLtEpsd"] == "11" else []), \
                mock.patch.object(dhlottery.time, "sleep", lambda _: None):
            draws = dhlottery.fetch_after(10)

        self.assertEqual([d.round for d in draws], [11, 12])


if __name__ == "__main__":
    unittest.main()
