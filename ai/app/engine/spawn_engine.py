from app.schemas.telemetry import GameTelemetry
from app.schemas.decisions import SpawnDecision
from app.engines.world_pressure_engine import (
    calculate_world_pressure,
)


def recommend_spawns(
    telemetry: GameTelemetry,
    difficulty_score: float,
) -> SpawnDecision:
    enemies = telemetry.enemies
    players = telemetry.players
    performance = telemetry.performance

    # Limit population according to group size and difficulty.
    max_alive = round(
        10
        + players.alive * 5
        + difficulty_score * 10
    )

    # Avoid adding pressure when server performance is poor.
    if (
        performance.server_load >= 0.85
        or performance.server_tick_ms >= 40
    ):
        return SpawnDecision(
            enabled=False,
            recommended_count=0,
            interval_seconds=10.0,
            max_alive=max_alive,
        )

    # Respect the enemy population limit.
    remaining_capacity = max(0, max_alive - enemies.alive)

    if remaining_capacity == 0 or players.alive == 0:
        return SpawnDecision(
            enabled=False,
            recommended_count=0,
            interval_seconds=10.0,
            max_alive=max_alive,
        )

    world_pressure = calculate_world_pressure(telemetry)

    # Higher difficulty and world pressure increase spawn frequency.
    desired_count = round(
        (1 + difficulty_score * 4) * world_pressure
    )

    # Higher difficulty means a shorter interval.
    interval = 12.0 - difficulty_score * 8.0
    interval /= world_pressure

    # Bound the recommendation.
    spawn_count = min(
        max(1, desired_count),
        remaining_capacity,
        10,
    )

    interval = max(2.0, min(interval, 15.0))

    return SpawnDecision(
        enabled=True,
        recommended_count=spawn_count,
        interval_seconds=round(interval, 2),
        max_alive=max_alive,
    )