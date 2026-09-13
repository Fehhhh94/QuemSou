# Índice da documentação — QuemSou

Ponto de entrada para pessoas e agentes. A regra é carregar somente o contexto
necessário para a tarefa.

## Leitura obrigatória para alterações

1. `docs/AI_RULES.md` — regras estáveis.
2. `docs/AI_COLLABORATION.md` — autoria, Git e handoff.
3. Checagem inicial de Git definida no protocolo.

Depois, escolha a rota abaixo.

## Roteamento por tipo de tarefa

| Tarefa | Ler primeiro | Complementos quando necessários |
|---|---|---|
| Entender produto ou arquitetura | `docs/PROJECT_CONTEXT.md` | código citado pelo documento |
| Continuar a entrega atual | `docs/HANDOFF_ACTIVE.md` | arquivos citados no handoff |
| Investigar ou atender um problema | `docs/SUPPORT_RUNBOOK.md` | `docs/BUGS.md`, handoff ativo |
| Alterar regra do jogo | `docs/GAME_RULES.md` | `domain/`, testes do domínio |
| Alterar identidade da central, Setup, partida ou navegação | `docs/PROJECT_CONTEXT.md` | `docs/GAME_RULES.md` se houver regra |
| Alterar espelho de leitura | `docs/PROJECT_CONTEXT.md` | `data/espelho/`, `assets/espelho/` |
| Alterar catálogo ou download | `docs/CATALOG_FORMAT.md` | `docs/PROJECT_CONTEXT.md`, `data/catalogo/` |
| Criar ou revisar cards | `docs/CARDS_GUIDE.md` | `docs/CATALOG_FORMAT.md` |
| Pedidos automáticos e acervo de dicas | `docs/DECK_STUDIO.md` | `docs/GAME_RULES.md`, `docs/CATALOG_FORMAT.md` |
| Planejar validação da fábrica e dicas inéditas | `docs/DECK_STUDIO_TEST_PLAN.md` | `docs/DECK_STUDIO.md` |
| Validar catálogo externo | `docs/CATALOG_FORMAT.md` | tarefas `validarBaralho`/`validarCatalogo` |
| Consultar bugs conhecidos | `docs/BUGS.md` | `docs/SUPPORT_RUNBOOK.md` |
| Planejar evolução | `docs/IMPROVEMENTS.md` | `docs/PROJECT_CONTEXT.md` |
| Consultar história | `docs/CHANGELOG.md` | commits Git específicos |

## Fonte dona de cada assunto

| Assunto | Fonte dona |
|---|---|
| Regras para agentes | `docs/AI_RULES.md` |
| Protocolo de colaboração | `docs/AI_COLLABORATION.md` |
| Produto e arquitetura atuais | `docs/PROJECT_CONTEXT.md` |
| Próxima ação concreta | `docs/HANDOFF_ACTIVE.md` |
| Diagnóstico e atendimento | `docs/SUPPORT_RUNBOOK.md` |
| Regras do jogo | `docs/GAME_RULES.md` |
| Bugs e armadilhas conhecidas | `docs/BUGS.md` |
| Melhorias e backlog detalhado | `docs/IMPROVEMENTS.md` |
| Criação de cards | `docs/CARDS_GUIDE.md` |
| Contratos JSON do catálogo | `docs/CATALOG_FORMAT.md` |
| Cenários e critérios de teste da fábrica | `docs/DECK_STUDIO_TEST_PLAN.md` |
| Histórico datado | `docs/CHANGELOG.md` |
| Sincronização entre documentos | `docs/DOC_SYNC.md` |

## Documentos que não são contexto padrão

- `docs/CHANGELOG.md` é histórico extenso; não representa sozinho o estado
  vigente.
- `docs/CLAUDE.md` é uma ponte legada. As fontes atuais estão neste índice.
- `.claude/settings.local.json` contém configuração pessoal da máquina, fica
  ignorado pelo Git e nunca define política do projeto.
- Relatórios de build em `build/` e `app/build/` são artefatos locais.

## Privacidade documental

Não registrar em Markdown versionado tokens do espelho, credenciais, keystores,
dados pessoais, dumps completos de aparelho ou conteúdo privado. Em suporte,
use valores mascarados e compartilhe somente o trecho necessário.
