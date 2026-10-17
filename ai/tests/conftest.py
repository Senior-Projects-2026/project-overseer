import pytest

from app.schemas.common import WorldPhase
from app.schemas.telemetry import (
    GameTelemetry,
    PlayerTelemetry,
    EnemyTelemetry,
    WorldTelemetry,
    PerformanceTelemetry,
)


@pytest.fixture
def telemetry():
    return GameTelemetry(
        match_id="test-world",
        tick=1000,
        players=PlayerTelemetry(
            alive=4,
            average_health=80,
            deaths_last_5_minutes=0,
            kills_last_5_minutes=12,
            average_equipment_level=2,
        ),
        enemies=EnemyTelemetry(
            alive=10,
            killed_last_5_minutes=12,
        ),
        world=WorldTelemetry(
            day=3,
            elapsed_seconds=1200,
            phase=WorldPhase.DAY,
        ),
        performance=PerformanceTelemetry(
            server_tick_ms=15,
            server_load=0.5,
        ),
    ) # healthy team performing well