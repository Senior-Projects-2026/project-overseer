from app.engine.spawn_engine import recommend_spawns


def test_spawning_is_disabled_when_server_load_is_high(telemetry):
    telemetry.performance.server_load = 0.9

    result = recommend_spawns(telemetry, difficulty_score=0.7)

    assert result.enabled is False
    assert result.recommended_count == 0


def test_spawning_is_disabled_when_no_players_are_alive(telemetry):
    telemetry.players.alive = 0

    result = recommend_spawns(telemetry, difficulty_score=0.5)

    assert result.enabled is False
    assert result.recommended_count == 0


def test_spawning_is_disabled_when_population_cap_is_reached(telemetry):
    difficulty = 0.5
    max_alive = round(10 + telemetry.players.alive * 5 + difficulty * 10)
    telemetry.enemies.alive = max_alive

    result = recommend_spawns(telemetry, difficulty_score=difficulty)

    assert result.enabled is False
    assert result.recommended_count == 0


def test_spawn_count_does_not_exceed_remaining_capacity(telemetry):
    difficulty = 0.5
    max_alive = round(10 + telemetry.players.alive * 5 + difficulty * 10)
    telemetry.enemies.alive = max_alive - 1

    result = recommend_spawns(telemetry, difficulty_score=difficulty)

    assert result.recommended_count <= 1


def test_spawn_interval_is_bounded(telemetry):
    result = recommend_spawns(telemetry, difficulty_score=0.8)

    assert 2.0 <= result.interval_seconds <= 15.0