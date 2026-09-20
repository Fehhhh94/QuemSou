# Administrador local de baralhos

Atualizado em 2026-09-20. Implementação concluída e disponível para uso local
pelo atalho `Abrir-Administrador.cmd`.

## Escopo e decisão

Felipe escolheu um painel local no navegador para administrar os baralhos
desta máquina. Ele é separado do Android e da fábrica automática, que
permanece em hold. O painel pode publicar no catálogo Firestore somente por uma
ação explícita, depois de salvar e validar o conteúdo exato. Não faz push, não
gera APK e não instala conteúdo no telefone.

O inventário consolida a biblioteca legada privada, o catálogo privado e os
rascunhos privados sem fundir origens iguais. Na auditoria atual há sete
baralhos de jogo distintos e um técnico; Cinema Clássico e Mundo da Música
existem nas duas origens. Ambas ficam fora do Git. A tela Android de catálogo continua enumerando
somente o índice remoto/cache e não foi redesenhada nesta unidade.

## Separação de conteúdo e código (2026-09-20)

- O conteúdo real saiu do asset: o APK leva apenas o envelope vazio v8.
  Atualizar não apaga Room/históricos; instalar do zero exige primeiro download.
- A Central lê `%LOCALAPPDATA%/QuemSou/administrador/origens/`:
  `embarcados-legado.json` e `catalogo/indice.json` + `catalogo/baralhos/`.
  Chaves internas `asset:`/`catalogo:` e bytes originais foram preservados,
  mantendo compatibilidade com rascunhos e aprovações existentes.
- Sete arquivos foram copiados e conferidos byte a byte. Backup original em
  `backups/separacao-git-v1/`, com manifesto SHA-256. Não sobrescrever.
- Configuração recusa conteúdo no código ou em outro repositório Git, inclusive
  worktrees e caminhos resolvidos por links. Aplicar não escreve mais no APK.
- O checkout antigo `QuemSou-Baralhos` deixa de rastrear seis JSONs; arquivos
  locais são preservados e ignorados, mas não são mais usados pela Central.
- Nenhuma mudança no Firestore, nas Rules ou nos dados do telefone nesta etapa.
  Retirar arquivos dos próximos commits não limpa histórico já publicado.
- Procedimento de migração/recuperação: `administrador/README.md`.
  Evidências de teste e situação de Git: `HANDOFF_ACTIVE.md`.

## Funcionamento

- servidor HTTP somente em `127.0.0.1`, com validação de Host/Origin, token
  anti-CSRF, CSP fechada e lista fixa de arquivos estáticos;
- busca por nome, agrupamento, categoria e respostas; filtros de origem,
  agrupamento e situação; conteúdo técnico oculto por padrão;
- todos os baralhos com estado técnico conhecido são editáveis; `FINALIZADO`
  é aceito apenas como valor legado e não cria mais uma edição imutável;
- o editor e o validador aceitam até 500 cards por baralho;
- rascunhos e backups ficam no diretório privado fora dos repositórios;
- avaliações de dicas sincronizadas no Firestore são consultadas diretamente
  pelo navegador ao abrir e a cada 30 segundos; a importação de listas
  `quemsou-feedback` v3 permanece como contingência local;
- votos e comentários aparecem sob a dica correspondente, agrupados pela fonte
  **Firestore — automático** ou pelo arquivo de contingência que os trouxe;
- o pedido de reescrita para o Codex é preparado com o contexto editorial e
  copiado para a área de transferência (ou baixado em `.md`); não há chamada
  automática de IA nem alteração automática da dica;
- alterações preservam campos desconhecidos, ids e bancos de dicas; correção
  de uma das dez dicas sincroniza o fato correspondente sem descartar extras;
- revisão otimista bloqueia outra aba, resposta atrasada ou mudança externa
  no arquivo/`indice.json` entre abrir, salvar, validar e aplicar;
- validação executa `validarBaralho` e, para o catálogo, `validarCatalogo`
  pelo Gradle real, offline e sem janela adicional;
- aplicar exige confirmação e validação do candidato exato, incrementa versão,
  cria backup único, usa temporário exclusivo e faz rollback em falha;
- exportar rascunho novo também exige validação do conteúdo exato;
- remover um baralho existente exige digitar o id exato, cria backup único e
  preserva os vizinhos; no catálogo, arquivo e entrada do índice são tratados
  como uma única operação com rollback;
- publicar no Firestore cria versão e blocos imutáveis em um commit atômico;
  mesma versão com conteúdo diferente e rollback de versão são recusados;
- baralhos comuns são publicados como `PUBLICO`; `ESPECIAIS` são `PRIVADO` e
  exigem um UID ativo em `leitoresCatalogo` no aparelho leitor;
- retirar do catálogo em nuvem muda somente `publicado` para `false`; nenhuma
  versão é apagada;
- publicação do catálogo é manual e confirmada; geração do APK continua fora
  do painel.

## Segurança de dados

Os caminhos enviados pelo navegador são chaves opacas. Arquivos do catálogo
são resolvidos e precisam permanecer dentro de `baralhos/`; links/reparse que
escapem são ignorados no inventário e recusados no staging. IDs de cards novos
recebem sufixo aleatório e não são reaproveitados após remoção. IDs de novos
baralhos não podem colidir com asset, catálogo ou outro rascunho.

Backups usam diretórios exclusivos e nomes diferentes para baralho, índice e
asset. A escrita atômica cria um temporário irmão exclusivo, sem reutilizar um
arquivo `.parcial` preexistente.

Os feedbacks importados manualmente ficam em
`%LOCALAPPDATA%/QuemSou/administrador/feedbacks`. O painel guarda somente o
contexto necessário à revisão editorial; identificadores de sessão não são
persistidos. Feedbacks gerais de card são contabilizados como ignorados nessa
visão, porque não podem ser associados com segurança a uma dica específica.

Na rota automática, o Android mantém o Room como fonte de verdade e usa uma
fila WorkManager condicionada a rede. Só avaliações `DICA_REVELADA` chegam à
coleção `feedbacks`; resposta correta, id de sessão e as outras dicas não saem
do aparelho. O painel recebe do servidor local apenas `projectId` e a chave
pública do cliente Android, autentica-se anonimamente direto no Firebase e só
pode ler e administrar se houver um documento `admins/{uid}` com
`ativo = true`. As regras negam exclusão e qualquer outro caminho por padrão.
`google-services.json` é local e ignorado pelo Git.

No catálogo, o navegador divide o JSON validado em blocos de no máximo 25
cards, calcula SHA-256 por bloco e pelo conteúdo completo e grava tudo junto com
o manifesto. O app reconstrói e verifica o mesmo JSON antes do parser/Room. A
categoria `ESPECIAIS` é privada por padrão para não expor conteúdo personalizado.

## Acervo de respostas e dicas (2026-09-19)

Implementação concluída após correções do Codex sobre o trabalho do Claude Code.
**Rules publicadas e migração concluída em produção em 2026-09-19**, no projeto
`borajogar-app`. O catálogo/feedback anteriores continuam separados.

- Uma resposta possui até 500 dicas, contando também as desativadas; corrigir
  preserva o id. A partida continua offline, com seleção de dez dicas e
  histórico no host. A fábrica permanece em hold.
- A prévia lê as bibliotecas privadas locais, exclui técnicos por padrão,
  relata origens indisponíveis, colisões e bancos acima do teto. Não gera cards.
  Alias legado usa a mesma normalização/identidade histórica do aplicativo.
- A migração cria cada resposta e seu banco em um commit atômico. Não
  sobrescreve bancos completos. Um pai incompleto pode ser retomado com
  criação apenas das dicas faltantes, preservando correções remotas.
- Ao abrir uma resposta cadastrada, o formulário mostra o conteúdo REMOTO,
  não a cópia local antiga. Se existe rascunho local, a pessoa confirma o uso
  do remoto; o arquivo local é preservado, sem merge automático.
- Salvar envia somente dicas alteradas e avança a revisão do pai no mesmo
  commit. Precondições impedem sobrescrita concorrente. Falha/conflito mantém
  o texto na tela e exige reabrir/conferir; nunca adota revisão nova para
  reenviar silenciosamente o texto antigo.
- Sem Firebase configurado, a edição continua como rascunho local. Com
  Firebase configurado mas consulta negada/incompleta, edição remota fica
  bloqueada até reabrir/conferir. Publicar exige banco completo e validado.
- Busca e páginas de 25 dicas preservam edições; trocar resposta/fechar pede
  confirmação quando há texto não salvo. Falha ao acrescentar não apaga
  a nova dica. O poll de feedback não reconstrói o formulário durante digitação.
- Feedback é associado por resposta + dica; importações sem respostaId usam
  referências exatas baralho/card. A nuvem usa baralho/card/dica, sem colisão
  entre respostas. Copiar pedido para Codex não chama IA automaticamente.
- Na Biblioteca, **Trazer bancos de dicas do Firestore para este baralho**
  projeta no rascunho do destino. Cards sem banco completo/quantidade mínima
  ficam intactos, com contagem informada. A seguir: validar → aplicar na
  origem → validar o conteúdo aplicado → publicar, sempre explicitamente.
- Projeção introduz somente o alias histórico em cards legados; IDs explícitos
  continuam imutáveis. O servidor registra as identidades autorizadas sem
  flexibilizar a edição manual. Preserva vizinhos, campos desconhecidos e
  metadado de escopo; PRIVADO é barrado em baralho público, inclusive após
  aplicação/reabertura e tentativa de mudar categoria.
- Regras novas: admin-only, índice de até 500 identidades únicas no pai,
  sem exclusão/reaproveitamento; dica precisa pertencer ao índice e avançar o
  pai atomicamente. Leitores do catálogo não leem o acervo editorial.
- Projeção HTTP tem teto de 4 MiB por chamada; publicação tem guardrail
  operacional de 9 MiB por commit. Um baralho muito grande pode ser recusado
  mesmo estando dentro da contagem de cards/dicas. Não há publicação parcial.

### Evidência local e limites

- Suítes Python/Node e regressões do formulário: resultados atuais no handoff.
- Emulador Firestore real v1.21.0, projeto fictício `demo-quemsou-acervo`,
  sete cenários: 500 dicas/paginação; edição de uma dica (pai + dica);
  501 recusada antes da rede; IDs especiais; migração interrompida/retry;
  retomada de pai incompleto; negações das Rules e conflito de revisão.
- Gradle real opt-in: projeção legada e editorial, banco de 500, validação,
  aplicação e preparo da publicação, somente em staging. Não publica.
- Smoke visual em servidor isolado sem Firebase: 500 dicas em 20 páginas,
  feedback importado sob a dica correta, texto preservado ao paginar,
  salvamento/recarga, recusa de 501 sem perda do texto e botão de projeção.
  Edição/conflitos remotos cobertos por teste de estado + transporte/Rules
  no emulador; a etapa de produção abaixo não alterou textos para testar edição.
- A etapa local não publicou Rules/dados nem alterou fontes reais. Não houve
  geração de dicas reais, build/instalação de APK, commit ou push.

### Ativação em produção (2026-09-19)

- Após autorização explícita, a Central foi reiniciada em
  `http://127.0.0.1:8766`, preservando o diretório privado e sem edição aberta.
- Publicação pelo Firebase CLI limitada a `firestore:rules`, projeto explícito
  `borajogar-app`; exit 0. Leitura posterior da release confirmou conteúdo igual
  ao arquivo local. SHA-256 publicado:
  `da1418d07e9ef4f231c07f061e95bbaf3fd22fc12df30bbecc4de5d7d8628020`.
- A Central confirmou **195 respostas criadas, zero existentes e zero erros**.
  Releitura completa: **1.980 dicas, 1.680 de escopo PUBLICO e 300 PRIVADO**,
  nenhum banco incompleto e nenhuma divergência de texto/escopo/status em
  relação à prévia. Todas as dicas editoriais continuam acessíveis só a admin.
- Os sete manifestos do catálogo mantiveram versões, hashes, publicação e
  visibilidade. Kimberly-Clark — Finanças continua privado. O feedback real
  existente manteve `updateTime` anterior à publicação, sem nova avaliação.
- Pela interface real, Shakira abriu como banco registrado no Firestore,
  com dez dicas. O feedback já existente apareceu automaticamente abaixo
  da dica correta, sem importação.
- Nenhum snapshot de baralho foi republicado, nem o conteúdo local das
  origens foi alterado. Corrigir o acervo continua exigindo incorporar no
  rascunho, validar, aplicar e publicar explicitamente para distribuir.
- Não houve operação no telefone, novo APK, commit, push ou geração de cards.
  Provas/recuperação da unidade: `HANDOFF_ACTIVE.md`.

Contrato: `CATALOG_FORMAT.md`. Próxima ação: `HANDOFF_ACTIVE.md`.

## Evidências de conclusão

As evidências abaixo (catálogo publicado, regras ampliadas, sete baralhos
migrados, feedback real do `mm_028` sincronizado, APK instalado no Samsung
SM-F966B) foram confirmadas pelo Codex em turnos anteriores desta mesma linha
de trabalho, antes do acervo editorial começar. Não são reevidenciadas a cada
unidade nova; ficam preservadas aqui como o estado real do catálogo/feedback
já existentes. O acervo editorial (`acervoEditorial/**`) é uma coleção NOVA,
separada, e seu próprio estado de publicação está descrito na seção acima
("Ativação em produção") — não deduzir a partir desta seção histórica.

- suíte rápida atual: **138 testes, OK; 3 skips esperados**, sem
  `ResourceWarning`. Dois skips são os casos de link simbólico, indisponíveis
  porque o Windows da sessão recusou a criação com erro 1314; o terceiro é a
  integração Gradle real, opt-in;
- os novos casos cobrem importação/deduplicação/sanitização dos feedbacks,
  configuração pública mínima do Firebase, endpoint HTTP, interface automática,
  associação por id estável ou texto legado, confirmação textual de remoção,
  preservação de vizinhos, backup e rollback;
- integração opt-in executada separadamente: **1 teste, OK em 65,795 s**;
  editou, validou e aplicou `mundo-dos-bruxos-1` somente em uma cópia de
  staging, passando por `validarBaralho` e `validarCatalogo`;
- hashes do asset e de todos os arquivos do catálogo reais foram comparados
  antes/depois da integração: **zero fontes reais alteradas**;
- `node --check administrador/estaticos/app.js`: exit 0;
- `:app:testDebugUnitTest` e `:app:assembleDebug`: `BUILD SUCCESSFUL` com a
  fonte Firestore do catálogo e os testes de integridade dos blocos;
- `validarCatalogo` aprovou os cinco baralhos do catálogo externo, inclusive
  Mundo dos Bruxos com 70 cards, sem divergências;
- compilação Android e **286 testes JVM Debug** passaram com Room v7 e a fila
  de sincronização; o Console do Firebase aceitou e publicou as regras locais
  (SHA-256
  `D39C21C88A46A028993FC501F65B1BBAA1844AC48D76491A8959A2E9614CC294`);
- após a publicação, o painel autenticado confirmou a leitura remota com
  `Firestore conectado • 0 feedback(s) sincronizado(s)`;
- o APK `0.5.0-dev-0919.1436` foi instalado como atualização no Samsung
  SM-F966B (`RQCY804H40K`), preservando o `firstInstallTime` e os dados locais;
- uma avaliação real feita pela interface do app na dica 10 do card `mm_028`
  (Shakira) sincronizou pelo WorkManager. A Central passou a mostrar
  `Firestore conectado • 1 feedback(s) sincronizado(s)` e exibiu **Boa dica**
  com o comentário `Teste sincronizacao Firestore 2026-09-19` diretamente sob
  a dica, na fonte **Firestore — automático**;
- smoke no navegador: biblioteca carregou com CSP ativa; criação de rascunho,
  card novo, salvamento, recuperação, confirmação ao reabrir com edição
  pendente e reprovação editorial funcionaram em dados temporários;
- inspeção visual em tema escuro confirmou contraste do botão principal e
  quebra de conteúdo longo. A nova inspeção confirmou a área de importação, o
  estado sem feedback sob cada dica e a ação de remoção destacada. O CSS
  reorganiza os painéis abaixo de 900 px;
- o smoke encontrou e corrigiu uma regressão adicional em que o evento de
  clique era interpretado como parâmetro interno do salvamento. O feedback
  final “Rascunho salvo” foi reconfirmado após a correção;
- o atalho real iniciou e encerrou o servidor de teste corretamente.

Os diretórios de QA ficam sob `build/`, ignorados pelo Git. O APK Debug foi
gerado e instalado como atualização no aparelho físico, sem limpar os dados.
Não houve commit nem push. O Firestore foi criado em produção, o provedor
Anônimo foi ativado, a configuração Android local foi validada e o admin do
painel foi criado. As regras ampliadas foram publicadas, o índice composto está
ativo e os sete baralhos distintos foram migrados: seis `PUBLICO` e
Kimberly-Clark — Finanças `PRIVADO`. A Central confirmou um feedback real por
sincronização automática. O UID anônimo do aparelho físico foi autorizado em
`leitoresCatalogo` com `ativo = true`.

Em 2026-09-20, o Fold SM-F966B/API 36 recebeu a atualização preservando dados
`0.5.0-dev-0920.0045`, após 292 testes JVM e assembleDebug passarem. Pela
interface Android, Mundo dos Bruxos passou de v2 para v3 e Mundo Pop de v1
para v2 via Firebase. Especiais exibiu Kimberly-Clark — Finanças; uma partida
privada de quatro rodadas chegou ao placar final. Os 121 feedbacks anteriores
permaneceram idênticos. Nenhuma avaliação artificial foi enviada.
O especial já estava instalado na mesma v1: sua visibilidade e uso foram
comprovados, mas **um novo download privado não foi executado**. Não se apagou
conteúdo para forçá-lo. Limites, evidência e ressalva de encerramento de sessão:
`HANDOFF_ACTIVE.md` e `BUGS.md`, seção 9.

## Uso

Instruções curtas e fluxo de operação: `administrador/README.md`. Para abrir,
dar dois cliques em `C:\Dev\QuemSou\Abrir-Administrador.cmd`. Fechar a janela
do servidor encerra o painel; rascunhos salvos continuam disponíveis na
próxima abertura.
