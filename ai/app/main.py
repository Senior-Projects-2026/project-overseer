from fastapi import FastAPI

from app.api.routes import router

app = FastAPI(
    title="Project Overseer",
    description="Adaptive difficulty and enemy spawn recommendations",
    version="0.1.0",
)

app.include_router(router)
