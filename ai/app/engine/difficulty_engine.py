from app.schemas.common import DifficultyBand
from app.schemas.telemetry import GameTelemetry
from app.schemas.decisions import DifficultyDecision


def clamp(value: float, minimum: float = 0.0, maximum: float = 1.0) -> float:
    return max(minimum, min(value, maximum))


def get_difficulty_band(score: float) -> DifficultyBand:
    if score < 0.25:
        return DifficultyBand.LOW
    if score < 0.50:
        return DifficultyBand.MODERATE
    if score < 0.75:
        return DifficultyBand.HIGH
    return DifficultyBand.EXTREME


def evaluate_difficulty(
    telemetry: GameTelemetry,
    current_score: float = 0.5,
) -> DifficultyDecision:
    players = telemetry.players
    world = telemetry.world

    # Baseline challenge increases as the world progresses.
    day_progress = max(0, min(world.day - 1, 10))
    target = 0.30 + day_progress * 0.035

    if world.phase.value == "night":
        target += 0.12

    # Stronger teams can handle greater pressure.
    target += min(players.average_equipment_level, 5) * 0.025

    # Ease the challenge when the team is struggling.
    if players.average_health < 30:
        target -= 0.20
    elif players.average_health < 50:
        target -= 0.10

    target -= min(players.deaths_last_5_minutes, 5) * 0.045

    # Reward sustained success with gradually increasing difficulty.
    if (
        players.kills_last_5_minutes >= 10
        and players.deaths_last_5_minutes == 0
        and players.average_health >= 70
    ):
        target += 0.12

    target = clamp(target)

    # Reduce difficulty faster than we increase it.
    target = clamp(target)
    alpha = 0.35 if target < current_score else 0.15
    score = clamp(current_score + alpha * (target - current_score))

    rounded_score = round(score, 3)
    return DifficultyDecision(
        score=rounded_score,
        band=get_difficulty_band(rounded_score),
    )