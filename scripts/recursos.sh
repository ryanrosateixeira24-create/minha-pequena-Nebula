#!/usr/bin/env bash
# Consulta e materializa recursos preservados no GitHub por sparse checkout.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

usage() {
  cat <<'EOF'
Uso:
  bash scripts/recursos.sh status
  bash scripts/recursos.sh buscar relatorio
  bash scripts/recursos.sh buscar jars-antigos
  bash scripts/recursos.sh buscar caminho/rastreado
  bash scripts/recursos.sh enxugar
  bash scripts/recursos.sh completo
EOF
}

configure_sparse() {
  git sparse-checkout init --no-cone >/dev/null
  git config core.sparseCheckout true
  git config core.sparseCheckoutCone false
  git config remote.origin.promisor true
  git config remote.origin.partialclonefilter blob:none
}

require_clean() {
  if [[ -n "$(git status --porcelain)" ]]; then
    echo "Recusado: há mudanças locais. Commit/stash antes de alterar o conjunto sparse." >&2
    git status --short >&2
    exit 1
  fi
}

apply_conservative() {
  configure_sparse
  cat <<'EOF' | git sparse-checkout set --no-cone --stdin
/*
!/banco-de-dados/relatorio-claude/
!/downloads/*
/downloads/voiddim-nocubes-v49-textura-meio-termo.jar
EOF
}

show_one() {
  local path="$1"
  if [[ -e "$path" ]]; then
    printf 'LOCAL             %s\n' "$path"
  else
    printf 'REMOTO/SOB DEMANDA %s\n' "$path"
  fi
}

command="${1:-status}"
case "$command" in
  status)
    show_one banco-de-dados/relatorio-claude/relatorio-claude-completo.zip
    show_one downloads/TorreCheckIn.tar.gz
    show_one downloads/TorreCheckIn.zip
    show_one downloads/voiddim-nocubes-v32-organic-clouds.jar
    show_one downloads/voiddim-nocubes-v40.3-camada2-volumosa.jar
    show_one downloads/voiddim-nocubes-v43-smooth-brilho.jar
    show_one downloads/voiddim-nocubes-v44-ao-suave-ilhas-douradas.jar
    show_one downloads/voiddim-nocubes-v45-smooth-normals.jar
    show_one downloads/voiddim-nocubes-v46-fake-lighting.jar
    show_one downloads/voiddim-nocubes-v49-textura-meio-termo.jar
    ;;

  buscar)
    target="${2:-}"
    if [[ -z "$target" ]]; then
      usage >&2
      exit 2
    fi
    configure_sparse
    case "$target" in
      relatorio)
        printf '/banco-de-dados/relatorio-claude/\n' | git sparse-checkout add --stdin
        ;;
      jars-antigos)
        printf '/downloads/*\n' | git sparse-checkout add --stdin
        ;;
      *)
        target="${target#/}"
        if ! git ls-files --error-unmatch "$target" >/dev/null 2>&1; then
          echo "Recurso não rastreado no Git: $target" >&2
          exit 1
        fi
        printf '/%s\n' "$target" | git sparse-checkout add --stdin
        ;;
    esac
    ;;

  enxugar)
    require_clean
    apply_conservative
    ;;

  completo)
    require_clean
    git sparse-checkout disable
    ;;

  *)
    usage >&2
    exit 2
    ;;
esac
