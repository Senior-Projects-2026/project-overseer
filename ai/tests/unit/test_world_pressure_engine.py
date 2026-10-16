from app.engine.world_pressure_engine import calculate_world_pressure
from app.schemas.common import WorldPhase


def test_night_has_higher_pressure_than_day(telemetry):
    day_pressure = calculate_world_pressure(telemetry)

    telemetry.world.phase = WorldPhase.NIGHT
    night_pressure = calculate_world_pressure(telemetry)

    assert night_pressure > day_pressure


def test_world_pressure_increases_with_day_progress(telemetry):
    telemetry.world.day = 1
    early_pressure = calculate_world_pressure(telemetry)

    telemetry.world.day = 5
    later_pressure = calculate_world_pressure(telemetry)

    assert later_pressure > early_pressure


def test_world_pressure_has_upper_bound(telemetry):
    telemetry.world.day = 100
    telemetry.world.phase = WorldPhase.NIGHT

    assert calculate_world_pressure(telemetry) <= 1.7