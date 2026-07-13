# tarefa futura: migrar sistema de cor pra textura 1D

## quando fazer
DEPOIS de estabilizar v44 (ou versão aprovada). Não misturar com fix visual.

## por quê
Hoje `emitVertex` faz ~15-20 ops float por vértice só pra decidir cor:
- comparações Y_LOW/MID/HIGH
- smoothstep + lerp RGB
- bloom, glitter, fade

Milhões de operações por segundo. Numa Radeon integrada 496MB isso pesa.

## como fazer (mantendo estrutura)
1. Gerar PNG 1x64 (largura 1, altura 64) com gradiente já baked:
   - pixel Y=0 → LOW laranja (0.78, 0.44, 0.15)
   - pixel Y=32 → MID amarelo (0.98, 0.78, 0.35)
   - pixel Y=64 → HIGH branco (1.00, 1.00, 0.95)
   - smoothstep já aplicado no baking
2. Registrar textura como resource: `assets/voiddim/textures/environment/cloud_gradient.png`
3. Bindar textura antes de renderizar nuvens
4. UV do vértice: U = qualquer (repeat), V = `clamp(vy / Y_HIGH, 0, 1)`
5. GPU interpola bilinear DE GRAÇA (hardware)
6. CPU só calcula: AO (mantém), UV.v (1 divisão)

## o que preservar
- AO por vértice (continua na CPU, é geometria)
- Estrutura Y_LOW/MID/HIGH (vira posição UV)
- Bloom nos topos (dá pra baked na textura junto)

## o que perde
- Glitter aleatório (perde. ninguém sente falta)
- Fade atmosférico por distância (substitui por fog engine do MC)

## ganho estimado
70-90% menos custo de shading. Diferença entre 30fps e 60fps olhando chão de nuvem.

## risco
Baixo. Estrutura de cor idêntica, só migra o CÁLCULO pra pré-computado.
Se não gostar, reverto — o filtro fica comentado, não deletado.
