"""collect.py 문서 변환 테스트 (firebase-admin 없이 실행).

사용법: python -m unittest discover -s tools/collect
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import collect  # noqa: E402
import dhlottery  # noqa: E402


class ToDocumentTest(unittest.TestCase):
    def test_fields_match_app_entity(self):
        draw = dhlottery.Draw(1246, "2026-10-17", (16, 22, 23, 24, 27, 28), 6, 1881025430, 16)

        doc = collect.to_document(draw)

        self.assertEqual(
            doc,
            {
                "round": 1246, "date": "2026-10-17",
                "n1": 16, "n2": 22, "n3": 23, "n4": 24, "n5": 27, "n6": 28,
                "bonus": 6, "firstPrize": 1881025430, "firstWinners": 16,
            },
        )


if __name__ == "__main__":
    unittest.main()
