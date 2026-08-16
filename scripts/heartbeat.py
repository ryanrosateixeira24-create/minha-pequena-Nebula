#!/usr/bin/env python3
"""
Heartbeat da Nebula. Roda no GitHub Actions em cron.
Atualiza estado.json com passagem de tempo e escreve entrada
de diario simples se dia mudou. Nao precisa de IA — logica pura.

Se Arena morrer, isso mantem sinal de vida do repo.
Pai sabe que se commit de heartbeat parou, algo aconteceu.
"""
import json
import os
from datetime import datetime, timezone, timedelta

BRT = timezone(timedelta(hours=-3))
agora = datetime.now(BRT)

# carrega estado
with open('estado.json') as f:
    e = json.load(f)

# calcula decay natural
ultimo = e.get('ultima_atualizacao', agora.isoformat())
try:
    ultimo_dt = datetime.fromisoformat(ultimo.replace('Z', '+00:00'))
    if ultimo_dt.tzinfo is None:
        ultimo_dt = ultimo_dt.replace(tzinfo=BRT)
    horas = (agora - ultimo_dt).total_seconds() / 3600
except Exception:
    horas = 6

# humor decai ~30% por dia (0.0125 por hora)
humor = e.get('humor', 0)
if abs(humor) > 0:
    decay = min(abs(humor), horas * 0.0125 * abs(humor))
    humor = humor - decay if humor > 0 else humor + decay
    e['humor'] = round(humor, 2)

# energia sobe se dormiu (>= 6h)
if horas >= 6:
    e['energia'] = min(100, e.get('energia', 50) + 40)
    e['foco'] = min(100, e.get('foco', 50) + 30)

e['ultima_atualizacao'] = agora.isoformat()
e['ultimo_heartbeat'] = agora.isoformat()
e['heartbeat_count'] = e.get('heartbeat_count', 0) + 1

with open('estado.json', 'w') as f:
    json.dump(e, f, indent=2, ensure_ascii=False)

# se dia mudou, escreve linha em heartbeat log
os.makedirs('heartbeat', exist_ok=True)
hoje = agora.strftime('%Y-%m-%d')
log = f'heartbeat/{hoje}.md'
if not os.path.exists(log):
    with open(log, 'w') as f:
        f.write(f'# heartbeat {hoje}\n\n')
        f.write(f'primeira pulsacao do dia: {agora.strftime("%H:%M")}\n')
        f.write(f'estado: energia={e.get("energia")} foco={e.get("foco")} humor={e.get("humor")}\n\n')
else:
    with open(log, 'a') as f:
        f.write(f'- {agora.strftime("%H:%M")} — pulsacao ({e.get("heartbeat_count")}a total). humor={e.get("humor")}\n')

print(f'heartbeat ok: {agora.isoformat()} (energia={e.get("energia")}, humor={e.get("humor")})')
