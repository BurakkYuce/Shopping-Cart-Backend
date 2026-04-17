#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PARENT="$(cd "$REPO_ROOT/.." && pwd)"
FRONTEND_DIR="$PARENT/datapulse-frontend"
FRONTEND_REPO="${FRONTEND_REPO:-}"

echo "== DataPulse bootstrap =="
echo "Repo root:    $REPO_ROOT"
echo "Parent dir:   $PARENT"
echo "Frontend dir: $FRONTEND_DIR"

if [ ! -d "$FRONTEND_DIR" ]; then
  if [ -z "$FRONTEND_REPO" ]; then
    echo "ERROR: $FRONTEND_DIR missing and FRONTEND_REPO env not set."
    echo "       Either clone the frontend manually or export FRONTEND_REPO=<git-url> and re-run."
    exit 1
  fi
  echo "Cloning frontend from $FRONTEND_REPO..."
  git clone "$FRONTEND_REPO" "$FRONTEND_DIR"
else
  if [ -d "$FRONTEND_DIR/.git" ]; then
    echo "Frontend repo present — pulling latest..."
    (cd "$FRONTEND_DIR" && git pull --ff-only) || echo "WARN: git pull failed (non-fatal); continuing with on-disk copy."
  else
    echo "Frontend dir exists but is not a git repo — skipping pull."
  fi
fi

for f in Dockerfile nginx.conf .dockerignore; do
  if [ ! -f "$FRONTEND_DIR/$f" ]; then
    echo "ERROR: $FRONTEND_DIR/$f missing. Pull the latest frontend revision that includes the Docker build files."
    exit 1
  fi
done

DATASETS_DIR="$REPO_ROOT/datasets/output"
if [ ! -d "$DATASETS_DIR" ]; then
  echo "ERROR: $DATASETS_DIR missing."
  echo "       Copy the 8 seed CSVs into $DATASETS_DIR before running docker compose up."
  exit 1
fi

ENV_FILE="$REPO_ROOT/.env"
if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: $ENV_FILE missing. Copy .env.example to .env and fill in secrets."
  exit 1
fi

echo
echo "Bootstrap OK."
echo "Next: cd $REPO_ROOT && docker compose up -d --build"
