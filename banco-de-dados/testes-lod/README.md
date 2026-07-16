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
