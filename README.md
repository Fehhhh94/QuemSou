# QuemSou

Jogo Android presencial de adivinhação por dicas. Um jogador lê até dez dicas;
os demais tentam descobrir a resposta. O jogo suporta grupos, baralhos locais,
Modo Shot opcional e um espelho de leitura acessível pelo navegador na rede
local.

## Estado do projeto

- Partida principal offline e jogável em um único aparelho.
- Catálogo de baralhos com cache e downloads opcionais.
- Fase 4A parte 1 implementada: pareamento do espelho por QR/URL local.
- Partes 2 e 3 do espelho ainda pendentes.

Estado e próxima ação: [Handoff ativo](docs/HANDOFF_ACTIVE.md).

## Começar rápido

Pré-requisitos: Android Studio/JBR configurado pelo projeto e Android SDK 35.

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

APK gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Stack

- Kotlin + Jetpack Compose Material 3
- Clean Architecture + MVVM
- Hilt/KSP, Room e DataStore
- Navigation Compose com rotas tipadas
- OkHttp para catálogo
- Ktor CIO para servidor HTTP local
- ZXing core para QR gerado no aparelho

## Documentação

Use o [índice da documentação](docs/DOCS_INDEX.md) para carregar somente o que
precisa:

- [Contexto e arquitetura atuais](docs/PROJECT_CONTEXT.md)
- [Regras do jogo](docs/GAME_RULES.md)
- [Runbook de suporte](docs/SUPPORT_RUNBOOK.md)
- [Bugs e armadilhas](docs/BUGS.md)
- [Formato do catálogo](docs/CATALOG_FORMAT.md)
- [Criação de cards](docs/CARDS_GUIDE.md)
- [Histórico](docs/CHANGELOG.md)

Agentes devem começar por `AGENTS.md` ou `CLAUDE.md`. **Nunca fazer push por
agente**; o push é manual do Felipe.
