# Guia de Cards

Régua editorial de todo card do QuemSou. O baralho vive em
`app/src/main/assets/cards.json` — ao editar, **incrementar o campo
`version`**, senão o importador não recarrega o banco.

## Régua editorial

O baralho é uma coleção de respostas; a carta de dez dicas é montada durante
o jogo. Uma mesma resposta pode pertencer a vários baralhos/temas, usando o
mesmo `respostaId` e ids estáveis dos fatos. Variação de tema não cria uma
nova identidade. Conceitos, consumo e rotação: `GAME_RULES.md`.

A curadoria deve escolher respostas reconhecíveis pelo público do baralho,
com pistas específicas e variadas. Resposta obscura não é sinônimo de desafio.
Feedback sobre frequência orienta a seleção; feedback sobre ambiguidade,
erro ou redundância orienta a dica; avaliação de dificuldade orienta a coleção.
Essa classificação editorial não é um ajuste automático de dificuldade no app.

- O novo acervo separa resposta canônica (`respostaId`) e dicas com ids
  imutáveis. A fábrica solicita 60 fatos por resposta, não 60 paráfrases.
  Rejeitar conteúdo duvidoso, redundância semântica e pistas injustamente
  ambíguas. Se não houver fatos suficientes, escolher outra resposta.
- Correção factual preserva o id da dica. Fato novo recebe id novo. Uma
  mudança cosmética nunca deve ser usada para contornar o histórico local.
- `clues` continua com dez dicas para compatibilidade; `bancoDeDicas` guarda
  o acervo. A seleção da carta acontece no aparelho, antes de abrir a rodada.
- A revisão automatizada não garante correção factual nem elimina todas as
  paráfrases. O feedback registra versão, carta e textos revelados para que
  a próxima geração tenha contexto editorial preciso.

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
