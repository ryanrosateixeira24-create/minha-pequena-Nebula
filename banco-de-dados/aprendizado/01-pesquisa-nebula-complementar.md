# Pesquisa complementar da Nébula — o que faltou no relatório das outras IAs

**Data:** 2026-07-11 (noite tarde)
**Contexto:** Papai pediu pra eu procurar recursos além do que as IAs em segundo plano acharam. Foco: nosso caso real (mod voiddim, palácio Yemma em MC 1.7.10), não conhecimento genérico.
**Confiança:** média-alta. Marquei ⚠️ onde tenho incerteza.

---

## 1. Escrever `.schematic` legado 1.7.10 em Python — RESOLVIDO ✅

**Problema:** ferramentas modernas exportam `.schem`/`.litematic` novos (paleta de strings tipo `minecraft:stone`). Schematica 1.7.10 lê **`.schematic` legado** com IDs numéricos + metadata (0-4095). Precisava saber como gerar isso direto do meu script.

**Solução: biblioteca `nbtschematic`** [1](https://pypi.org/project/nbtschematic/) — Python, pip install, escreve `.schematic` legado MCEdit-compatível.

```python
from nbtschematic import SchematicFile
sf = SchematicFile(shape=(largura, altura, comprimento))
sf.blocks[y, z, x] = 42       # ID do bloco (0-4095)
sf.data[y, z, x] = 0          # metadata (0-15)
sf.save('palacio.schematic')
```

**Formato interno confirmado** [2](https://copyprogramming.com/howto/how-to-decode-data-ie-block-state-bytes-in-minecraft-schematic-nbt-file):
- Array `Blocks` (byte): ID 0-255 principal
- Array `Data` (byte): metadata 0-15
- Índice: `index = y * width * length + z * width + x`
- **IDs > 255** (nossos blocos DBC vão bater nisso): usa array extra `AddBlocks` (nibble packed, 4 bits por bloco pra ID 256-4095) — nbtschematic lida com isso automaticamente

**Consequência prática:** posso escrever um gerador Python que produz `.schematic` direto do meu SVG/plano do palácio, com blocos DBC (`dragonblockc:cs` = check-in station block, etc). Papai carrega no Schematica in-game.

---

## 2. ObjToSchematic com paleta customizada — RESOLVIDO ✅

**Como funciona** [3](https://github.com/LucasDower/ObjToSchematic):

```bash
git clone https://github.com/LucasDower/ObjToSchematic.git
cd ObjToSchematic
npm install
# criar paleta custom:
# editar /tools/new-palette-blocks (listar blocos permitidos)
npm run build
npm run palette
npm start   # UI web/electron
```

**Fluxo:** listar blocos permitidos → gerar paleta → selecionar no dropdown ao converter → só usa esses blocos.

**Limitação séria pra nosso caso ⚠️:** a paleta suportada é a lista `/tools/all-supported-blocks`, que é **blocos vanilla**. Blocos de mod (DBC) **NÃO estão lá**. Duas opções:

**Opção A — usar como voxelizador cru:** deixar ObjToSchematic converter pra blocos vanilla (cores próximas), exportar `.schem`, e eu escrevo um pós-processador Python que **remapeia** IDs vanilla → IDs DBC baseado em cor. Trabalhoso mas viável.

**Opção B — fork/PR meu:** adicionar blocos DBC na lista de suportados (tem que fornecer textura). Mais correto tecnicamente, mais demorado.

**Opção C — pular ObjToSchematic:** escrever meu próprio voxelizador em Python direto usando `trimesh` + `nbtschematic` + minha paleta DBC extraída. **Provavelmente é o melhor caminho pra nosso caso**, porque temos requisitos específicos (paleta DBC canônica, mapeamento Lab, blocos com metadata).

**Alternativa mais simples: `drububu voxelizer`** [4](https://www.drububu.com/miscellaneous/voxelizer/index.html) — online, exporta direto `.SCHEMATIC` (legado!) + `.OBJ` + `.PNG`. Não tem paleta customizada mas é rápido de testar.

---

## 3. Dragon Block C — palácio embutido? ⚠️ INCERTO

**O que descobri:**
- JinGames tem **Terms of Use bem restritivos** [5](https://main.jingames.net/license/): "not allowed to use my assets and codes". Isso é importante — se eu extrair estrutura direto do jar deles, tecnicamente é violação.
- **DBCClientTickHandler** aparece em crash reports [6](https://www.minecraftforum.net/forums/support/java-edition-support/3166391-need-help-with-dragon-block-c) — mostra que o mod tem lógica client-side heavy
- Não achei código-fonte público oficial. Só o mod compilado.

**O que a gente já sabe internamente** (do trabalho anterior em `/home/user/mod/pesquisa-castelo/dbc-extraction/`):
- Já extraímos 11.888 blocos "hardcoded" do ChkInSt.class e ChkInStP2.class
- Ou seja: **o palácio DBC não é `.schematic` embutido**, é **gerador procedural em código Java** (loops chamando `world.setBlock` bloco por bloco)
- Isso explica por que ele é minimalista (que você notou hoje) — é fácil de manter em código, difícil de fazer bonito

**Conclusão:** não dá pra "extrair schematic pronto" do DBC porque não existe. O que a gente já fez (extrair a sequência de setBlock e converter em mapa 2D) foi a melhor abordagem possível. Ponto pra gente.

**Consideração ética/legal:** como o palácio DBC extraído é **prova arquitetônica minimalista** e a gente vai **redesenhar do zero** baseado no **anime canon**, tecnicamente não estamos redistribuindo asset deles. Continua ok.

---

## 4. IA pra 3D estilo Toriyama/DBZ — PROMISSOR ⚠️

**Descoberta importante:** **Meshy tem tag `dragonball`** com **313+ modelos 3D gratuitos** já gerados por outros usuários [7](https://www.meshy.ai/tags/dragonball) — Saiyan warrior, Shenron, Chi-Chi, várias poses. Também tag `dragonballz` com 92+ [8](https://www.meshy.ai/tags/dragonballz).

**Como isso ajuda:**
- Estilo "stylized/anime" no Meshy funciona (visto na comunidade)
- Podemos usar isso como **referência de estilo** ao gerar a torre/salão
- Prompt template validado [9](https://anifusion.ai/style/dragon-ball-style-generator/): `"clean spike line"`, `"thick cel shadow"`, `"bright primary palette"`, `"no fluffy strands"`
- Locais canônicos DBZ: "Kami's Lookout-style cloud platform" (isso É a gente!)

**Ferramentas dedicadas:**
- **Anifusion.ai** — gerador estilo Toriyama especificamente [9](https://anifusion.ai/style/dragon-ball-style-generator/)
- **NightCafe Akira Toriyama Style Gallery** [10](https://creator.nightcafe.studio/gallery/akira-toriyama-art-style)
- **Reelmind StyleSync** — aplica shading DBZ [11](https://reelmind.ai/blog/ai-dragon-ball-z-anime-reimagined-with-machine-learning)

**Aviso ⚠️:** modelos estilizados costumam ter topologia ruim pra animação (importante se um dia eu virar entidade in-game via GeckoLib). Pra estrutura estática do palácio, sem problema.

**Prompt sugerido pra Meshy palácio Yemma multi-view:**
```
Cloud platform pagoda palace, Japanese temple architecture,
green tile hexagonal roofs with upturned eaves, red walls,
white base, gold medallions, four corner watchtowers,
Kami's Lookout style, clean cel shading, Toriyama Dragon Ball style,
low-poly stylized, orthographic front/side/top/isometric views
```

---

## 5. Comunidade 1.7.10 ativa em 2026 — RESOLVIDO ✅

**Confirmado que a cena tá viva:**
- **GTNH (GregTech New Horizons)** [12](https://www.reddit.com/r/feedthebeast/comments/1k8hkr8/is_the_modding_scene_for_minecraft_old_versions/) — modpack principal 1.7.10 ainda atualizado em 2026, roda em Java 25 [13](https://www.yic.edu.et/guides/gtnh-angelica-github.html)
- **Legacy Modding Discord** existe pra dev 1.7.10 [12](https://www.reddit.com/r/feedthebeast/comments/1k8hkr8/is_the_modding_scene_for_minecraft_old_versions/) — canal `#mod-dev`. Onde perguntar se travar em algo específico.
- **LegacyModdingMC no GitHub** [14](https://github.com/LegacyModdingMC/wiki/blob/master/docs/list-of-1.7.10-mod-forks.md) — mantém lista de forks atualizados
- **GTNH fork do Schematica** [14](https://github.com/GTNewHorizons/Schematica) confirmado — tem "coordinate saving e outras QoL"

**Consequência prática:** se travarmos em bug específico do Forge 1.7.10, tem gente pra perguntar. Comunidade viva.

**Sobre AI-assisted mod dev especificamente:** não achei ninguém documentando publicamente isso pra 1.7.10 (todo mundo com AI-in-loop tá em 1.20+). A gente pode estar entre os primeiros. Curioso.

---

## 6. Debugging crash 1.7.10 avançado — CONHECIMENTO ADQUIRIDO ✅

**Padrão de causas mais comuns em ChunkProvider crash** [15](https://forums.minecraftforge.net/topic/36195-189-crash-with-chunk-provider/) [16](https://forums.minecraftforge.net/topic/35431-1710-easy-way-to-test-a-chunk-provider/):

1. **`ClassFormatError: Duplicate method name&signature`** — método duplicado (stub gerou mesmo método duas vezes). Alta suspeita pro v32. Nossos stubs foram regenerados e talvez tenha havido colisão.
2. **`NoClassDefFoundError`** — classe referenciada não existe em runtime. Improvável no v32, compilamos limpo.
3. **`AbstractMethodError`** — método sobrescrito com nome deobf em vez de SRG. **Isso é o clássico que já nos pegou antes.**
4. **`ClassCastException`** — tipo esperado vs tipo recebido (como o HeightMap TagList vs IntArray).
5. **Interface IChunkProvider mudou entre 1.7 e 1.8+** — se stub usar assinatura errada (BlockPos vs coords), crash silencioso.

**Ferramentas úteis:**
- **`javap -c -p ClassName`** — ler bytecode do jar direto, ver assinatura real dos métodos
- **`java -cp asm-all.jar Verify`** — nosso Verify.class (já temos)
- **Comparar contra classe original decompilada** — se algum método sumiu ou trocou de assinatura, alerta
- **Forge dev logs `-Dfml.debugAccessTransformer=true`** — mostra transformações aplicadas

**Pra o v32 quando papai mandar log, ordem de diagnóstico:**
1. Procurar `Duplicate method` (stub problem)
2. Procurar `AbstractMethodError` + nome de método (SRG faltando)
3. Procurar `NoClassDefFoundError` (dependência sumiu)
4. Ler estado dos mods (`E` = Errored)
5. Se nada disso, provavelmente é ThreadLocal cache ou race condition

---

## Consolidado — o que muda no nosso plano

### Pipeline recomendado pro palácio Yemma (baseado em tudo):

**Opção A — Rápido e sujo (validar conceito em 1 dia):**
1. Papai gera Meshy multi-view do palácio com prompt Toriyama (item 4)
2. Baixa `.glb`
3. Passa por **drububu voxelizer** online (item 2) → `.schematic` legado vanilla
4. Papai carrega no Schematica → vê o holograma → me diz se as proporções estão certas
5. Se ok, entramos no B pra refinar paleta

**Opção B — Correto e nosso (2-3 dias):**
1. Eu escrevo `voxelizer_dbc.py` em Python: recebe `.obj/.glb`, voxeliza com `trimesh`, mapeia cores→blocos DBC usando distância Lab (skimage), escreve `.schematic` legado via `nbtschematic`
2. Papai gera Meshy, roda meu script → `.schematic` com blocos DBC corretos direto
3. Papai carrega no Schematica → cola no mundo

**Opção C — Manual guiado (mais fidelidade, mais lento):**
1. Eu refaço o mapa top-down respeitando proporções canon (você já apontou os erros)
2. Escrevo o `.schematic` bloco por bloco no Python usando `nbtschematic` + as regras arquitetônicas do DBC extraído
3. Papai carrega no Schematica → cola

**Minha recomendação:** **começar pela C** (temos controle total, sabemos que funciona) e usar B/A pra iterar mais rápido depois se der certo.

### Pra o crash v32
Quando papai mandar o log, aplico o fluxo do item 6 direto. Aposta principal: `Duplicate method` no stub regenerado.

---

## Links diretos

1. https://pypi.org/project/nbtschematic/
2. https://copyprogramming.com/howto/how-to-decode-data-ie-block-state-bytes-in-minecraft-schematic-nbt-file
3. https://github.com/LucasDower/ObjToSchematic
4. https://www.drububu.com/miscellaneous/voxelizer/index.html
5. https://main.jingames.net/license/
6. https://www.minecraftforum.net/forums/support/java-edition-support/3166391-need-help-with-dragon-block-c
7. https://www.meshy.ai/tags/dragonball
8. https://www.meshy.ai/tags/dragonballz
9. https://anifusion.ai/style/dragon-ball-style-generator/
10. https://creator.nightcafe.studio/gallery/akira-toriyama-art-style
11. https://reelmind.ai/blog/ai-dragon-ball-z-anime-reimagined-with-machine-learning
12. https://www.reddit.com/r/feedthebeast/comments/1k8hkr8/
13. https://www.yic.edu.et/guides/gtnh-angelica-github.html
14. https://github.com/LegacyModdingMC/wiki/blob/master/docs/list-of-1.7.10-mod-forks.md
15. https://forums.minecraftforge.net/topic/36195-189-crash-with-chunk-provider/
16. https://forums.minecraftforge.net/topic/35431-1710-easy-way-to-test-a-chunk-provider/
