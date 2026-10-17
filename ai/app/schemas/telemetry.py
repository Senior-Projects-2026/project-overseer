from pydantic import BaseModel, Field

from app.schemas.common import WorldPhase


class PlayerTelemetry(BaseModel):
    alive: int = Field(ge=0) # nr of living players currently
    # aggregate accross all players (down)
    average_health: float = Field(ge=0, le=100)
    deaths_last_5_minutes: int = Field(ge=0) 
    kills_last_5_minutes: int = Field(ge=0)
    average_equipment_level: float = Field(ge=0)


class EnemyTelemetry(BaseModel):
    alive: int = Field(ge=0)
    killed_last_5_minutes: int = Field(ge=0)


class WorldTelemetry(BaseModel):
    day: int = Field(ge=1)
    elapsed_seconds: float = Field(ge=0)
    phase: WorldPhase


class PerformanceTelemetry(BaseModel):
    server_tick_ms: float = Field(ge=0)
    server_load: float = Field(ge=0, le=1)


class GameTelemetry(BaseModel):
    match_id: str = Field(min_length=1)
    tick: int = Field(ge=0)

    players: PlayerTelemetry
    enemies: EnemyTelemetry
    world: WorldTelemetry
    performance: PerformanceTelemetry