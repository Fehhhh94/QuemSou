# Handoff ativo — três novos baralhos de séries

## Unidade atual — 2026-10-02

Objetivo confirmado: Felipe pediu outros baralhos de séries. Foram criados
The Big Bang Theory, Stranger Things e The Office (EUA), vinte respostas cada.
A autorização de commit e push automático foi dada nesta conversa.
Publicação Firebase não foi solicitada; fábrica em hold.

Executor anterior e atual: Codex. Base: `main`, HEAD `f08adc6`, remoto
`Fehhhh94/QuemSou`, worktree inicialmente limpo e sincronizado. Nenhuma
mudança preexistente nesta unidade. Handoff anterior arquivado com bytes
idênticos em `historico/HANDOFF_2026-10-02_CATALOGO_E_FRIENDS.md`.

## Conteúdo e integração

- Três baralhos v1, `EM_DESENVOLVIMENTO`, no agrupamento Cinema e TV:
  `the-big-bang-theory-1`, `stranger-things-1` e `the-office-1`.
  Sessenta cartas, seiscentos fatos, dez por resposta, com `respostaId`
  e ids estáveis das dicas. Fontes/recortes: `PACOTES_EDITORIAIS.md`.
- Stranger Things cobre somente temporadas 1–4 e exclui obras derivadas;
  as três descrições avisam sobre spoilers. The Office é a versão americana.
- Conteúdo, manifesto, fontes por resposta, preparação, revisão HTML e recibo
  em `%LOCALAPPDATA%/QuemSou/administrador/temporarios/series-20261002`.
  JSONs integrados na origem privada `origens/catalogo/baralhos/`;
  conteúdo real não entrou no Git nem no asset vazio do APK.
- Índice atualizado com três entradas e tamanhos exatos. Os seis baralhos
  anteriores, biblioteca legada e nove rascunhos foram preservados por hash.
  Foram acrescentados três rascunhos de edição, sem aprovação; a Central
  exigirá validar novamente antes de publicar. Rascunhos anteriores mantêm
  o índice-base original e eventuais conflitos precisam de revisão.
- Donos atualizados: `CATALOG_FORMAT.md`, `PROJECT_CONTEXT.md`,
  `PACOTES_EDITORIAIS.md`, `CHANGELOG.md` e este handoff.
  Nenhum código do app ou regra do jogo mudou.

## Validações e evidências

- Seleção conferida antes da redação contra 314 ocorrências nas origens
  locais: sessenta respostas novas, zero colisões normalizadas.
- Dez dicas e dez fatos distintos após normalização em cada carta,
  seiscentos ids de dicas únicos, nenhuma pista contendo a resposta completa.
  Revisão editorial corrigiu relação familiar, dupla de vendas e redundâncias.
- `gradlew.bat test validarCatalogo -Ppasta=<preparação>` aprovado:
  294 testes Debug e 294 Release, zero falhas, erros ou skips; catálogo com
  nove baralhos aprovado, sem divergência entre índice e arquivos.
- `validarBaralho` aprovou separadamente cada um dos três lotes.
  `validarCatalogo` também aprovou a origem após a integração.
  O inventário real da Central reconhece os três lotes como editáveis,
  sem avisos ou observações de inconsistência.
- Backup integral antes da escrita local:
  `%LOCALAPPDATA%/QuemSou/administrador/backups/series-20261002-092301`.
  Dezesseis arquivos anteriores continuam idênticos; somente o índice mudou.
  Feedbacks e históricos do aparelho não foram acessados.
- Hashes dos três novos JSONs e recibo em `manifesto.json` e
  `integracao-local.json` no diretório privado da entrega. Logs Gradle em
  `build/qa-series-20261002/`, ignorados pelo Git.
- O handoff anterior foi arquivado sem alteração. Revisão humana em partidas
  continua pendente; aprovação mecânica não comprova diversão, dificuldade
  ou precisão factual humana de cada dica. Não há nova prova de dispositivo.

## Pendências e próxima ação

Para disponibilizar os três novos lotes no celular, obter autorização específica
de publicação, validar o conteúdo exato pela Central e publicar no Firestore;
depois baixar pela UI real e conferir cartas, identidades e histórico.
Friends v2 também continua somente local; a v1 de trinta cartas é a última
versão remota/física validada. Essa pendência está no handoff arquivado.

Permanece pendente instalar no Fold o APK da correção do placar e repetir a
saída pela Home, conforme `historico/HANDOFF_2026-10-02_ENCERRAMENTO_PLACAR.md`.
TalkBack e espelho em rede física continuam pendentes.

Não autorizados nesta unidade: publicação Firebase, limpeza de dados,
ativação da fábrica e alteração das regras do jogo.
