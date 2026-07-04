# QuemSou — Guia do projeto para o Claude

## Histórico de versões

- **v1** — esqueleto do documento (Fase 0).
- **v2** (2026-07-04) — URL do GitHub, Fase 0 registrada como concluída, decisão
  "leitor pontua" resolvida, estado da Fase 1.
- **v3** (2026-07-04) — revisão de arquitetura: multiplayer via Nearby
  Connections + decisões de produto fechadas (jogadores 2–4, individual/times,
  placar em todos, card queimado descartado).
- **v4** (2026-07-04) — Fase 2 (parte técnica) concluída: Room + importador de
  cards versionado + campo `descartarCardQueimado` + `org.gradle.java.home`.
  Cards definitivos (trabalho editorial) pendentes; decisão "Livre" em aberto.

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
  pontuação, testes JVM): concluída — commit `5227dd8`.
- **Fase 2 — parte técnica concluída** nesta atualização: banco de cards Room
  (`data/local/`), importador de `assets/cards.json` com versionamento via
  DataStore (`data/importer/`), campo `RegrasPartida.descartarCardQueimado` e
  `org.gradle.java.home` no `gradle.properties`. **Pendente**: os cards
  definitivos são trabalho editorial posterior — `cards.json` contém apenas 4
  cards DUMMY (`TESTE_01`..`TESTE_04`).
- **Fase 3 — planejada**: telas de uma partida em um único celular (sem rede),
  modelos `Partida`/`Turno`/`Placar`.
- **Fase 4 — planejada**: multiplayer via Nearby Connections (é quando a
  biblioteca `play-services-nearby` é instalada); validação exige 2 aparelhos
  físicos.
- **Backlog**: geração de cards por IA (Gemini); salas online à distância
  (Firebase).

## Arquitetura de multiplayer

- **Revisada** (v3): o multiplayer deixa de ser "baralho sincronizado por seed
  em cada aparelho" e passa a ser rede local real via **Nearby Connections API**
  (`play-services-nearby`), sem depender de internet (Bluetooth/Wi-Fi direto).
- **Modelo estrela**: o anfitrião é a fonte única da verdade. Ele anuncia a
  sala, distribui os cards, e sincroniza turno, dica atual e placar em todos os
  aparelhos conectados.
- A biblioteca só é instalada na Fase 4 — nada de rede nas Fases 2 e 3.
- `SeedDeCodigo` e `EmbaralhadorDeCards` (Fase 1) continuam válidos, mas mudam
  de papel: viram embaralhamento **interno do anfitrião**, não mais mecanismo
  de sincronização de baralho entre aparelhos.

## Decisões de design

- **Leitor pontua** — RESOLVIDA: configurável em `RegrasPartida.leitorPontua`,
  padrão SIM; quando ativo, o leitor ganha os **mesmos** pontos do acertador.
- **Destino do card queimado** — RESOLVIDA: descartado (não volta ao baralho).
  Campo `RegrasPartida.descartarCardQueimado`, padrão SIM (criado na Fase 2).
- **Categoria "Livre"** — EM ABERTO: o papel e o conteúdo da categoria `LIVRE`
  ainda não foram definidos.
- **Jogadores por partida** — RESOLVIDA: mínimo 2, máximo 4 (1 leitor + 1 a 3
  adivinhadores por rodada).
- **Modo de jogo** — RESOLVIDA: individual ou times, configurável antes da
  partida; ambos os modos entram na v1.
- **Placar** — RESOLVIDA: exibido em todos os aparelhos, sincronizado pelo
  anfitrião via Nearby (ver "Arquitetura de multiplayer" acima).
- **Determinismo é sagrado**: seed e embaralhamento não podem usar `hashCode()`
  da plataforma, `kotlin.random.Random` nem `java.util.Random`. As
  implementações próprias vivem em `domain/rules/` (hash polinomial +
  xorshift64). A mesma seed precisa gerar o mesmo baralho, para sempre — hoje é
  a base do embaralhamento interno do anfitrião (ver "Arquitetura de
  multiplayer").

## Documentação — quem é dono do quê

- `docs/GAME_RULES.md` — dono das regras do jogo.
- `docs/CHANGELOG.md` — mudanças notáveis por fase.
- `docs/BUGS.md` / `docs/IMPROVEMENTS.md` — bugs conhecidos e melhorias futuras.
- `docs/CARDS_GUIDE.md` — guia de criação de cards.

## Fluxo de trabalho

- Domínio (`domain/`) é Kotlin puro: sem Android, Room ou Compose — testável na JVM.
- KDoc e commits em português, formato `tipo: descrição`.
- Rodar `./gradlew test` antes de commitar. O JDK do build (JBR do Android
  Studio, JVM 21) está fixado em `gradle.properties` via `org.gradle.java.home`
  — não é preciso configurar `JAVA_HOME`.
- Cards vivem em `app/src/main/assets/cards.json` (fonte da verdade, com campo
  `version`); o banco Room é só um espelho recarregado pelo `CardsImporter`
  quando a versão do asset avança. Para editar cards: alterar o JSON **e
  incrementar `version`**, senão a mudança não chega ao banco.
- **Push é sempre manual do Felipe** — nunca fazer `git push`.
