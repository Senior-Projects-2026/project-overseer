from app.schemas.telemetry import GameTelemetry
from app.schemas.decisions import EvaluationResponse
from app.engine.difficulty_engine import evaluate_difficulty
from app.engine.spawn_engine import recommend_spawns


def evaluate_telemetry(
    telemetry: GameTelemetry,
    current_score: float = 0.5,
) -> EvaluationResponse:
    difficulty = evaluate_difficulty(
        telemetry,
        current_score=current_score,
    )

    spawn = recommend_spawns(
        telemetry,
        difficulty_score=difficulty.score,
    )

    reasons = []

    if telemetry.world.phase.value == "night":
        reasons.append("NIGHT_PHASE")

    if telemetry.players.deaths_last_5_minutes > 0:
        reasons.append("RECENT_PLAYER_DEATHS")

    if not spawn.enabled:
        reasons.append("SPAWNING_SUPPRESSED")

    return EvaluationResponse(
        match_id=telemetry.match_id,
        evaluated_tick=telemetry.tick,
        difficulty=difficulty,
        spawn=spawn,
        reason_codes=reasons,
    )