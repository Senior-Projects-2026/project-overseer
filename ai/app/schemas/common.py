from enum import Enum


class WorldPhase(str, Enum):
    DAY = "day"
    NIGHT = "night"


class DifficultyBand(str, Enum):
    LOW = "low"
    MODERATE = "moderate"
    HIGH = "high"
    EXTREME = "extreme"