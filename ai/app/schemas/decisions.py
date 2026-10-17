from pydantic import BaseModel, Field

from app.schemas.common import DifficultyBand


class DifficultyDecision(BaseModel):
    score: float = Field(ge=0, le=1)
    band: DifficultyBand


class SpawnDecision(BaseModel):
    enabled: bool
    recommended_count: int = Field(ge=0)
    interval_seconds: float = Field(gt=0)
    max_alive: int = Field(ge=0)


class EvaluationResponse(BaseModel):
    match_id: str
    evaluated_tick: int

    difficulty: DifficultyDecision
    spawn: SpawnDecision

    reason_codes: list[str] = Field(default_factory=list)