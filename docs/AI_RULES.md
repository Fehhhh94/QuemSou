# QuemSou — regras compartilhadas para agentes

Estas são as regras estáveis de Codex/GPT e Claude Code. Estado atual, tarefa
ativa e histórico não vivem aqui: use `docs/DOCS_INDEX.md`.

## Antes de agir

1. Leia este arquivo e `docs/AI_COLLABORATION.md` por completo.
2. Confirme `git remote -v`; só prossiga se o remoto for
   `Fehhhh94/QuemSou`.
3. Faça a checagem inicial de Git do protocolo de colaboração.
4. Consulte `docs/DOCS_INDEX.md` e abra apenas os donos relevantes.
5. Preserve mudanças locais existentes; silêncio não prova autoria.

## Produto em uma frase

QuemSou é um party game Android presencial: um leitor revela até 10 dicas e os
demais tentam adivinhar a resposta. A partida funciona offline; catálogo e
espelho de leitura são recursos acessórios, nunca requisitos para jogar.

## Invariantes do jogo

- Pontuação, seed e embaralhamento só mudam com autorização explícita e revisão
  de `docs/GAME_RULES.md`.
- Determinismo é sagrado: nunca usar `hashCode()`, `kotlin.random.Random` ou
  `java.util.Random` no fluxo determinístico. Usar `domain/rules/`.
- Mesma seleção de baralhos + mesma seed produz o mesmo monte.
- Todo turno distribui exatamente 10 pontos; empate final não tem desempate.
- O espelho de leitura apenas apresenta dados. Nunca vira fonte da verdade da
  partida nem altera `ConfiguracaoDaPartida` por estado de pareamento.

## Arquitetura e código

- Preserve Clean Architecture + MVVM e os pacotes existentes.
- `domain/` é Kotlin puro: zero Android, Room, Compose, Ktor ou detalhe de UI.
- UI nova usa Compose Material 3; ViewModels expõem estado imutável via
  `StateFlow`.
- Não bloquear a main thread com rede, banco, arquivos ou servidor local.
- Reutilizar padrões e componentes existentes antes de criar outra abstração.
- KDoc e comentários explicam contratos e decisões não óbvias, em português.
- Texto visível fica em `res/values/strings.xml`; cliente web offline é exceção
  autocontida em `assets/espelho/`.
- Mudança Room exige migration, schema exportado e teste de upgrade.

## Rede, catálogo e espelho

- A partida principal continua offline mesmo se catálogo ou espelho falharem.
- Catálogo remoto aceita somente conteúdo validado; JSON inválido nunca entra
  no Room. Formato: `docs/CATALOG_FORMAT.md`.
- Editar `assets/cards.json` exige incrementar `version`, senão o importador
  não reaplica o conteúdo.
- Ktor permanece em 3.2.4 enquanto o projeto usar Kotlin 2.1.x. Atualização
  conjunta e justificativa: `docs/BUGS.md`, seção 8.
- O SSE do espelho exige o par jogador + token da sessão. Nunca expor dica ou
  resposta apenas por um id previsível.
- Servidor local deve cair ao dono do ciclo de vida previsto e nunca impedir o
  início da partida.

## Segurança e privacidade

- Nunca versionar ou exibir tokens, senhas, keystores, credenciais,
  `local.properties` ou configurações pessoais de agente.
- Não colocar token de sessão do espelho em logs, docs, screenshots públicas ou
  relatos de suporte.
- Não orientar limpeza de dados/reinstalação antes de avisar que isso remove
  baralhos baixados, preferências e feedback local.
- Ações destrutivas, publicação, alteração remota e push exigem autorização
  explícita e escopo claro.

## Testes e evidência

- Testar a menor unidade que prova a mudança e depois a integração afetada.
- Bug corrigível em JVM recebe teste de regressão sempre que viável.
- Antes de qualquer commit, executar `./gradlew test` e mostrar o resultado ao
  Felipe. Só commitar após a confirmação pedida para aquela entrega.
- Compilação verde não prova comportamento físico, visual, Wi-Fi real,
  acessibilidade ou restauração no aparelho.
- Distinguir claramente: teste JVM, APK montado, emulador, aparelho físico e
  validação manual do Felipe.
- Nunca declarar validação física sem aparelho/modelo/API, cenário e resultado.

## Documentação

- Cada fato tem uma fonte dona; outros documentos apenas apontam para ela.
- Estado atual vai em `PROJECT_CONTEXT.md`; próxima ação em
  `HANDOFF_ACTIVE.md`; diagnóstico em `SUPPORT_RUNBOOK.md`; história datada em
  `CHANGELOG.md`.
- Decisão substituída sai do documento atual e entra como antes/depois no
  changelog. Histórico não é reescrito para parecer vigente.
- Sincronização obrigatória por tipo de mudança: `docs/DOC_SYNC.md`.

## Git e operações externas

- **Nunca fazer `git push`.** Push é sempre manual do Felipe.
- Não usar `reset --hard`, checkout destrutivo ou limpeza ampla para preparar o
  workspace.
- Não apagar nem sobrescrever mudanças de outra autoria.
- Commit em português no formato `tipo: descrição` (`feat`, `fix`, `docs`,
  `refactor`, `test`, `chore`).
- Antes de sugerir entrega, conferir `git log origin/main..HEAD --oneline` e
  informar branch, commits locais e arquivos pendentes.

## Comandos locais comuns

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat validarBaralho -Parquivo=<caminho>
.\gradlew.bat validarCatalogo -Ppasta=<raiz>
```

No Windows, o wrapper é customizado para UTF-8; não regenerá-lo sem seguir
`docs/BUGS.md`, seção 5.
