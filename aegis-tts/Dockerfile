# ─── AEGIS TTS Sidecar — Kokoro-82M ──────────────────────────────────────────
# Runs Kokoro-82M locally. Deploy as a Railway service alongside aegis-backend.
# Internal URL: http://aegis-tts.railway.internal:8000

FROM python:3.11-slim

WORKDIR /app

# espeak-ng is required by kokoro for G2P (grapheme-to-phoneme) conversion
RUN apt-get update && apt-get install -y \
    espeak-ng \
    libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pre-download Kokoro model weights at build time so cold-starts are fast
# This bakes the ~300 MB weights into the Docker image layer
RUN python -c "\
from huggingface_hub import hf_hub_download; \
hf_hub_download(repo_id='hexgrad/Kokoro-82M', filename='kokoro-v1_0.pth'); \
print('Model weights downloaded.')"

COPY . .

ENV PORT=8000

EXPOSE 8000

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
