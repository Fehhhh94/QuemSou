# Guia de Cards

Régua editorial de todo card do QuemSou. Conteúdo real vive no Firebase e
nas origens privadas da Central, fora do Git. Não inserir cards/dicas em
`app/src/main/assets/cards.json`, que permanece vazio. Para distribuir uma
correção: salvar, validar, aplicar localmente e publicar no Firestore com nova
versão técnica. Fluxo completo: `ADMINISTRADOR_LOCAL.md`.

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

### Evolução do acervo

Não há teto editorial total de dicas de uma resposta: os limites do JSON
são por item recebido, não pelo banco reunido na instalação. Um baralho pode
crescer até 500 cards e não existe mais o estado editorial "edição final".
Ao ampliar uma resposta, preservar `respostaId` e criar ids apenas para fatos
novos. Não republicar dicas antigas como se fossem inéditas. Lotes e fontes:
`PACOTES_EDITORIAIS.md`.

O convite de avaliação aparece após cada dica revelada. O export v3 distingue
`DICA_REVELADA` de `ACERTO`/`QUEIMADO`, mantendo voto BOM/FRACO e comentário.
Seu `contextoJson` contém `sessaoId`, `rodada`, `posicao`, `respostaId`,
`resposta`, `dicaId`, `texto` e `versaoDoBaralho`; só aquela dica é incluída.
Corrigir o voto atualiza essa ocorrência; outra rodada/partida é outro registro.
Comentários são limitados a 1.000 caracteres. Rascunho sem voto não é salvo.
Esses registros orientam revisão editorial e pedidos explicitamente enviados;
não alteram sozinhos o conteúdo instalado, a seleção ou a dificuldade.

### Campos

- `id`: único no baralho (ex.: `pf_001`, `mm_014`).
- `type`: `PESSOA`, `LUGAR` ou `COISA`.
- `category`: categoria interna do modelo. No JSON do catálogo, o card herda
  `categoria` do baralho (`PERSONAGEM_FILME`, `MUNDO_DA_MUSICA`, `ESPECIAIS`);
  não enviar `category` por card. Temas personalizados usam o agrupamento
  Especiais; uma empresa não exige um enum novo. Contrato: `CATALOG_FORMAT.md`.
- `answer`: a resposta secreta.
- `clues`: lista com exatamente 10 dicas.
