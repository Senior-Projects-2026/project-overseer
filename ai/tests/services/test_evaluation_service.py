from app.service.evaluation_service import evaluate_telemetry


def test_evaluation_returns_expected_match(telemetry):
    result = evaluate_telemetry(telemetry, current_score=0.5)

    assert result.match_id == telemetry.match_id
    assert result.evaluated_tick == telemetry.tick


def test_evaluation_contains_difficulty_and_spawn_decisions(telemetry):
    result = evaluate_telemetry(telemetry, current_score=0.5)

    assert 0.0 <= result.difficulty.score <= 1.0
    assert result.difficulty.band is not None
    assert result.spawn.recommended_count >= 0


def test_evaluation_reports_night_phase(telemetry):
    from app.schemas.common import WorldPhase

    telemetry.world.phase = WorldPhase.NIGHT

    result = evaluate_telemetry(telemetry, current_score=0.5)

    assert "NIGHT_PHASE" in result.reason_codes


def test_evaluation_reports_suppressed_spawning(telemetry):
    telemetry.performance.server_load = 0.95

    result = evaluate_telemetry(telemetry, current_score=0.5)

    assert result.spawn.enabled is False
    assert "SPAWNING_SUPPRESSED" in result.reason_codes