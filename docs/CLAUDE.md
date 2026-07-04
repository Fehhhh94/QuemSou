# QuemSou — Guia do projeto para o Claude

## Histórico de versões

- **v1** — esqueleto do documento (Fase 0).
- **v2** (2026-07-04) — URL do GitHub, Fase 0 registrada como concluída, decisão
  "leitor pontua" resolvida, estado da Fase 1.
- **v3** (2026-07-04) — revisão de arquitetura: multiplayer via Nearby
  Connections + decisões de produto fechadas (jogadores 2–4, individual/times,
  placar em todos, card queimado descartado).

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
- **Fase 2 — planejada**: banco de cards + importador Room, campo de card
  queimado em `RegrasPartida`, `gradle.properties` (`org.gradle.java.home`).
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
  Campo entra em `RegrasPartida` na Fase 2 (não criado ainda).
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
- Rodar `./gradlew test` antes de commitar. No Windows, o Gradle precisa de
  JDK 17+ — usar o JBR do Android Studio
  (`JAVA_HOME = C:\Program Files\Android\Android Studio\jbr`).
- **Push é sempre manual do Felipe** — nunca fazer `git push`.
