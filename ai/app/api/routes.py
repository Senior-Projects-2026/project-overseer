from fastapi import APIRouter

from app.schemas.telemetry import GameTelemetry
from app.schemas.decisions import EvaluationResponse
from app.service.evaluation_service import evaluate_telemetry

router = APIRouter()


@router.post("/evaluate", response_model=EvaluationResponse)
def evaluate(telemetry: GameTelemetry) -> EvaluationResponse:
    return evaluate_telemetry(telemetry)
