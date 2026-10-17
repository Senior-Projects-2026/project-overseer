from app.schemas.telemetry import GameTelemetry


def calculate_world_pressure(
    telemetry: GameTelemetry,
) -> float:
    world = telemetry.world

    pressure = 1.0

    # Night increases environmental pressure.
    if world.phase.value == "night":
        pressure += 0.30

    # Later days gradually increase pressure.
    day_progress = max(0, min(world.day - 1, 10))
    pressure += day_progress * 0.04

    return min(pressure, 1.7)