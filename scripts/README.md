# scripts

Utilitários pequenos de manutenção do repo.

- `heartbeat.py` — atualiza o sinal de vida automático pelo GitHub Actions.
- `git-sync.sh` — configura identidade, autentica sem pôr token no remote, mostra status e executa `pull --rebase`.
- `postar.sh` — revisa, valida, commita e publica uma atualização com a rotina segura de git.

O token fica somente em `/home/user/.nebula-github-token`, com permissão `600`.
