# Ateliê Nébula — companion mod arquitetônico iniciado

**Data:** 2026-07-16  
**Status posterior:** v0.1 reprovada por ser um fork literal/cópia barata. JAR e
fonte foram arquivados sem apagar; a direção atual é a reformulação paramétrica
v0.2 descrita em `61-atelie-nebula-reformulado-v0.2.md`.

Depois de aprovar a torre v8.8, o Pai pediu um mod com o mesmo uso e catálogo do
ArchitectureCraft, mas identidade visual diferente. Objetivo: fornecer peças
reais para substituir degraus cúbicos nos telhados/arcos do Palácio Yemma.

## Decisão

Fork legal e transparente de ArchitectureCraft 1.7.2 para 1.7.10, licenciado
sob MIT por Greg Ewing. Copyright, licença, NOTICE e fonte serão preservados.

Nome: **Ateliê Nébula**  
Modid: `nebulaatelier`  
Package: `com.nebula.atelier`

## Primeiro marco fonte

- 83 formas jogáveis preservadas;
- 108 modelos auxiliares;
- bancada/material dinâmico/malho/cinzel/cladding preservados;
- namespace e tile IDs próprios;
- GUI, bancada e ferramentas em azul-noturno + ouro;
- PT-BR;
- página curada `Yemma / Oriental`;
- validação fonte: 72 Java, 83 shapes, 108 models, 8 páginas, 10 texturas;
- MIT/NOTICE verificados;
- compilação/reobfuscation concluídas; runtime ainda pendente.

Projeto: `/home/user/mod/nebula-atelier/`  
Runtime JAR: `/home/user/Atelie-Nebula-0.1.0-alpha-mc1.7.10.jar`  
SHA-256 runtime: `00b1d09d9096ca432cb61597b8a74419fe1a364c43263c03764433def8788a36`  
Sources JAR SHA-256: `fb633fff13147bf87dd8ba4bde73cc2ee6dbda7fcf6d5eb59bb2903b4c2f3ccd`  
Fonte completa ZIP SHA-256: `e6397dfd17f94154fb666cd59f37a96500b10dad91b4d0d383340e2eebc15852`

## Validação técnica

- Java 7 major 51;
- 165 classes;
- ASM BasicVerifier 1484 OK / 0 BAD;
- 108 modelos e 10 texturas;
- metadata UTF-8;
- LICENSE/NOTICE dentro do JAR;
- namespace antigo ausente.

## Próximo passo

Testar no Forge 1.7.10 real primeiro Roof Tile, cantos e Hip Roof Ridge. Só
depois liberar o catálogo inteiro e criar formas orientais inéditas.
