# ─── AEGIS TTS Sidecar — Kokoro-82M ──────────────────────────────────────────
# Deploy as a NEW SERVICE in the same Railway project as aegis-backend.
# Services in the same project communicate via Railway private networking:
#   http://aegis-tts.railway.internal:8000
#
# In aegis-backend, set:
#   TTS_SERVICE=local
#   TTS_LOCAL_URL=http://aegis-tts.railway.internal:8000

FROM python:3.11-slim

WORKDIR /app

# espeak-ng is required by kokoro for G2P (grapheme-to-phoneme) conversion
RUN apt-get update && apt-get install -y \
    espeak-ng \
    libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pre-download Kokoro model weights at build time so cold-starts are instant
RUN python -c "\
from huggingface_hub import hf_hub_download; \
hf_hub_download(repo_id='hexgrad/Kokoro-82M', filename='kokoro-v1_0.pth'); \
print('Kokoro weights baked into image.')"

COPY . .

ENV PORT=8000
EXPOSE 8000

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
