# Handoff ativo — validação do catálogo e expansão de Friends

## Unidade atual — 2026-10-02

Objetivo confirmado: seguir as melhorias e criar mais cartas. Felipe confirmou
Friends com vinte respostas novas. A autorização de commit e push automático
foi dada nesta conversa. Publicação Firebase não foi solicitada; fábrica em hold.

Executor anterior e atual: Codex. Base: `main`, HEAD `fb16980`, remoto
`Fehhhh94/QuemSou`, worktree inicialmente limpo e sincronizado. Nenhuma mudança
preexistente nesta unidade. Handoff anterior arquivado com hash idêntico em
`historico/HANDOFF_2026-10-02_ENCERRAMENTO_PLACAR.md`.

## Implementação e conteúdo

- `NucleoDeValidacaoCli.kt`: tamanho positivo declarado no índice precisa
  corresponder aos bytes do arquivo. Campo ausente ou zero continua válido.
  A CLI permanece no sourceSet de testes e não entra no APK.
- `NucleoDeValidacaoCliTest.kt`: quatro regressões para igualdade em UTF-8
  com acentos/emoji, divergência, zero e espaços/quebras de linha reais.
- Friends local v2: 50 cartas, 800 fatos. Novos ids `fr_031`–`fr_050`, vinte
  respostas de dez dicas cada; ids e conteúdo das trinta cartas anteriores,
  incluindo seus bancos de vinte, preservados integralmente.
- Cópia privada do catálogo e rascunho de edição sincronizados. A aprovação
  antiga do rascunho foi invalidada; a Central exigirá nova validação antes
  de publicar. O rascunho original de criação v1 foi preservado.
- Donos atualizados: `CATALOG_FORMAT.md`, `IMPROVEMENTS.md`,
  `PROJECT_CONTEXT.md`, `PACOTES_EDITORIAIS.md`, `CHANGELOG.md` e este handoff.
  A menção antiga a geração automática como pedido atual foi corrigida no
  backlog para a decisão vigente de hold.

## Validações e evidências

- Antes da correção: 14 testes focados, duas falhas reproduzindo tamanhos
  incorretos aceitos. Depois: 14/14 aprovados.
- `gradlew.bat test validarCatalogo -Ppasta=<origem privada>`: 294 testes
  Debug e 294 Release, zero falhas/erros/skips. Catálogo original aprovado.
- Candidato Friends de cinquenta cartas aprovado por `validarBaralho`.
  `validarCatalogo` aprovou tanto a preparação quanto a origem após integração.
- Varredura de 294 respostas nas origens: zero colisões normalizadas. Banco
  novo com 200 dicas, sem duplicação normalizada por resposta ou resposta
  completa nas pistas. Isso não comprova dificuldade/diversão humana.
- Backup integral anterior à aplicação local:
  `%LOCALAPPDATA%/QuemSou/administrador/backups/friends-expansao-20261002-084535`.
  Apenas arquivo Friends, índice e rascunho de edição mudaram; quatorze
  arquivos anteriores preservados por SHA-256. Histórico e feedback do
  aparelho não foram acessados nesta unidade.
- Candidato, catálogo preparado, revisão HTML, fontes por resposta,
  manifesto e recibo de integração:
  `%LOCALAPPDATA%/QuemSou/administrador/temporarios/friends-expansao-20261002`.
  SHA-256 do JSON integrado:
  `8db916344ddd54d12bab25c85b5bb5203869bf4b7ffebb5123b1f88d4378a5c3`.
- Logs Gradle em `build/qa-catalogo-cards-20261002/`, ignorados pelo Git.
  Conteúdo real não foi colocado no repositório.

## Pendências e próxima ação

Revisar as dicas durante partidas. Para disponibilizar Friends v2 no celular,
obter autorização específica de publicação, validar a origem exata pela
Central e publicar no Firestore; depois baixar a atualização pela UI real e
conferir a preservação das identidades e dos históricos. A v1 de trinta cartas
é a última versão remota/física validada; esta unidade não publicou conteúdo.

Permanece pendente instalar o APK da correção do placar no Fold e repetir a
saída pela Home, conforme o handoff arquivado. TalkBack e espelho em rede
física continuam pendentes. Não há nova prova de dispositivo nesta unidade.

Não autorizados nesta unidade: publicação Firebase, limpeza de dados,
ativação da fábrica e alteração das regras do jogo.
