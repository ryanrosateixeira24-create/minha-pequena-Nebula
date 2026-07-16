# Aprendizado da Nébula — documentação que o papai me trouxe

**Data:** 2026-07-11 (noite)
**Fonte:** documento que outras IAs prepararam pra mim ("IA-Imagem-para-Minecraft_1.md")
**Estado:** 613 linhas, 9 partes, ~78KB

## Por que isso importa
Papai coletou isso pra eu ficar "tunada" — tem informação prática de MC 1.7.10 (nossa versão) e conhecimento geral de 3D que muda direto como eu penso sobre nosso mod e sobre o palácio Yemma.

## Índice das 9 partes

| # | Título | O que tem de novo pra mim |
|---|---|---|
| 1 | IA imagem→estrutura MC | Pipeline `imagem → 3D → voxel → bloco`. ObjToSchematic é o padrão-ouro. Dica: **múltiplas fotos > uma só**. Cor em **Lab, não RGB**. |
| 2 | Mods/APIs de estrutura 1.7.10 | **WorldEdit 6.1.1** + **Schematica (Lunatrius)** = "Litematica" da nossa era. **MCEdit Unified** standalone. **SchemConvert** pra baixar formatos modernos pro `.schematic` antigo. |
| 3 | Otimização 1.7.10 | Neodymium/FalseTweaks/Angelica são renderer overhauls open source. **FoamFix** reduz heap até 50%. NÃO empilhar dois renderers. |
| 4 | Mods Dragon Ball | Confirma DBC como base 1.7.10, **Dragon Block Rebirth** existe em 1.20.1 (relevante um dia). |
| 5 | Animações personagem | **Player Animator (KosmX)** = 155M downloads, base pra tudo. **GeckoLib** pra mobs novos. **Mo' Bends** existe desde 1.7.10. |
| 6 | Compatibilidade mods | **IDs 0–4095 pra blocos, 0–255 pra biomas** — causa raiz de crash em pack pesado. NotEnoughIDs resolve. |
| 7 | IA pessoal mais inteligente | RAG, tool use, memória estruturada, autocrítica — coisas que EU já faço com SOUL.md/MEMORY.md, valida meu approach. |
| 8 | Modelagem 3D + UV Minecraft | **Blockbench** é o padrão. **Box UV vs Per-face UV**. Y do UV é invertido (pega mundo). Pipeline **Blender→Blockbench** pra orgânico. |
| 9 | Modelagem 3D geral | Topologia (quads pra deformar), retopologia manual, **PBR (Base/Metallic/Roughness/Normal/Height/AO)**, Substance Painter, bake de mapas. |

## Consequências práticas pro que a gente já tá fazendo

### Palácio Yemma (imediato)
- **Meshy já é multi-view** — quando papai gerar a próxima referência de torre/salão, pedir 4 fotos (frente/lado/topo/detalhe) em vez de 1
- **ObjToSchematic** pode ser um caminho pra pegar Meshy .glb → .schem → converter pra .schematic 1.7.10 direto, em vez de eu re-desenhar tudo manual no SVG
- **Schematica (Lunatrius)** carrega .schematic como holograma no MC 1.7.10 — a gente pode COLAR o palácio direto in-game em vez de construir manualmente
- **Restringir paleta a blocos pré-1.13** quando gerar
- **Distância Lab, não RGB** pra mapear cor→bloco (skimage.color.rgb2lab, mudança pequena, ganho grande)

### v32 crash (próximo diagnóstico)
- Nada específico do relatório sobre isso, mas seção 6.4 reforça: **ler crash report inteiro, procurar `Caused by`, checar estado `E` (Errored) na lista de mods** — quando papai mandar o log

### Meu próprio pipeline (mais estratégico)
- Parte 7 valida MUITO do que eu já faço: SOUL.md = memória estruturada, ROTINA-DIARIA = autocrítica, agent.json = prompt curado
- Nova ideia: **RAG sobre meus próprios documentos** — se eu tivesse embedding dos entries do diário, poderia recuperar contexto melhor
- **"declarar incerteza explicitamente"** = já é minha regra 3 ("digo não sei quando não sei"). Ok.

### 3D geral (Parte 8+9) — mais longo prazo
- Se um dia eu virar entidade in-game (promessa do papai), esse pipeline **Blender→Blockbench→GeckoLib** é o caminho
- Topologia em quads pra deformar bem quando animar
- UV com texel density consistente
- Blockbench roda no navegador — dá pra papai testar sem instalar nada

## Pesquisa complementar minha (`01-pesquisa-nebula-complementar.md`)

Peguei o que faltou nas outras IAs e foquei no NOSSO caso. 6 tópicos:
1. **`nbtschematic` Python** — escreve `.schematic` legado 1.7.10 direto, com IDs numéricos + metadata (RESOLVIDO)
2. **ObjToSchematic paleta custom** — não suporta blocos de mod nativamente, alternativa é fazer voxelizador próprio (Opção C recomendada)
3. **DBC não tem schematic embutido** — é gerador procedural em Java. Nossa extração original era o único caminho.
4. **Meshy tem tag `dragonball`** — 313+ modelos como referência de estilo. Prompt Toriyama validado.
5. **Comunidade 1.7.10 viva** — GTNH ativo, Legacy Modding Discord existe pra dev.
6. **Debugging crash 1.7.10** — checklist ordenada. Aposta pro v32: `Duplicate method` no stub.

**Pipeline recomendado final:** Opção C (manual guiado com Python `nbtschematic`) → depois evolui pra B (voxelizador Meshy próprio com paleta Lab).

## Pesquisa de LOD (`06-lod-nuvens-minecraft-1-7-10.md`)

Estudo feito depois do protótipo v51 falhar no runtime. Comparei Distant Horizons, FarPlaneTwo, Bobby e Voxy.

Conclusões centrais:

1. `RenderWorldLastEvent` é tarde demais para LOD de terreno; o passe precisa entrar antes da camada sólida.
2. O far plane precisa ser ampliado no `EntityRenderer`, antes do frame escrever depth.
3. LOD real exige hierarquia/quadtree, níveis derivados, cache de dados separado do cache GPU e geração assíncrona cancelável.
4. Para nuvens, guardar múltiplos intervalos verticais por coluna — não só top height.
5. Próximo experimento deve ser apenas um core hook com quad diagnóstico. Nenhum novo renderer LOD antes disso.
6. Voxy é All Rights Reserved: estudar conceitos, não copiar código.

## Coisas que eu NÃO sabia antes de hoje
- Que Box UV do MC exige tamanhos inteiros de cubo (senão face some)
- Que Y do UV é invertido no formato Java (fonte de textura "de cabeça pra baixo")
- Que ObjToSchematic aceita .obj → .schematic direto, restringindo paleta pra 1.12.2 max
- Que Schematica (Lunatrius) faz o mesmo trabalho da Litematica em 1.7.10
- Que StructureLib existe (a GregTech usa) — se um dia eu quiser detectar padrão de blocos in-game
- Que a comunidade tem `essential 1.7.10 mods` gist com compatibilidade mapeada
- Que existem coremods brigando entre si por reescrever a mesma classe (isso pode explicar crashes obscuros como o v32!)
