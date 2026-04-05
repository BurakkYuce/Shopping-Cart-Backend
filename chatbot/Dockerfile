FROM python:3.11-slim

WORKDIR /app

# Install system deps for psycopg2
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    supervisor \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir -r requirements.txt

COPY . .

# supervisord config
COPY supervisord.conf /etc/supervisor/conf.d/chatbot.conf

EXPOSE 8001 8002

HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
    CMD curl -sf http://localhost:8002/health || exit 1

CMD ["supervisord", "-n", "-c", "/etc/supervisor/conf.d/chatbot.conf"]
