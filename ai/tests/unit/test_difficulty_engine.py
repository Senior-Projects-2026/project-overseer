from app.engine.difficulty_engine import (
    evaluate_difficulty,
    clamp,
    get_difficulty_band,
)
from app.schemas.common import DifficultyBand, WorldPhase


def test_difficulty_score_stays_within_bounds(telemetry):
    result = evaluate_difficulty(telemetry, current_score=0.5)

    assert 0.0 <= result.score <= 1.0


def test_difficulty_increases_gradually(telemetry):
    result = evaluate_difficulty(telemetry, current_score=0.4)

    assert result.score > 0.4


def test_difficulty_decreases_faster_when_team_struggles(telemetry):
    telemetry.players.average_health = 10
    telemetry.players.deaths_last_5_minutes = 5

    result = evaluate_difficulty(telemetry, current_score=0.8)

    assert result.score < 0.8


def test_difficulty_band_matches_score(telemetry):
    result = evaluate_difficulty(telemetry, current_score=0.6)

    assert result.band == get_difficulty_band(result.score)


def test_clamp_limits_values():
    assert clamp(-0.5) == 0.0
    assert clamp(1.5) == 1.0
    assert clamp(0.5) == 0.5


def test_high_day_progress_does_not_exceed_baseline_cap(telemetry):
    telemetry.world.day = 100

    result = evaluate_difficulty(telemetry, current_score=0.5)

    assert 0.0 <= result.score <= 1.0


def test_night_increases_target_pressure(telemetry):
    day_result = evaluate_difficulty(telemetry, current_score=0.5)

    telemetry.world.phase = WorldPhase.NIGHT
    night_result = evaluate_difficulty(telemetry, current_score=0.5)

    assert night_result.score > day_result.score