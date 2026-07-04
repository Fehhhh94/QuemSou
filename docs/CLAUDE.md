# QuemSou — Guia do projeto para o Claude

## Histórico de versões

- **v1** — esqueleto do documento (Fase 0).
- **v2** (2026-07-04) — URL do GitHub, Fase 0 registrada como concluída, decisão
  "leitor pontua" resolvida, estado da Fase 1.

## Visão geral

- Party game Android offline: um leitor lê até 10 dicas de um card e os demais
  jogadores tentam adivinhar a resposta; quanto menos dicas, mais pontos.
- Repositório: https://github.com/Fehhhh94/QuemSou
- Stack: Kotlin 2.1.0, Jetpack Compose (Material 3), Hilt (KSP), Room,
  Navigation Compose com rotas tipadas (`@Serializable`), Clean Architecture + MVVM.
- Pacote raiz `com.quemsou.app`. Código Kotlin em `app/src/main/kotlin/`
  (não `src/main/java/`); testes JVM em `app/src/test/kotlin/`.

## Estado do projeto

- **Fase 0 — esqueleto** (Gradle, Hilt, navegação, 4 telas placeholder):
  concluída — commit `f544a09`.
- **Fase 1 — domínio puro** (modelos, seed/embaralhamento determinísticos,
  pontuação, testes JVM): concluída nesta atualização.

## Decisões de design

- **Leitor pontua** — RESOLVIDA: configurável em `RegrasPartida.leitorPontua`,
  padrão SIM; quando ativo, o leitor ganha os **mesmos** pontos do acertador.
- **Destino do card queimado** — EM ABERTO: não criar campo em `RegrasPartida`
  até a decisão ser fechada.
- **Determinismo é sagrado**: seed e embaralhamento não podem usar `hashCode()`
  da plataforma, `kotlin.random.Random` nem `java.util.Random`. As
  implementações próprias vivem em `domain/rules/` (hash polinomial +
  xorshift64). A mesma seed precisa gerar o mesmo baralho em qualquer aparelho,
  para sempre — é a base do multiplayer offline por código de partida.

## Documentação — quem é dono do quê

- `docs/GAME_RULES.md` — dono das regras do jogo.
- `docs/CHANGELOG.md` — mudanças notáveis por fase.
- `docs/BUGS.md` / `docs/IMPROVEMENTS.md` — bugs conhecidos e melhorias futuras.
- `docs/CARDS_GUIDE.md` — guia de criação de cards.

## Fluxo de trabalho

- Domínio (`domain/`) é Kotlin puro: sem Android, Room ou Compose — testável na JVM.
- KDoc e commits em português, formato `tipo: descrição`.
- Rodar `./gradlew test` antes de commitar. No Windows, o Gradle precisa de
  JDK 17+ — usar o JBR do Android Studio
  (`JAVA_HOME = C:\Program Files\Android\Android Studio\jbr`).
- **Push é sempre manual do Felipe** — nunca fazer `git push`.
