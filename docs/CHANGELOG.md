# Changelog

Todas as mudanças notáveis do projeto QuemSou serão documentadas neste arquivo.

## Fase 0 — Esqueleto do projeto

- Estrutura Gradle (Kotlin DSL + version catalog) com Kotlin 2.1.0, Jetpack Compose BOM
  2025.02.00, Material 3, Hilt 2.53.1, Room 2.6.1, Navigation Compose 2.8.9 e
  kotlinx-serialization.
- `minSdk` 26, `targetSdk`/`compileSdk` 35. Pacote `com.quemsou.app`.
- Estrutura em Clean Architecture + MVVM: `domain/`, `data/`, `presentation/ui/`,
  `navigation/`, `di/`.
- Telas placeholder: Home (com botões "Criar partida" e "Entrar com código"),
  Setup, Game e Score — sem nenhuma regra de jogo implementada ainda.
- Navegação com rotas tipadas (`@Serializable`) via Navigation Compose.
- Tema Material 3 escuro padrão (cores definitivas virão em uma fase futura).
- `QuemSouApp` (`@HiltAndroidApp`) e módulo `AppModule` do Hilt preparado, ainda vazio.
- Infra do repositório: `.gitignore`, `docs/BUGS.md`, `docs/IMPROVEMENTS.md`,
  `docs/GAME_RULES.md`, `docs/CARDS_GUIDE.md` e `README.md`.
