import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from services.database import _coverage_adjusted_score, _expected_question_count_from_session


def test_coverage_adjusted_score_penalizes_skipped_questions():
    assert round(_coverage_adjusted_score(70, usable_answers=1, expected_questions=9)) == 8


def test_coverage_adjusted_score_uses_observed_answers_when_expected_missing():
    assert round(_coverage_adjusted_score(210, usable_answers=3, expected_questions=0)) == 70


def test_coverage_adjusted_score_keeps_complete_session_average():
    assert round(_coverage_adjusted_score(630, usable_answers=9, expected_questions=9)) == 70


def test_expected_question_count_falls_back_for_older_standard_sessions():
    assert _expected_question_count_from_session({"length": "standard", "question_count": 0}, 1) == 9


def test_expected_question_count_falls_back_for_older_quick_sessions():
    assert _expected_question_count_from_session({"length": "quick", "question_count": 0}, 1) == 4
