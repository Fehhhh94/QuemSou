# Improvements

## 🟣 Modo Shot (implementar após a 5.1)

- **Status**: backlog — especificação registrada em 2026-07-09, **não
  implementar agora**.
- `RegrasPartida`: `modoShot: Boolean = false` + `quantidadeDeShots: Int = 2`
  (1–3, visível na Configuração só com o toggle ligado).
- Posições-shot sorteadas por rodada com seed derivada da fórmula existente
  (seed da partida × 31 + rodada, de `Partida.seedDasDicas`) + fator próprio —
  determinístico, sobrevive à morte de processo, sincronizável na Fase 4.
- O shot é **pedágio**: quem escolheu o número bebe → toca "Bebi!" → a dica
  da posição aparece normalmente. Pontuação intocada (11 − N e a invariante
  dos 10 pontos seguem valendo).
- Grid **não marca** a posição-shot antes do toque (surpresa, coerente com o
  grid às cegas).
- Overlay "🥃 UM SHOT!" com o nome de quem bebe, botão "Bebi!", tratamento
  de insets via `BarraDeAcaoInferior`.
- Quem bebe: **sempre quem escolheu o número**, sem exceções.
- **Nota**: referência a álcool afeta a classificação etária na Play Store.

## Texto do botão da tela Anuncio: "Próxima jogada"

- **Motivação**: feedback do teste de jogo físico (Z Fold) — "próxima
  jogada" é a linguagem que o grupo usa à mesa; "próximo turno" soava mais
  técnico.
- **Mudança**: só o texto exibido no botão (`R.string.partida_anuncio_proximo_turno`
  em `strings.xml`, de "Próximo turno" para "Próxima jogada"). Nomes de
  classes/funções do domínio (`Turno`, `PartidaViewModel.proximoTurno()`
  etc.) e o id do recurso de string permanecem inalterados — a mudança é
  só de linguagem voltada ao jogador.
