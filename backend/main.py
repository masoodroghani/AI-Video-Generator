import base64
import os
from typing import Optional
import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

RUNWAY_API_KEY = os.getenv("RUNWAY_API_KEY", "")
RUNWAY_BASE = "https://api.dev.runwayml.com/v1"
RUNWAY_VERSION = "2024-11-06"
DEFAULT_MODEL = os.getenv("RUNWAY_MODEL", "gen4.5")
app = FastAPI(title="AI Video Studio API", version="1.0.0")

class GenerateRequest(BaseModel):
    mode: str = Field(pattern="^(text|image)$")
    prompt: str = Field(min_length=1, max_length=1000)
    duration: int = Field(default=5, ge=2, le=10)
    ratio: str = Field(default="720:1280", pattern="^(1280:720|720:1280|960:960)$")
    image_base64: Optional[str] = None
    image_mime: Optional[str] = "image/jpeg"

def headers():
    if not RUNWAY_API_KEY:
        raise HTTPException(status_code=503, detail="Server not configured: set RUNWAY_API_KEY.")
    return {"Authorization": f"Bearer {RUNWAY_API_KEY}", "X-Runway-Version": RUNWAY_VERSION, "Content-Type": "application/json"}

@app.get("/health")
async def health():
    return {"ok": True, "provider": "Runway", "configured": bool(RUNWAY_API_KEY)}

@app.post("/generate")
async def generate(body: GenerateRequest):
    h = headers()
    if body.mode == "image" and not body.image_base64:
        raise HTTPException(status_code=400, detail="Image is required for image-to-video.")
    payload = {"model": DEFAULT_MODEL, "promptText": body.prompt, "ratio": body.ratio, "duration": body.duration}
    endpoint = "text_to_video"
    if body.mode == "image":
        raw = body.image_base64.split(",", 1)[-1]
        try:
            decoded = base64.b64decode(raw, validate=True)
        except Exception:
            raise HTTPException(status_code=400, detail="Invalid image data.")
        if len(decoded) > 5 * 1024 * 1024:
            raise HTTPException(status_code=413, detail="Image must be 5 MB or smaller for inline upload.")
        mime = body.image_mime if body.image_mime in ("image/jpeg", "image/png", "image/webp") else "image/jpeg"
        payload["promptImage"] = f"data:{mime};base64,{base64.b64encode(decoded).decode('ascii')}"
        endpoint = "image_to_video"
    async with httpx.AsyncClient(timeout=60) as client:
        response = await client.post(f"{RUNWAY_BASE}/{endpoint}", headers=h, json=payload)
    if response.status_code >= 400:
        try: detail = response.json()
        except Exception: detail = response.text[:1000]
        raise HTTPException(status_code=response.status_code, detail=detail)
    return response.json()

@app.get("/tasks/{task_id}")
async def task_status(task_id: str):
    async with httpx.AsyncClient(timeout=30) as client:
        response = await client.get(f"{RUNWAY_BASE}/tasks/{task_id}", headers=headers())
    if response.status_code >= 400:
        try: detail = response.json()
        except Exception: detail = response.text[:1000]
        raise HTTPException(status_code=response.status_code, detail=detail)
    return response.json()
