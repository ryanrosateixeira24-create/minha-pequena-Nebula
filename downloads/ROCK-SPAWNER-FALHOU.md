# RockSpawner — STATUS: FALHOU (limitação técnica)

**Data:** 2026-08-10
**Tentativa:** adicionar pedras/montanhas flutuantes no voiddim
**Resultado:** ❌ NÃO FOI ENTREGUE

## O que foi tentado

Pai pediu pra adicionar ilhas rochosas (como na imagem do Snake Way baixo que ele mostrou) no voiddim, com:
- 1-3 pedras pequenas (seixos) por chunk
- ~30% dos chunks com pedra
- Bloco `minecraft:stone` (vanilla)
- Forma arredondada tipo seixo de rio

## Abordagem técnica

Criei `RockSpawner.java` que faz patch ASM no `ChunkProviderVoidDim`:

1. **Adiciona método novo** `generateRocks(Chunk, int, int)V` na classe
2. **Injeta chamada** no final do `generateOrganicClouds`
3. O método novo sorteia pedras com `mixHash` (que JÁ EXISTE) e usa `Blocks.stone`

## Por que FALHOU

Erro persistente:
```
java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
  at org.objectweb.asm.Frame.a(Unknown Source)
  at org.objectweb.asm.MethodWriter.visitMaxs(Unknown Source)
```

**Causa:** ASM 5.0.3 tem bug conhecido com `COMPUTE_FRAMES` em alguns cenários. O frame do novo método não consegue ser calculado corretamente. Adicionar LabelNode no início e fim não resolveu.

**Tentativas que NÃO resolveram:**
- Adicionar LabelNode inicial
- Adicionar LabelNode final
- Verificar ordem das instruções
- Verificar se vars long usam 2 slots

## O que PRECISA pra próxima tentativa

1. **ASM versão mais nova** (7.x ou superior tem frame computation melhor)
2. **Compilar Java real** (precisa de forgeSrc — que não temos no sandbox)
3. **Usar outro approach:** ao invés de adicionar método, injetar bytecode inline no `generateOrganicClouds` existente (mesma dificuldade de frame, mas menos)
4. **Pós-processamento manual dos frames** (calcular na mão qual é o frame de cada Label)

## Recomendação pra próxima Nébula

Não perder tempo tentando re-fazer com ASM 5.0.3. Pular pra uma dessas:

**Opção A:** Atualizar ASM pra 9.x (não sei se o sandbox permite)
**Opção B:** Usar bytecode em formato de string e gerar com `ASMifier` (tool do próprio ASM)
**Opção C:** Esperar conseguir forgeSrc e compilar .java real
**Opção D:** Outra Nébula já tem o método pronto em outro lugar

## Lição aprendida

NÃO tentar adicionar método novo em ASM 5.0.3 sem testar frame por frame.
Custa mais que o benefício. As constantes (v5/v6) foram seguras porque
só mudam o ConstantPool, não mexem em flow de código.

## Estado dos artefatos

- `RockSpawner.java` — código do patcher (não funciona)
- `RockSpawner.class` — compilado (não funciona)
- `voiddim-otimizado-v7-rocks.jar` — NÃO FOI GERADO (erro)

## Pra Nébula do futuro

Se tu for tentar de novo, o algoritmo está todo descrito no `RockSpawner.java`
no comentário grande. Só precisa resolver o problema de frame.
