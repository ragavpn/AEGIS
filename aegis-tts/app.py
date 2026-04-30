"""
AEGIS TTS Sidecar — Kokoro-82M Local Inference
FastAPI server that synthesises speech via Kokoro-82M.
Deploy as a separate Railway service; the Node.js backend
calls it at http://aegis-tts.railway.internal:8000/synthesize
"""

import io
import os
import logging
import numpy as np
import soundfile as sf
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel
from contextlib import asynccontextmanager

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("aegis-tts")

# ── Global pipeline (loaded once at startup) ──────────────────────────────────
pipeline = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global pipeline
    logger.info("[TTS] Loading Kokoro-82M pipeline…")
    from kokoro import KPipeline
    pipeline = KPipeline(lang_code="a")  # 'a' = American English
    logger.info("[TTS] Kokoro-82M ready.")
    yield
    logger.info("[TTS] Shutting down.")


app = FastAPI(title="AEGIS TTS", lifespan=lifespan)

# ── Request / Response schemas ────────────────────────────────────────────────


class SynthesizeRequest(BaseModel):
    text: str
    voice: str = "af_heart"
    speed: float = 1.0


# ── Endpoints ─────────────────────────────────────────────────────────────────


@app.get("/health")
async def health():
    return {"status": "ok", "model": "Kokoro-82M", "pipeline_ready": pipeline is not None}


@app.post("/synthesize")
async def synthesize(req: SynthesizeRequest):
    if pipeline is None:
        raise HTTPException(status_code=503, detail="Pipeline not loaded yet")
    if not req.text or not req.text.strip():
        raise HTTPException(status_code=400, detail="text must not be empty")

    logger.info(f"[TTS] Synthesising {len(req.text)} chars | voice={req.voice} | speed={req.speed}")

    try:
        audio_chunks = []
        for _, _, audio in pipeline(req.text, voice=req.voice, speed=req.speed):
            audio_chunks.append(audio)

        if not audio_chunks:
            raise HTTPException(status_code=500, detail="Kokoro produced no audio chunks")

        combined = np.concatenate(audio_chunks)

        # Write to in-memory WAV buffer (24 kHz, mono)
        buf = io.BytesIO()
        sf.write(buf, combined, 24000, format="WAV", subtype="PCM_16")
        buf.seek(0)

        logger.info(f"[TTS] Done — {len(combined)} samples ({len(combined)/24000:.1f}s)")
        return Response(content=buf.read(), media_type="audio/wav")

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"[TTS] Synthesis failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))
