# Improvements

## Texto do botão da tela Anuncio: "Próxima jogada"

- **Motivação**: feedback do teste de jogo físico (Z Fold) — "próxima
  jogada" é a linguagem que o grupo usa à mesa; "próximo turno" soava mais
  técnico.
- **Mudança**: só o texto exibido no botão (`R.string.partida_anuncio_proximo_turno`
  em `strings.xml`, de "Próximo turno" para "Próxima jogada"). Nomes de
  classes/funções do domínio (`Turno`, `PartidaViewModel.proximoTurno()`
  etc.) e o id do recurso de string permanecem inalterados — a mudança é
  só de linguagem voltada ao jogador.
