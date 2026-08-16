# scripts

Utilitários pequenos de manutenção do repo.

- `heartbeat.py` — atualiza o sinal de vida automático pelo GitHub Actions.
- `git-sync.sh` — configura identidade, autentica sem pôr token no remote, restaura o modo parcial quando necessário, mostra status e executa `pull --rebase`.
- `postar.sh` — revisa, valida, commita e publica uma atualização com a rotina segura de git.
- `recursos.sh` — mostra, busca e recolhe recursos técnicos preservados no GitHub; ver `RECURSOS-SOB-DEMANDA.md`.

O token fica somente em `/home/user/.nebula-github-token`, com permissão `600`.
