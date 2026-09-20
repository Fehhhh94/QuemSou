# Handoff ativo — conteúdo fora do Git

Atualizado em 2026-09-20. Separação implementada e verificada localmente.
Felipe confirmou commit e push dos DOIS repositórios, sem reescrever histórico.
Essa autorização é pontual; não altera as regras gerais dos agentes.

## Entrega e preservação

- App: main, base 681daad, remoto Fehhhh94/QuemSou. Catálogo: main, base
  aa39447, remoto Fehhhh94/QuemSou-Baralhos. Consulte o Git para os hashes do
  fechamento; mudanças anteriores de Firebase/Central/acervo foram preservadas.
- Conteúdo copiado para `%LOCALAPPDATA%/QuemSou/administrador/origens/`:
  envelope legado + índice + cinco JSONs. Backup original separado em
  `backups/separacao-git-v1/`, com manifesto SHA-256. Sete hashes conferidos.
- Asset v8 vazio; nenhum baralho real no APK. Upgrade não remove Room ou
  histórico. Instalação nova exige internet para o primeiro download.
  Android-dev/data-layer orientaram a preservação offline sem migration nova.
- Central usa fontes privadas; recusa diretórios no código/Git. IDs e bytes
  preservados para rascunhos. Biblioteca legada não escreve mais no APK.
- Catálogo Git antigo deixa de rastrear seis JSONs, sem apagar arquivos locais.
  Ignore/documentação impedem retomada acidental do fluxo GitHub raw.
- Central em `http://127.0.0.1:8766` reiniciada sem edição pendente:
  nove entradas jogáveis (sete baralhos distintos), um técnico oculto,
  195 respostas/1.980 dicas, um feedback automático. Instância antiga de 8765
  não foi encerrada; usar 8766. O servidor não recarrega código em execução.
- Firebase/Rules e telefone não foram alterados nesta unidade. Nenhum card novo.
  Fotos, credenciais, backups e QA ficam fora do commit.
- O conteúdo público antigo ainda existe no histórico de ambos os repositórios.
  Não há autorização para rewrite/force-push, excluir repos ou mudar visibilidade.

## Validação desta separação

- `gradlew.bat test :app:assembleDebug :app:compileDebugAndroidTestKotlin
  validarCatalogo -Ppasta=<catalogo-privado>`: exit 0; 288 JVM Debug +
  288 Release, zero falhas/skips; cinco arquivos de catálogo aprovados.
- Python: 207 testes, OK, quatro skips previstos (dois links sem privilégio
  Windows e dois Gradle opt-in). Node: 33/33.
- Comparação normalizada das 1.980 dicas contra arquivos candidatos do app:
  zero ocorrências. APK aberto como ZIP confirmou envelope v8 vazio.
  Auditoria recuperável local: `build/auditar_separacao.py`.
- APK local `0.5.0-dev-0920.0130`, SHA-256
  `5c99efbdcb51c81624bf20d480f99624c84a9ebaf0786c2b4bab98771b61295c`.
  NÃO instalado no telefone nesta unidade. Teste Room de upgrade compilado,
  mas não executado em aparelho; preservação do importador coberta em JVM.
- Retirada de seis testes dependentes de conteúdo real e adição de dois de
  bootstrap explicam 292 → 288 JVM; teste sintético de bancos preservado.

## Próxima ação segura

- Fechar commit/push normal dos dois repositórios após revisão final, conforme
  autorização explícita do Felipe. Não incluir os JSONs privados nem forçar push.
- Novo teste físico desta build/instalação limpa e a inconsistência de saída
  do placar (BUGS, seção 9) são unidades separadas, não corrigidas aqui.
- Limpeza de conteúdo nas revisões históricas exige decisão própria.

## Teste físico solicitado em 2026-09-20

- Felipe autorizou testar o download/atualização pelo Firebase, o especial
  privado Kimberly-Clark e a seleção de dicas entre partidas no host.
- Fold SM-F966B, serial RQCY804H40K, API 36. Felipe liberou o aparelho e
  autorizou explicitamente encerrar a partida aberta da Shakira. Tarefa
  WakeSong confirmada concluída; nenhum comando foi dirigido ao WakeSong.
- A partida anterior foi abandonada pela UI: 30 chaves reservadas → zero,
  sem perda de dica utilizada ou feedback. Não houve limpeza/reinstalação.
- O APK instalado 0.5.0-dev-0919.1436 ainda exibia o catálogo antigo.
  `:app:testDebugUnitTest :app:assembleDebug`: exit 0, 292 testes, zero
  falhas/skips. Atualização `adb install -r`: Success. Versão instalada
  **0.5.0-dev-0920.0045**, versionCode 1, lastUpdateTime 2026-09-20 00:46:23;
  firstInstallTime mantido em 2026-09-13 00:38:20. SHA-256 do APK:
  `74e80ce801ff15b49037fe1ff2de91535d8227e72d330da2cd4e10f598fc8d6e`.
- UI real Firebase: Mundo dos Bruxos atualizado v2→v3; Mundo Pop v1→v2.
  Ambos terminaram como “Baixado”; Room confirmou as versões novas.
  Especiais listou Kimberly-Clark — Finanças, 30 cards, já instalado v1.
- Instrumentos: carta com dez dicas, nenhuma consumida durante a reserva;
  uma dica de Violino revelada e persistida, seguida de abandono. As nove
  ocultas não foram consumidas. Na nova configuração, elegíveis 4→3;
  quatro rodadas foram bloqueadas com orientação de reduzir/adicionar baralhos.
- Partida Kimberly-Clark completa pela UI: quatro rodadas, uma dica em cada,
  dez dicas distintas por carta, placar QA1=20 / QA2=20. Não foram enviados
  votos/comentários de teste. App deixado na tela inicial.
- Preservação verificada por cópias somente leitura: quick_check=ok;
  121 feedbacks idênticos, todos os turnos/sessões e as 24 chaves anteriores
  preservados. Cinco dicas novas reveladas = 15 chaves adicionais; total 39.
  Reservas finais zero; nenhum conteúdo ou banco remoto foi editado.
- **Ressalva:** o botão “Voltar ao início” do placar não fecha a flag Room
  `encerrada`; uma sessão permanece com PLACAR_FINAL/encerrada=0.
  `verify_evidence.py` retorna exit 1 por essa verificação, não por perda de
  dados. Diagnóstico e próximo teste: `BUGS.md`, seção 9. Sem correção de código.
- Evidência ignorada pelo Git: `build/qa-fold-20260920/` (XMLs de UI,
  snapshots Room antes/depois, `verification.json`, helpers e `home-final.png`).
  Screenshots anteriores a `home-final.png` têm prefixo textual do screencap;
  não usar esses arquivos como PNGs válidos sem decodificar o transporte.
- Limites: não rebaixou/reinstalou o especial para forçar novo download v1;
  não validou no Fold sorteio repetido da mesma resposta com banco >10/500,
  queda real de rede, TalkBack, espelho ou morte de processo.
- Somente documentação dona e evidências ignoradas foram editadas; código e
  alterações preexistentes preservados. Main/681daad; sem commit/push/publicação.

## Objetivo, autoridade e executor

Felipe aprovou publicar as novas Rules e migrar respostas/dicas para o Firebase
após a conclusão local. Codex foi o único executor desta unidade. O trabalho
anterior de Claude Code/Codex foi preservado; não houve nova implementação.
Sem geração de conteúdo nem reativação da fábrica.

## Git e arquivos

- Remoto Fehhhh94/QuemSou, branch main, HEAD 681daad.
- Zero commits à frente de origin/main; nenhum commit/push nesta entrega.
- Worktree extensamente modificado antes da unidade, incluindo Android,
  assets, administrador e exclusão de SeloDeEstado.kt: preservado integralmente.
- No teste físico, alterados somente documentos donos: ADMINISTRADOR_LOCAL,
  PROJECT_CONTEXT, CHANGELOG, BUGS e este handoff. README do administrador
  pertence à etapa anterior e foi preservado.
- Helper de auditoria somente leitura/checklist/logs em
  `build/claude-acervo-20260919/`, ignorados pelo Git, sem credenciais.

## Resultado de produção

- Central reiniciada na porta 8766 com código atual, sem edição aberta;
  diretório privado original preservado. Processo atual iniciado: 27636.
- `firebase deploy --only firestore:rules --project borajogar-app --non-interactive`:
  exit 0. Não publicou índices, Functions nem snapshots de baralho.
- Release ativa: `135cd30a-6d4d-43be-9b4f-3c3e95f67bb5`.
  `updateTime=2026-09-20T02:49:14.744772Z` (19/09 no horário local).
- Rules lidas de volta iguais ao arquivo local; SHA-256:
  `da1418d07e9ef4f231c07f061e95bbaf3fd22fc12df30bbecc4de5d7d8628020`.
- Migração criar-apenas pela Central: 195 criadas, zero existentes, zero erros.
- Auditoria completa: 195 respostas e 1.980 dicas, sendo 1.680 de escopo
  PUBLICO e 300 PRIVADO; zero bancos incompletos, zero divergências de
  texto/escopo/status contra a prévia. Técnicos excluídos.
- Os sete manifestos mantiveram versões, hashes, publicação e visibilidade.
  Kimberly-Clark continua privado. Nenhum baralho foi republicado.
- Um feedback preexistente permaneceu intacto, com updateTime
  `2026-09-19T18:23:16.430573Z`, anterior a esta publicação.
- UI real: Shakira aparece registrada no Firestore com dez dicas; feedback
  existente mostrado sob a dica correta como “Firestore — automático”.
  Não foi criada avaliação sintética nem modificado texto para teste.

## Evidência e limites

- Regressões pré-publicação: Python 202 testes/4 skips previstos, Node 33/33,
  Firestore Emulator 8/8 (sete cenários + pai), Gradle real opt-in 2/2 e
  Android JVM 292/292. Logs da entrega local na mesma pasta de build.
- Validação desta unidade: compilação das Rules pelo serviço, comparação
  remota/local, migração via sessão admin da Central, auditoria REST somente
  leitura de todos os bancos e leitura/feedback pela interface real.
- Na etapa da migração não houve teste físico nem instalação; a validação
  posterior no Fold está descrita no início deste handoff. Pontuação/seed
  não foram alteradas. Edição/conflito remoto foi validado no emulador;
  não corrigimos conteúdo real apenas para testar um salvamento.
- Hash inicial bruto do JSON de feedbacks não é comparável por ordem dos
  campos; verificação usa timestamp anterior e hash canônico posterior:
  `7efed1726053e9f3665515da20cf45927ad628e607df934b9ff1e17716a7a63f`.
- Servidor real permanece aberto; não encerrar ao retomar sem conferir
  edições do usuário. Abas originais foram preservadas.

## Recuperação

Rules anteriores correspondem a `build/claude-acervo-20260919/baseline/firestore.rules`,
release `862bb842-4089-498c-b0a1-01810ff3aa67`. Não restaurar preventivamente.
Em regressão de acesso/privacidade, interromper novas escritas e avaliar
restauração das Rules anteriores, sem apagar os bancos. Repetir migração
criar-apenas não sobrescreve bancos completos. Checklist e auditoria:
`publicacao-checklist.md` e `auditar-producao.cjs` na pasta de build.

## Próxima ação segura

Se Felipe autorizar correção, alinhar a saída do placar à rotina de encerramento
existente e adicionar regressão; repetir a saída pelo botão no Fold sem limpar
dados. Os demais cenários físicos não cobertos estão delimitados acima.

Acervo pronto para edição pela Central em http://127.0.0.1:8766.
Ao alterar dicas, incorporar os bancos no rascunho do baralho, validar,
aplicar na origem, validar e publicar explicitamente; não há distribuição
automática de edição editorial. Limites/contrato: ADMINISTRADOR_LOCAL e
CATALOG_FORMAT. Não foram criadas 500 dicas por resposta: esse é o teto,
não a quantidade do conteúdo migrado.

O teste físico acima foi autorizado depois da migração. Commit/push foram
autorizados posteriormente, com a decisão de privacidade pendente descrita
no início. Geração de cards, limpeza/reinstalação destrutiva, reativação da
fábrica e publicação de novos snapshots continuam fora desta autorização.
