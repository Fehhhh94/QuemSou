# Handoff ativo — Friends publicado, baixado e jogado no Fold

## Unidade atual — 2026-09-24

Felipe autorizou publicar o Friends e levá-lo ao celular. Codex executor;
`main`, HEAD `d078ace`, remoto `Fehhhh94/QuemSou`, zero commits à frente de
`origin/main`. Mudanças não commitadas da unidade anterior de agrupamentos
foram preservadas. Não houve autorização para commit ou push.

## Resultado concluído

- `friends-1`, Friends, `PERSONAGEM_FILME`, Cinema e TV, v1,
  `EM_DESENVOLVIMENTO`: 30 personagens e 600 fatos, exatamente 20 por resposta;
  dez permanecem em `clues` para compatibilidade.
- Rascunho aprovado integrado à cópia privada do catálogo. Backup recuperável:
  `%LOCALAPPDATA%/QuemSou/administrador/backups/friends-catalogo-20260920-201526`.
  Índice e arquivo têm `tamanhoEmBytes=112721` e o catálogo completo passou no
  `validarCatalogo`: seis arquivos, nenhuma divergência.
- Origem aplicada validada outra vez pela Central: “Aprovado pela régua do app”.
  Publicação confirmada pela UI: “Baralho publicado no Firestore: versão 1,
  30 cards.”
- Auditoria REST somente leitura reconstruiu os dois blocos e confirmou
  manifesto `PUBLICO`, publicado, v1, 30 cards, 600 dicas e igualdade exata com
  a projeção local. Hash remoto:
  `19fc891654c47cd49a63d8c02ba8fc05d277209ba64473a656e10df64732a85c`.
- Há oito manifestos no catálogo. Kimberly-Clark permanece privado v1 com o
  mesmo hash. Rules mantêm hash
  `da1418d07e9ef4f231c07f061e95bbaf3fd22fc12df30bbecc4de5d7d8628020`;
  o feedback existente mantém hash/timestamp; o acervo mantém 195 respostas.
- A Central foi iniciada em `http://127.0.0.1:8766` e ficou no Friends. O
  rascunho original foi preservado como recuperação. Conteúdo real e helpers
  permanecem fora do Git/ignorados; a fábrica automática continua em hold.

## Validação física concluída em 2026-09-24

- Samsung `SM-F966B`, API 36, serial `RQCY804H40K`; BoraJogar
  `0.5.0-dev-0920.0045`. O app abriu na Home, sem retomar partida pela UI.
- Em Baralhos, Cinema e TV apresentou quatro baralhos e o Friends como
  `30 cards · ~80 KB`. O download real terminou com o estado `Baixado`.
- Snapshot consistente via `run-as` passou em `PRAGMA quick_check`: Room foi de
  8 para 9 baralhos e de 204 para 234 cards. `friends-1` v1 contém 30 ids de
  card e 30 ids de resposta únicos, 20 dicas em cada banco (600 no total) e dez
  dicas de compatibilidade em cada `clues`.
- Feedback, dicas utilizadas, sessões, turnos, respostas jogadas e reservas
  ficaram inalterados entre os snapshots. A sessão residual já conhecida
  continuou preservada e sem reservas; nenhuma partida foi iniciada ou
  abandonada.
- Não houve reinstalação, limpeza de dados, mudança de APK, commit ou push. O
  modo temporário de tela ligada no USB foi restaurado ao valor original `0`.
  Evidências locais ignoradas pelo Git:
  `build/qa-friends-fold-20260924/`.

## Partida controlada concluída em 2026-09-24

- Dois jogadores temporários (`TesteA` e `TesteB`), duas rodadas e somente
  Friends selecionado. O app sorteou `fr_021` (Ursula Buffay) e `fr_028`
  (Barry Farber), sem repetir resposta.
- Cada turno persistiu dez ids de dica distintos, todos pertencentes ao banco
  de vinte da resposta. A UI exibiu as dez posições e revelou uma dica em cada
  rodada; as duas dicas foram registradas no histórico por id, texto canônico e
  texto contextualizado.
- A partida chegou ao placar final com empate de 10–10 e voltou à Home pelo
  botão normal. Reservas finais zero, `PRAGMA quick_check=ok`, catálogo e os
  121 feedbacks inalterados; nenhuma avaliação artificial foi criada.
- O bug aberto de ciclo de vida foi reproduzido: a sessão nova permaneceu com
  `encerrada=0` e `fase=PLACAR_FINAL` após “Voltar ao início”. Iniciar a partida
  fechou automaticamente a flag residual anterior. Não houve limpeza manual.
- A tela ligada no USB foi novamente restaurada a `0`; nenhum APK, Firebase,
  commit ou remoto foi alterado.

## Próxima ação segura

Fazer revisão humana com a turma para avaliar diversão, dificuldade e precisão
editorial. Em uma unidade separada e com autorização, corrigir
`docs/BUGS.md` seção 9 para que “Voltar ao início” marque a sessão final como
encerrada. Download, sorteio de dez entre vinte e persistência no aparelho já
foram validados; não é necessário reinstalar o APK.
