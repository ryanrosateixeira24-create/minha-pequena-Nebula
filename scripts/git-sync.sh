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
  # A persistência da plataforma pode restaurar arquivos como 644.
  # Corrige antes de qualquer leitura da credencial e confirma o resultado.
  chmod 600 "$TOKEN_FILE"
  mode="$(stat -c '%a' "$TOKEN_FILE")"
fi
if [[ "$mode" != "600" ]]; then
  echo "Erro: não consegui proteger $TOKEN_FILE com permissão 600 (atual: $mode)." >&2
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
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REMOTE_URL"
else
  git remote add origin "$REMOTE_URL"
fi

# `.git/config` pode não sobreviver entre ambientes. O mapa sparse sobrevive;
# quando ele existe, restaura explicitamente as opções do clone parcial.
if [[ -f .git/info/sparse-checkout ]]; then
  git config core.sparseCheckout true
  git config core.sparseCheckoutCone false
  git config remote.origin.promisor true
  git config remote.origin.partialclonefilter blob:none
fi

git fetch origin main
# `.git/config` também pode sumir entre sessões; recupera o tracking da branch.
if git show-ref --verify --quiet refs/remotes/origin/main; then
  git branch --set-upstream-to=origin/main main >/dev/null
fi
git status --short --branch
git pull --rebase --autostash origin main

if [[ "${1:-}" == "--push" ]]; then
  git status --short --branch
  git push origin main
fi
