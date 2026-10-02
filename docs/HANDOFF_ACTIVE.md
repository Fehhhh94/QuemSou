# Handoff ativo — encerramento da sessão ao sair do placar

## Unidade atual — 2026-10-02

Objetivo confirmado: Felipe pediu para seguir o desenvolvimento. Retomada pela
pendência técnica registrada no handoff anterior e em `BUGS.md`, seção 9:
encerrar a sessão persistida antes de sair pelo botão do placar.

Executor anterior e atual: Codex. Base da unidade: branch `main`, HEAD
`d078ace`, remoto `Fehhhh94/QuemSou`, sincronizada com `origin/main`.

Decisões e limites: fábrica continua em hold. Após receber os resultados,
Felipe autorizou explicitamente commit e push automático das alterações
pendentes desta entrega. Publicação Firebase e atualização do Fold continuam
fora desse fechamento.

## Arquivos preexistentes preservados

As 18 alterações/arquivos preexistentes foram inventariados por SHA-256 em
`build/qa-sessao-20261002/baseline-preexisting.json`: administrador e seus
agrupamentos/testes, `Colecao.kt`, README, documentos e histórico anterior.
Código preexistente permaneceu idêntico; os documentos donos abaixo receberam
somente a atualização desta entrega. O handoff completo do Friends, incluindo
produção e validação física, foi preservado sem alteração em
`historico/HANDOFF_2026-09-24_FRIENDS_FOLD.md`.

## Arquivos alterados na tarefa

- `PartidaScreen.kt`: o botão do placar chama `confirmarAbandono` antes da saída,
  como já fazia o BackHandler. Nenhuma alteração de regra ou schema Room.
- `PartidaViewModelTest.kt`: duas regressões de encerramento assíncrono,
  duplicidade, falha e recuperação do placar.
- Novo `SaidaDoPlacarRoomUiTest.kt`: partida de duas rodadas pela tela real,
  com conteúdo fictício e banco Room isolado em memória.
- `BUGS.md`, `PROJECT_CONTEXT.md`, `CHANGELOG.md`, este handoff e o histórico
  do handoff anterior.

## Validações e resultados

- Regressão instrumentada reproduziu o bug antes da correção: callback de
  navegação executado antes da transação de encerramento. O mesmo teste passou
  com a correção, mantendo a saída bloqueada durante a gravação.
- `gradlew.bat test assembleDebug assembleDebugAndroidTest --console=plain`:
  sucesso; 290 testes Debug e 290 Release, zero falhas/erros/skips.
- Pixel_2, `emulator-5580`, API 35: 11/11 instrumentados nas classes
  `SaidaDoPlacarRoomUiTest` e `FasesDaPartidaUiTest`. Duas rodadas somam vinte
  pontos; sessão passa de aberta a encerrada, reservas/sessões abertas ficam
  em zero. Conteúdo, checkpoints de sessão/turnos, dicas/respostas utilizadas
  e feedback permanecem iguais. A navegação é verificada pelo callback da
  tela; o teste não usa o NavGraph nem constitui validação física no Fold.
- Logs, resumo JVM, manifesto de preservação e cópia do APK ficam em
  `build/qa-sessao-20261002/` (ignorados pelo Git).
- Preservação verificada: 14 arquivos preexistentes idênticos e quatro
  documentos donos atualizados; handoff anterior arquivado com o mesmo hash.
  `git diff --check` e verificação de espaços nos novos arquivos limpos.
  Antes do fechamento havia 22 arquivos pendentes, incluindo as alterações
  anteriores; todos compõem a entrega revisada para commit. Resumo da
  implementação: `verification.json` na pasta de evidência. O emulador
  iniciado para a tarefa foi desligado.
- APK: `build/qa-sessao-20261002/BoraJogar-0.5.0-dev-1002.0746.apk`.
  SHA-256: `952ebfc5aa82554b1ae20e1a0df90e88e92474751c4873238b2bf3b7d652942c`.
- Rechecagem antes do commit: `gradlew.bat test` aprovado (290 testes por
  variante); Python 213 testes, quatro skips previstos; Node 36 aprovados e
  um teste de emulador Firestore ignorado. Zero falhas. Logs `precommit-*`
  na mesma pasta. O remoto foi atualizado por fetch e estava sincronizado.

## Validações ainda pendentes

- Atualizar o Fold por instalação preservando os dados e repetir partida
  completa → placar → Voltar ao início → Home, conferindo `encerrada=1`,
  reservas zero e preservação do histórico. O aparelho não estava conectado;
  seu APK anterior continua inalterado.
- TalkBack, revisão humana dos baralhos e espelho em rede física continuam
  pendentes conforme `PROJECT_CONTEXT.md`; esta correção não os valida.

## Próxima ação segura

Revalidar o APK no Fold quando disponível. Commit e push dos 22 arquivos foram
autorizados para este fechamento; consultar o Git para o HEAD e a sincronização
resultantes. Firebase, geração de conteúdo e limpeza de dados permanecem fora
da unidade.
