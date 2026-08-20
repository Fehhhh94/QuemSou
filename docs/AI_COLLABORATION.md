# Colaboração entre Codex/GPT e Claude Code

## Objetivo

Este protocolo evita contexto excessivo, disputa de autoria e mistura de
escopos quando Codex e Claude trabalham no mesmo repositório.

**Regra central:** um único agente executor escreve no worktree por vez. Outro
agente pode revisar em modo somente leitura ou assumir após handoff explícito.

## Fontes operacionais

| Arquivo | Responsabilidade |
|---|---|
| `docs/AI_RULES.md` | Regras estáveis de engenharia, segurança, evidência e Git |
| `docs/AI_COLLABORATION.md` | Autoria, revisão, retomada e encerramento |
| `AGENTS.md` | Bootstrap mínimo do Codex/GPT |
| `CLAUDE.md` | Bootstrap mínimo do Claude Code |
| `docs/DOCS_INDEX.md` | Roteamento para carregar só o contexto necessário |
| `docs/HANDOFF_ACTIVE.md` | Estado operacional e próxima ação concreta |
| `.claude/settings.local.json` | Permissões pessoais locais; nunca regra compartilhada |

Não duplique uma regra entre esses arquivos. Edite somente a fonte dona.

## Checagem inicial obrigatória

```powershell
git remote -v
git status --short
git log -1 --oneline
git diff --stat
git log origin/main..HEAD --oneline
```

Depois, o agente:

- confirma que o remoto é `Fehhhh94/QuemSou`;
- identifica alterações e arquivos não rastreados preexistentes;
- declara quais pertencem à tarefa e preserva os demais;
- não assume que arquivo sem commit foi criado por ele;
- interrompe a escrita se houver sobreposição de autoria sem solução segura.

Falha ao consultar o remoto não autoriza pull, reset, rebase ou push.

## Fluxo padrão

1. Entender o pedido e selecionar os documentos pelo `DOCS_INDEX`.
2. Para mudança não trivial, apresentar abordagem, riscos e validação antes de
   editar, salvo quando o Felipe já autorizou diretamente a implementação.
3. Implementar somente o escopo autorizado.
4. Executar validações proporcionais ao risco.
5. Revisar `git diff --check`, o diff e a lista final de arquivos.
6. Sincronizar a documentação conforme `DOC_SYNC.md`.
7. Mostrar os testes e aguardar a confirmação exigida antes do commit.
8. Nunca fazer push.

Diagnóstico, revisão e explicação não autorizam correção, commit ou ação externa.

## Papéis

### Executor

Pode editar e testar o escopo autorizado. É responsável por preservar o
worktree, deixar evidência e preparar o handoff.

### Revisor

Inspeciona código, diff ou commit sem editar. Prioriza correção, segurança,
regressão e testes ausentes, citando arquivo e linha. Só corrige se o Felipe
transferir explicitamente a execução.

### Sucessor

Continua trabalho interrompido. Lê o handoff, mas confirma tudo no Git e no
código real antes de editar; conversa anterior não é fonte de verdade.

## Handoff entre agentes

Quando a tarefa não terminar em um commit, atualizar `docs/HANDOFF_ACTIVE.md`
com:

```text
Objetivo confirmado:
Executor anterior:
Branch e HEAD:
Estado do worktree:
Decisões do Felipe:
Arquivos preexistentes preservados:
Arquivos alterados na tarefa:
Validações e resultados:
Validações ainda pendentes:
Próxima ação segura:
Ações não autorizadas:
```

O handoff deve ser curto e representar apenas a unidade ativa. História extensa
pertence ao `CHANGELOG.md`.

## Trabalho paralelo

O padrão é sequencial. Se Felipe pedir paralelismo real:

- cada executor usa branch e worktree próprios;
- escopos não alteram os mesmos arquivos;
- builds que compartilham cache ou aparelho são coordenados;
- integração acontece depois, com revisão deliberada.

Dois agentes nunca escrevem simultaneamente no mesmo worktree.

## Uso eficiente de contexto

- Carregar sempre apenas estas regras e o protocolo; depois usar o índice.
- Usar `rg` para localizar símbolo/seção antes de abrir arquivo grande.
- Não ler o changelog inteiro para descobrir estado atual.
- Não ler todos os bugs para uma tarefa de cards, nem toda a arquitetura para
  responder suporte simples.
- Preferir ponteiros à repetição de contratos.
- No fechamento, informar resultado, evidência, pendências e Git sem recontar
  toda a investigação.

## Checklist de encerramento

- [ ] Remoto e branch conferidos.
- [ ] Diff limitado ao escopo e mudanças preexistentes preservadas.
- [ ] Testes relevantes executados ou ausência declarada.
- [ ] Evidência manual não foi confundida com teste automatizado.
- [ ] Documentos donos sincronizados.
- [ ] Handoff atualizado se a unidade ficou sem commit.
- [ ] Nenhum commit, push ou publicação fora da autorização.
