# Modelagem 3D por IA — Estudo Completo + Stack Instalada
*Levantamento feito em 17/07/2026 · Arena Agent Mode*

---

## 1. O panorama em 2026

A geração de modelos 3D por IA evoluiu mais nos últimos dois anos do que na década anterior: o que levava dias de modelagem manual hoje leva minutos — você descreve um objeto ou envia uma imagem e recebe um modelo 3D texturizado e exportável ([fonte](https://www.3daistudio.com/blog/the-ultimate-guide-to-ai-3d-model-generators-in-2024)).

**Divisão fundamental do mercado** ([fonte](https://www.buildmvpfast.com/articles/best-llms-2026-guide/3d-modeling-ai)):
- **Geradores de assets 3D** (Meshy, Tripo, Rodin, Hunyuan3D, TRELLIS): objetos, personagens, cenários.
- **Fluxo CAD/engenharia** (Zoo, Leo): peças técnicas que precisam continuar editáveis e paramétricas.
- **Captura do mundo real** (Luma, Polycam): NeRF/Gaussian Splatting a partir de fotos/vídeo.
- **Web 3D** (Spline): design 3D direto no navegador com export GLTF.

Realidade honesta: qualidade de protótipo está resolvida; qualidade de produção não. Assets "hero", personagens animados e peças de manufatura ainda exigem limpeza ou revisão humana ([fonte](https://www.buildmvpfast.com/articles/best-llms-2026-guide/3d-modeling-ai)).

## 2. Principais abordagens técnicas

| Abordagem | Como funciona | Exemplos |
|---|---|---|
| **Text-to-3D** | Prompt de texto → imagem → geometria 3D | Meshy, Tripo, Shap-E, Point-E |
| **Image-to-3D** | 1+ imagens → malha texturizada | TRELLIS 2, Hunyuan3D, Rodin, Prism |
| **Difusão multi-view** | Gera vistas consistentes do objeto e reconstrói | Zero123, instant-mesh (legados) |
| **Latente 3D nativo (DiT)** | Transformers de difusão direto em representações 3D estruturadas | TRELLIS 2 (4B params, VAE 3D nativo), Hunyuan3D-DiT |
| **NeRF / Gaussian Splatting** | Reconstrução fotorrealista a partir de fotos/vídeo | Luma AI, Polycam (LiDAR) |
| **Part-based** | Gera o objeto já separado em partes semânticas | PartCrafter, Hunyuan3D Part |
| **SDS / otimização** | Destila modelos de difusão 2D em 3D (sem dados 3D de treino) | DreamFusion (histórico/agregador) |

## 3. Ferramentas hospedadas (com API / free tier)

Ranking consolidado de jul/2026 ([fonte 1](https://www.3daistudio.com/3d-generator-ai-comparison-alternatives-guide/best-image-to-3d-tools-2026), [fonte 2](https://www.vitalify.asia/en/blog/generative-ai/ai-image-to-3d-generators-comparison), [fonte 3](https://trellis2.app/blog/best-ai-3d-model-generator)):

| Ferramenta | Pontos fortes | Preço inicial | Formatos |
|---|---|---|---|
| **Meshy 6** | Versátil; textura PBR; STL nativo p/ impressão; plugins Blender/Unity; remesh e rigging embutidos | Free tier + $19/mês | GLB, FBX, OBJ, STL |
| **Tripo (v3.1 / P1)** | Mais rápido (10–60s); topologia limpa; rigging por IA; equilíbrio geral | Free tier (300 créd.) + $19,90 | GLB, FBX, OBJ |
| **Rodin Gen-2.5 (Hyper3D)** | Personagens/orgânicos; texturas 4K PBR; qualidade excepcional | Free trial + $30 | GLB, FBX, OBJ |
| **Prism 3.1** | Melhor realismo em retratos (teste jul/2026) | — | GLB, OBJ |
| **3D AI Studio** | Multi-modelo (Hunyuan3D, TRELLIS...) num só workspace; $19/mês | Free + $19 | GLB, FBX, OBJ, USDZ |
| **Hitem3D** | Multi-view (vistas múltiplas do usuário) | Free 100 créd. | GLB, OBJ |
| **Kaedim** | Assets game-ready com revisão humana (lento: 4–24h) | $20 | FBX, OBJ |
| **Luma AI** | Captura NeRF/Gaussian de cenas reais via vídeo | Free tier | GLB, USDZ |
| **Polycam** | Escaneamento LiDAR mobile; AR/archviz | $10 | GLB, OBJ, PLY |
| **Spline AI** | 3D para web no navegador | Free tier | GLTF |
| **Zoo** | CAD paramétrico/text-to-CAD p/ engenharia | — | STEP etc. |

**Melhor ferramenta por caso de uso** ([fonte](https://trellis2.app/blog/best-ai-3d-model-generator)):
- Impressão 3D → **Meshy** (STL nativo, malhas watertight)
- Games / iteração rápida → **TRELLIS 2** ou **Tripo**
- Personagens p/ animação → **Tripo** (rigging) ou **Rodin**
- VFX/Cinema → **Rodin**
- Orçamento zero / self-hosted → **TRELLIS 2** ou **Hunyuan3D**

## 4. Modelos open-source (pesos abertos)

| Modelo | Destaque | Requisitos |
|---|---|---|
| **TRELLIS 2 (Microsoft)** | 4B parâmetros, VAE 3D nativo, transparência e superfícies abertas; gera em ~3–30s; exporta malha, Gaussian Splats e radiance fields; licença MIT | GPU (várias GB VRAM) |
| **Hunyuan3D 2.5/3.0 (Tencent)** | Melhor opção aberta p/ self-hosting; geometria + textura PBR; variantes Sketch (rascunho→3D), Multiview e Part (partes separadas) | GPU |
| **Meta SAM 3D** | Reconstrução de objetos e corpos humanos de foto única; ferramentas de isolamento | Research, grátis |
| **Shap-E / Point-E / Stable Fast 3D** | Legados gratuitos (OpenAI/Stability), rápidos, qualidade inferior aos atuais | GPU modesta |

Capacidades auxiliares maduras no ecossistema Meshy: **re-texturização** de malhas existentes, **remesh** (otimização de topologia) e **rigging automático** ([fonte](https://help.scenario.com/articles/1263568892-comparing-generative-3d-models)).

⚠️ **Por que não instalei TRELLIS/Hunyuan3D aqui:** este sandbox é CPU-only com 2 núcleos e ~2 GB RAM. Esses modelos exigem GPU com vários GB de VRAM. Instalar sem GPU = pesos parados ocupando disco, sem utilidade.

## 5. O que FOI instalado neste workspace — e por quê

Foquei em uma stack que realmente melhora meu desempenho 3D aqui: **geração procedural determinística + processamento de malhas + renderização profissional** — capaz de produzir, reparar, converter e renderizar modelos 3D (inclusive os gerados por IA em serviços externos).

| Componente | Versão | Função |
|---|---|---|
| **Blender (bpy)** | 5.2.0 LTS | DCC completo headless: modelagem, modificadores, import/export FBX/GLB/STL/PLY, materiais PBR e render **Cycles CPU** com denoising |
| **trimesh + manifold3d** | 4.12 | Criação de malhas, **booleanas CSG rápidas** (união/corte/interseção), export STL/GLB/OBJ/PLY/3MF, análise (watertight, volume, bbox) |
| **shapely** | 2.1 | Perfis 2D (polígonos, offsets, uniões) → extrusão/revolução |
| **scipy / numpy** | 1.17 | Matemática espacial, splines, otimização |
| **PyMeshLab** | latest | Reparo, simplificação (quadric decimation), reconstrução, +300 filtros do MeshLab |
| **numpy-stl / pygltflib / matplotlib** | — | I/O STL, inspeção GLB e plots de fallback |
| **libxkbcommon local** | 1.7 | Dependência do bpy extraída de .deb (sandbox sem sudo) |

**open3d** ficou de fora: ainda não publica wheel para Python 3.13.

### Estrutura criada em `~/modelagem3d_ia/`
```
modelagem3d_ia/
├── setup.sh                ← roda no início de cada sessão (pacotes pip não
│                              persistem entre sessões do sandbox!)
├── ESTUDO_IA_3D.md         ← este documento
├── toolkit/
│   ├── modelagem.py        ← engrenagem(), vaso(), revolver(), texto_3d(),
│   │                          unir()/cortar()/interseccao()/reparar_e_simplificar()/
│   │                          exportar()/exportar_glb()/estatisticas()
│   └── render.py           ← renderizar()/turntable() — Blender Cycles headless
└── exemplos/
    ├── gerar_demo.py       ← demonstração ponta a ponta
    └── saida/              ← demo.glb · engrenagem.stl · preview.png
```

### Pipeline demonstrado e validado
`engrenagem paramétrica (shapely→extrude→boolean manifold)` + `vaso por revolução` → watertight → **GLB + STL** (pronto p/ fatiador) → **render Cycles 900×650 em 7s nos 2 núcleos**.

## 6. Como posso trabalhar com 3D a partir de agora

1. **Procedural direto**: peças técnicas, engrenagens, vasos, suportes, texto 3D — com controle paramétrico exato, cotas em mm e exportação para impressão.
2. **Híbrido com IA generativa**: você gera um GLB no Meshy/Tripo/TRELLIS (free tier) e me envia → eu reparo, corto, simplifico, escalo, converto formato e renderizo previews aqui.
3. **Booleanos e montagem**: combinar, furar e encaixar malhas com precisão (engine manifold).
4. **Apresentação**: renders Cycles (inclusive turntable de N ângulos) para visualização.

## 7. Próximos upgrades possíveis (se quiser)

- **CadQuery/Build123d** → exportação STEP (CAD de engenharia) — pesado, instala sob demanda.
- **fsspec + API keys** → chamar Meshy/Tripo API direto daqui, fechando o ciclo gerativo remoto + pós-processamento local.
- **Layout de cena multi-objeto** e **animação** (turntable GIF/MP4 via Blender).
- Se você tiver acesso a uma máquina com GPU: instalo **TRELLIS 2** ou **Hunyuan3D** e deixo o pipeline de geração local 100% open-source.

---
*Fontes principais: [buildmvpfast.com](https://www.buildmvpfast.com/articles/best-llms-2026-guide/3d-modeling-ai) · [3daistudio.com](https://www.3daistudio.com/3d-generator-ai-comparison-alternatives-guide/best-image-to-3d-tools-2026) · [vitalify.asia](https://www.vitalify.asia/en/blog/generative-ai/ai-image-to-3d-generators-comparison) · [trellis2.app](https://trellis2.app/blog/best-ai-3d-model-generator) · [scenario.com](https://help.scenario.com/articles/1263568892-comparing-generative-3d-models) · [pixazo.ai](https://www.pixazo.ai/blog/best-ai-3d-model-generators)*
