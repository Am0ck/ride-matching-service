#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
IMAGE_NAME="ride-matching-service:latest"
CONTAINER_NAME="ride-matching-service"

echo "Checking Docker..."
if ! command -v docker >/dev/null 2>&1; then
  echo "Error: Docker is not installed or is not available on PATH." >&2
  exit 1
fi

echo "Removing an old container..."
docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true

echo "Building the image..."
docker build --tag "${IMAGE_NAME}" "${PROJECT_ROOT}"

echo "Starting the service..."
echo "Service URL: http://localhost:8080"
echo "Press Ctrl+C to stop the service."
exec docker run \
  --rm \
  --name "${CONTAINER_NAME}" \
  -p "8080:8080" \
  "${IMAGE_NAME}"
