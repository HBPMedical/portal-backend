#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <tag> [--push]" >&2
  exit 1
}

if [[ $# -lt 1 ]]; then
  usage
fi

TAG=""
PUSH=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --push)
      PUSH=true
      shift
      ;;
    -h|--help)
      usage
      ;;
    *)
      if [[ -n "$TAG" ]]; then
        echo "Unexpected argument: $1" >&2
        usage
      fi
      TAG="$1"
      shift
      ;;
  esac
done

if [[ -z "$TAG" ]]; then
  echo "Tag is required" >&2
  usage
fi

IMAGE="hbpmip/portal-backend:${TAG}"

echo "Building ${IMAGE}..."
docker build -t "${IMAGE}" .

if [[ "$PUSH" == true ]]; then
  echo "Pushing ${IMAGE}..."
  docker push "${IMAGE}"
fi
