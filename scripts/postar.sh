#!/usr/bin/env bash
# Revisa, commita, sincroniza e publica mudanças do repo público.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MESSAGE="${1:-}"

if [[ -z "$MESSAGE" ]]; then
  echo "Uso: bash scripts/postar.sh \"resumo do dia em uma linha\"" >&2
  exit 2
fi

cd "$ROOT"
bash scripts/git-sync.sh

echo "=== mudanças antes do commit ==="
git status --short
git diff --stat
git diff --check

git add -A
if git diff --cached --quiet; then
  echo "Nada para publicar."
  exit 0
fi

# Bloqueia prefixos comuns de token em arquivos de texto preparados para commit,
# sem imprimir o segredo na tela.
python3 - <<'PY'
import re
import subprocess
import sys

pattern = re.compile(rb'(?:github_pat_|gh[pousr]_)[A-Za-z0-9_]{12,}')
raw = subprocess.check_output([
    'git', 'diff', '--cached', '--name-only', '-z', '--diff-filter=ACMR'
])
problems = []
for item in raw.split(b'\0'):
    if not item:
        continue
    path = item.decode('utf-8', 'surrogateescape')
    try:
        data = subprocess.check_output(['git', 'show', ':' + path])
    except subprocess.CalledProcessError:
        continue
    if b'\0' not in data and pattern.search(data):
        problems.append(path)
if problems:
    print('Commit bloqueado: possível token GitHub em arquivo rastreado:', file=sys.stderr)
    for path in problems:
        print('  ' + path, file=sys.stderr)
    sys.exit(1)
PY

git diff --cached --check
git diff --cached --stat
git commit -m "$MESSAGE"

# A rotina exige novo status + pull --rebase imediatamente antes do push.
bash scripts/git-sync.sh --push
