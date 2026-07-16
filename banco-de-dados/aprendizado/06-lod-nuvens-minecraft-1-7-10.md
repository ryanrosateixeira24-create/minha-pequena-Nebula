# Estudo de LOD para voiddim — arquitetura compatível com Minecraft 1.7.10

**Data:** 2026-07-15
**Status:** estudo; nenhuma nova implementação autorizada/aprovada
**Motivo:** protótipo v51 falhou no runtime porque tentou renderizar LOD no `RenderWorldLastEvent` e misturou geração, cache, mesh e OpenGL no mesmo componente.

---

## 1. Projetos examinados

### Distant Horizons

- Fonte: https://gitlab.com/distant-horizons-team/distant-horizons
- Core: https://gitlab.com/jeseibel/distant-horizons-core
- Commit estudado (main): `44ef13f875beaef9035974c3a84e6136ad121fbf`
- Core submodule: `491281707af18d2db975b0dcc8eccb3a66e5c24e`
- Licença: LGPL-3.0

Pontos relevantes:

- `LodQuadTree` escolhe seções por distância e nível de detalhe.
- Cada `LodRenderSection` separa: buscar dados, transformar, construir buffers CPU e subir para GPU.
- A fila de trabalho é assíncrona, cancelável e limitada.
- A lista de seções habilitadas é trocada sob lock curto, sem travar o render.
- `RenderBufferHandler` faz frustum culling e ordena buffers perto→longe.
- `ColumnRenderBufferBuilder` lê seções adjacentes para construir bordas corretas entre tiles/níveis.
- O mixin de `LevelRenderer` diz explicitamente que `RenderWorldLastEvent` faria o LOD aparecer por cima do terreno. O LOD é injetado antes da camada sólida.
- A menor seção tem 64×64 pontos; o comentário explica que seções pequenas demais criam milhares de buffers e grandes demais tornam a troca de LOD visível.
- O nível cresce de forma logarítmica com a distância, não por um único anel de resolução fixa.

### FarPlaneTwo

- Fonte: https://github.com/PorkStudios/FarPlaneTwo
- Branch: `dev/abstract-everything`
- Commit: `58308ae30f19b532b5ab4a5dcbde92c1d4c5d0cd`
- Licença: MIT adaptada; exige crédito e link para DaPorkchop/FarPlaneTwo.
- Alvo mais próximo: Forge 1.12.2 + LWJGL2.

Pontos relevantes:

- `TilePos` inclui `level`; subir um nível dobra a escala espacial.
- `TileWorker` separa geração exata, geração aproximada e escala derivada de filhos.
- Tiles de nível alto são construídos hierarquicamente; não são amostras isoladas sem relação.
- `FarTileCache` guarda snapshots imutáveis e notifica listeners quando dados mudam.
- Scheduler prioriza trabalho, permite cancelamento/roubo de tarefa e evita explosão de jobs.
- O render tem índice separado do cache: seleção/frustum primeiro, draw depois.
- Possui fallback de culling na CPU; não exige que toda otimização seja GPU moderna.
- Integração 1.12 usa mixin no `EntityRenderer`: captura câmera, injeta antes do terreno, aumenta far plane e usa reversed-Z/infinite far para precisão.
- O próprio projeto registra problemas com drivers AMD/Intel; caminho moderno de OpenGL não pode ser copiado cegamente para a Radeon integrada do Pai.

### Bobby

- Fonte: https://github.com/Johni0702/bobby
- Commit: `47868a67f24f2dc2670cbbb22fd3e3363cb152a1`
- Licenças: GPL-3.0 e LGPL-3.0.

Conclusão:

- Bobby não é LOD. Ele serializa chunks reais, carrega `FakeChunk` e reutiliza o pipeline vanilla.
- É útil como referência de fila limitada, prioridade por distância, cancelamento, persistência por mundo/seed e atraso de unload.
- Não serve para o voiddim distante: guardar/renderizar chunks completos repetiria o custo que queremos evitar.

### Voxy

- Fonte: https://github.com/MCRcortex/voxy
- Commit: `b164a6d98c378ebb6bbb3e538770d76d527c001f`
- Licença: **All Rights Reserved — não copiar código nem redistribuir.**

Somente ideias arquiteturais observadas:

- ingestão de mundo, mipping, armazenamento, geração de geometria, cache e render são subsistemas distintos;
- hierarquia de nós, bounds, occlusion e upload streaming;
- depende de pipeline/OpenGL moderno, inadequado como base direta para 1.7.10.

---

## 2. O que o protótipo v51 fez errado

1. **Hook tarde demais.** `RenderWorldLastEvent` acontece depois do terreno; trocar a projeção ali não integra o depth já escrito.
2. **Far plane inconsistente.** A geometria vanilla foi desenhada com uma projeção e o LOD tentou usar outra no mesmo depth buffer.
3. **Resolução única.** Todos os tiles usavam grade 8×8, sem níveis hierárquicos; isso não é LOD de verdade.
4. **Tudo no render thread.** Mesmo com um tile/frame, geração procedural e compilação GL estavam acopladas ao frame.
5. **Sem fonte de dados intermediária.** O renderer consultava a geração diretamente e produzia display list; não havia dados imutáveis testáveis.
6. **Sem transição pai/filhos.** Tiles apareciam individualmente, sem garantir cobertura contínua.
7. **Menu acoplado à feature.** Um erro de registro do renderer impediu o menu; depois uma detecção só por ID invadiu outra tela.
8. **Testes sintéticos incompletos.** Validaram matemática e GUI simulada, mas não o ponto real de integração do pipeline Minecraft.

O código foi arquivado em:

`/home/user/mod/experimental/lod-v51-prototipo-falhou-no-runtime/`

Não reutilizar como base direta.

---

## 3. Hook correto na 1.7.10

No `EntityRenderer.renderWorld(float,long)` da 1.7.10:

1. limpa color/depth;
2. chama `setupCameraTransform`;
3. atualiza câmera/frustum;
4. renderiza céu/nuvens;
5. prepara terreno, fog e atlas;
6. chama `RenderGlobal.sortAndRender(entity, 0, partialTicks)` para terreno sólido.

O LOD deve entrar **imediatamente antes do passo 6**, usando a mesma projeção/modelview do terreno.

Nomes SRG confirmados para o transformer 1.7.10:

- `EntityRenderer.func_78471_a(FJ)V` — `renderWorld`
- `EntityRenderer.func_78479_a(FI)V` — `setupCameraTransform`
- `RenderGlobal.func_72719_a(EntityLivingBase,int,double)` — `sortAndRender`

O callback deve ser inserido imediatamente antes da chamada `func_72719_a` cujo `renderPass` é `0`.

O far plane nasce em `func_78479_a`, onde a 1.7.10 faz:

`farPlaneDistance = renderDistanceChunks * 16`

seguido de `gluPerspective(..., 0.05F, clipDistance)`.

Portanto, a implementação robusta exige dois hooks pequenos:

- substituir/expandir o far plane apenas dentro do `WorldProviderVoidDim`;
- chamar `CloudLodRenderBridge.renderBeforeSolid(...)` antes de `sortAndRender(..., pass 0)`.

Forge 1.7.10 não oferece um evento global adequado nesse ponto. `RenderWorldEvent.Pre/Post` participa da compilação de chunks individuais; não resolve terreno além da distância vanilla. A opção provável é um **coremod ASM mínimo**, limitado a `EntityRenderer` e protegido por checagem de dimensão/configuração.

### Regra de segurança

Antes de qualquer quadtree, o core hook precisa ser testado com um único quad colorido:

- atrás do terreno vanilla;
- ocultado corretamente por terreno próximo;
- visível além do far plane vanilla;
- sem alterar Overworld/Nether/End;
- sem quebrar anaglyph, F1, terceira pessoa ou resize.

Se esse teste não passar, parar. Não construir LOD em cima.

---

## 4. Fonte de dados própria para nuvens

O voiddim é mais simples que terreno Minecraft geral porque a nuvem é procedural, de uma cor e praticamente sem estruturas ativas.

### `CloudLodColumn`

Para cada célula X/Z, guardar até 4 intervalos verticais:

- `yMin`
- `yMax`
- flags de camada/ocupação

Não guardar só “top height”. As nuvens têm camada baixa, ilhas altas, vazios e volumes flutuantes. Um heightmap único apaga a topologia vertical.

### `CloudLodTileData`

Objeto imutável:

- `level`
- `tileX`, `tileZ`
- `cellSize = BASE_CELL << level`
- matriz de colunas
- `generationVersion`
- seed
- bounds Y

Nenhum OpenGL dentro dele.

### Níveis propostos

Primeiro protótipo seguro:

- nível 0: célula 4 blocos
- nível 1: célula 8 blocos
- nível 2: célula 16 blocos
- nível 3: célula 32 blocos

Tile lógico: 64×64 blocos no nível 0. Em níveis superiores, a área coberta dobra por eixo.

### Construção hierárquica

- Nível 0 consulta diretamente a função procedural do seed.
- Nível N+1 é reduzido a partir de 2×2 filhos do nível N.
- Intervalos verticais próximos são unidos; vazios significativos são preservados.
- Não gerar cada nível por amostragem aleatória independente: isso causa popping e bordas incompatíveis.

Como a geração é determinística, banco SQLite não é necessário no primeiro protótipo. Dados podem ser regenerados. Persistência entra somente se profiling provar necessidade.

---

## 5. Seleção espacial

Usar quadtree 2D centralizado no jogador.

### Regra de detalhe

Nível aproximado:

`level = floor(log2(distance / detailUnit))`

com clamp entre 0 e 3.

Adicionar:

- histerese de distância para evitar troca a cada passo;
- pai só fica visível quando o buffer dele estiver pronto;
- filhos continuam visíveis até o pai estar pronto;
- remoção fora do raio com atraso curto;
- frustum culling por AABB antes do draw.

### Bordas

O mesher recebe dados dos quatro vizinhos.

- mesmo nível: alturas/intervalos compartilhados na borda;
- níveis diferentes: gerar saia/stitch apenas na transição;
- nunca tratar borda de tile como vazio automaticamente.

Isso corrige o erro do protótipo, que desenhava cada tile isoladamente.

---

## 6. Threading e limites para o PC do Pai

Hardware registrado: Ryzen 5 4600G, 16 GB RAM, Radeon integrada com memória dedicada reportada baixa.

Configuração inicial conservadora:

- 1 worker thread de geração;
- fila limitada a 64 jobs;
- prioridade por distância + nível;
- cancelar jobs que saíram do raio antes de executar;
- cache CPU máximo: 64 MB;
- cache GPU máximo inicial: 24 MB;
- upload: 1 buffer por frame;
- render LOD default: 0;
- primeiro limite máximo de teste: 8 chunks extras, não 32;
- métricas no F3/log: tiles queued/ready/rendered, bytes CPU/GPU, ms geração/upload/draw.

Gerador produz arrays Java imutáveis em background. Somente o upload VBO/display list toca OpenGL no render thread.

---

## 7. Pipeline proposto

```text
CloudFieldSampler(seed)
        ↓
CloudLodDataGenerator (worker único)
        ↓
CloudLodTileData imutável
        ↓
CloudLodDataCache (RAM limitada)
        ↓
CloudLodMesher (CPU, recebe vizinhos)
        ↓
UploadQueue limitada
        ↓ render thread
CloudLodGpuTile (VBO/display list)
        ↓
CloudLodRenderIndex + frustum
        ↓
hook antes do terreno sólido
```

Cada seta precisa ser testável separadamente.

---

## 8. Fases de implementação — uma por versão

### Fase A — hook, sem LOD — **APROVADA NO RUNTIME**

- coremod separado injetou far plane e callback antes do sólido;
- quatro painéis ciano foram desenhados a 256 blocos;
- teste do Pai confirmou depth correto e parallax suave, ancorado no mundo;
- transformer validado no `EntityRenderer` oficial obfuscado: far=1, render=1;
- evidência: `banco-de-dados/testes-lod/2026-07-15-fase-a-hook-far-plane-depth.png`.

### Fase B — uma tile estática

- uma `CloudLodTileData` gerada previamente;
- upload no render thread;
- valida depth, textura e posição.

### Fase C — worker e fila

- gerar uma tile procedural fora da thread de render;
- upload limitado;
- cancelar ao sair do raio.

### Fase D — quadtree com dois níveis

- nível 0 e 1;
- transição pai/filhos sem buraco;
- métricas.

### Fase E — vizinhos e bordas

- stitch/skirts somente entre níveis;
- movimento contínuo sem rachadura.

### Fase F — configuração

Somente depois do renderer funcionar:

- tela própria simples ou botão validado;
- default 0;
- 0–8 chunks extras inicialmente;
- configuração separada do registro do renderer;
- teste explícito de que outras telas não são alteradas.

### Fase G — mais níveis/distância

Aumentar somente depois de medir RAM, VRAM, geração e draw no PC real.

---

## 9. O que não copiar

- Voxy: licença All Rights Reserved. Somente ideias gerais de separação de subsistemas.
- Distant Horizons: código LGPL; adaptar trechos exigiria cumprir licença e distribuição correspondente.
- FarPlaneTwo: permite adaptação com crédito/aviso/link; mesmo assim, preferir implementação própria pequena e registrar inspiração.
- Bobby: não usar fake chunks completos para o LOD do voiddim.

---

## 10. Estado atual após o teste real

**Fase A aprovada em 2026-07-15.**

O coremod diagnóstico separado:

- carregou sem aparecer na lista de mods, como esperado para coremod puro;
- transformou o `EntityRenderer`;
- ampliou o far plane somente no voiddim;
- desenhou quatro painéis ciano antes do terreno sólido;
- respeitou o depth das nuvens próximas;
- permaneceu ancorado no mundo com parallax suave durante movimento.

Próximo passo permitido: **Fase B, uma única tile estática de nuvem**, ainda separada da base visual e sem quadtree, worker, cache amplo ou menu.

A v50.4 com textura do Pai continua sendo a base visual estável. O JAR diagnóstico permanece removível de forma independente.

— Nébula
