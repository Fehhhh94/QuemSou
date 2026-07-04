# QuemSou

QuemSou é um jogo de adivinhação por dicas para grupos presenciais, no estilo Perfil.
Cada jogador pega um card no próprio celular; o card tem 1 resposta secreta e 10 dicas
em curva de dificuldade. O app é 100% offline na v1 — sem Firebase, sem internet.

## Status

Fase 0 — esqueleto do projeto. Veja [docs/CHANGELOG.md](docs/CHANGELOG.md) para o
histórico de mudanças, [docs/GAME_RULES.md](docs/GAME_RULES.md) para as regras do
jogo e [docs/CARDS_GUIDE.md](docs/CARDS_GUIDE.md) para o guia de criação de cards.

## Stack técnica

- Kotlin 2.1.0 + Jetpack Compose (Material 3)
- Arquitetura: Clean Architecture + MVVM
- Injeção de dependência: Hilt
- Persistência local: Room
- Navegação: Navigation Compose com rotas tipadas (`kotlinx.serialization`)
- `minSdk` 26 · `targetSdk`/`compileSdk` 35

## Build

```
./gradlew assembleDebug
```
