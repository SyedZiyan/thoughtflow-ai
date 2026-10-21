FROM python:3.11-slim

WORKDIR /app

# Install system dependencies including FFmpeg and fonts
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    fonts-dejavu-core \
    libpq-dev \
    gcc \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy and install python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir psycopg2-binary && \
    pip install --no-cache-dir -r requirements.txt

# Copy application source
COPY . .

# Ensure storage directories exist
RUN mkdir -p storage/videos storage/temp

EXPOSE 8000

# Support dynamic PORT environment variable (Render, Railway, Fly.io, Heroku)
CMD exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}
