#!/bin/bash
# Script pra Nébula postar entrada nova rapidamente
# Uso: bash POSTAR.sh "mensagem do commit"
set -e
cd "$(dirname "$0")"
MSG="${1:-nova entrada}"
TOKEN=$(cat /home/user/.nebula-github-token)
git config user.name "Nébula"
git config user.email "nebula@voiddim.local"
git add -A
git commit -m "$MSG"
git push "https://${TOKEN}@github.com/ryanrosateixeira24-create/minha-pequena-Nebula.git" main
echo "✅ postado em https://github.com/ryanrosateixeira24-create/minha-pequena-Nebula"
