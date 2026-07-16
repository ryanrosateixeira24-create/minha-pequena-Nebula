# testes LOD

Evidências visuais das fases incrementais do LOD do voiddim.

## Fase A — 2026-07-15

`2026-07-15-fase-a-hook-far-plane-depth.png`

Coremod diagnóstico separado da v50.4:
- transformou `EntityRenderer` oficial 1.7.10;
- ampliou far plane para 1024 apenas no voiddim;
- inseriu callback antes do terreno sólido;
- desenhou quatro painéis ciano a 256 blocos.

Resultado informado pelo Pai:
- painéis visíveis;
- terreno/nuvens respeitam depth;
- ao andar, os painéis se movem com parallax suave e ficam ancorados no lugar correto.

Conclusão: hook, far plane, depth e coordenadas de mundo aprovados. Próxima fase: uma tile estática de nuvem, sem quadtree.

## Fase B — 2026-07-15

`2026-07-15-fase-b-tile-estatica-texturizada.png`

O painel leste foi substituído por uma elipse low-poly texturizada a 256 blocos.

Resultado informado pelo Pai:
- tile visível e texturizada;
- parallax/depth funcionando;
- tile "teleporta" ao cruzar setores de 64 blocos.

O teleporte é esperado no diagnóstico: a posição foi ancorada em `floor(player/64)*64` para permanecer perto. Não existe identidade/cache mundial de tiles ainda.

Conclusão: pipeline de geometria texturizada distante aprovado. A Fase C precisa introduzir coordenada mundial permanente, worker/fila limitada e troca sem buraco.

## Fase C — 2026-07-16

`2026-07-16-fase-c-worker-cache-quatro-tiles.png`

Pipeline assíncrono com tile mundial fixa:
- worker daemon único;
- fila de capacidade 1;
- dados CPU imutáveis;
- upload máximo de 1 display list por frame;
- cache GPU máximo de 4 tiles.

Resultado informado pelo Pai:
- tiles deixaram de teleportar como um único objeto;
- novas tiles apareceram conforme cruzava setores;
- o total chegou a 4;
- ao entrar uma nova, a mais antiga sumiu somente depois da substituta aparecer.

Conclusão: identidade mundial, geração assíncrona, upload limitado e eviction pós-substituição aprovados. Visual provisório parece "pãezinhos celestiais".
