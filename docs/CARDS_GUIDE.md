# Guia de Cards

Régua editorial de todo card do QuemSou. O baralho vive em
`app/src/main/assets/cards.json` — ao editar, **incrementar o campo
`version`**, senão o importador não recarrega o banco.

## Régua editorial

- **Exatamente 10 dicas por card, com mix de dificuldades.** Não existe curva
  do difícil para o fácil: o app embaralha a posição das dicas e a escolha no
  grid 1–10 é às cegas, então qualquer dica pode ser a primeira.
- **Toda dica é autossuficiente**: faz sentido sozinha, em qualquer ordem, sem
  depender de outra dica.
- **Nenhuma dica nomeia a resposta.** A dica mais forte **aponta** para a
  resposta, mas não a entrega — "o arqueólogo aventureiro interpretado por
  Harrison Ford" aponta; "sou o Indiana Jones" entrega.
- **Sem trechos de letras de música** nas dicas.
- **Answer não vazio e nenhuma dica vazia** — o importador valida na
  inicialização e falha ruidosamente apontando o id do card inválido.

## Estrutura do card no JSON

- `id`: único no baralho (ex.: `pf_001`, `mm_014`).
- `type`: `PESSOA`, `LUGAR` ou `COISA`.
- `category`: `PERSONAGEM_FILME` ou `MUNDO_DA_MUSICA`. A categoria **Livre**
  é um filtro que une todas as categorias — não existem cards exclusivos dela.
- `answer`: a resposta secreta.
- `clues`: lista com exatamente 10 dicas.
