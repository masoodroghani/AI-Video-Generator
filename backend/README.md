# AI Video Studio backend (Runway)

## Run locally
1. Install Python 3.11+.
2. `cd backend`
3. Install dependencies: `pip install -r requirements.txt`
4. Set `RUNWAY_API_KEY` as an environment variable. Never commit the key.
5. Start: `uvicorn main:app --reload --host 0.0.0.0 --port 8000`
6. Health check: `http://localhost:8000/health`

Endpoints:
- `POST /generate` JSON: `mode` (`text` or `image`), `prompt`, `duration`, `ratio`; image mode also requires `image_base64` and optionally `image_mime`.
- `GET /tasks/{task_id}` returns provider task status and output details.

Runway accepts image data URIs for image-to-video, with a documented 5 MB inline data-URI limit. This backend checks a 5 MB decoded-image limit.

The API key stays on the backend. Before public deployment, add user authentication, request quotas, abuse prevention, HTTPS, and per-user usage tracking. The Android app must point to the deployed HTTPS base URL.
