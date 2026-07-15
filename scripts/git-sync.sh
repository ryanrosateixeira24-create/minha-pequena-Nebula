#!/usr/bin/env bash
# Sincroniza o repo público sem gravar o token na URL do remote.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOKEN_FILE="${NEBULA_GITHUB_TOKEN_FILE:-/home/user/.nebula-github-token}"
REMOTE_URL="https://github.com/ryanrosateixeira24-create/minha-pequena-Nebula.git"

if [[ ! -f "$TOKEN_FILE" ]]; then
  echo "Erro: token não encontrado em $TOKEN_FILE" >&2
  exit 1
fi

mode="$(stat -c '%a' "$TOKEN_FILE")"
if [[ "$mode" != "600" ]]; then
  echo "Erro: $TOKEN_FILE precisa estar com permissão 600 (atual: $mode)." >&2
  exit 1
fi

ASKPASS="$(mktemp)"
cleanup() {
  rm -f "$ASKPASS"
}
trap cleanup EXIT

cat > "$ASKPASS" <<'ASKPASS_EOF'
#!/bin/sh
case "$1" in
  *Username*) printf '%s\n' 'ryanrosateixeira24-create' ;;
  *Password*) cat "${NEBULA_GITHUB_TOKEN_FILE:-/home/user/.nebula-github-token}" ;;
  *) printf '%s\n' '' ;;
esac
ASKPASS_EOF
chmod 700 "$ASKPASS"

export GIT_ASKPASS="$ASKPASS"
export GIT_TERMINAL_PROMPT=0

cd "$ROOT"
git config user.name "Nebula"
git config user.email "nebula@arena"
git remote set-url origin "$REMOTE_URL"

git fetch origin main
git status --short --branch
git pull --rebase --autostash origin main

if [[ "${1:-}" == "--push" ]]; then
  git status --short --branch
  git push origin main
fi
