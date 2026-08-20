# Sincronização da documentação

Cada fato tem uma fonte dona. Este processo evita documentos divergentes e
reduz o contexto necessário para pessoas e agentes.

## Princípios

1. Atualize o dono; outros documentos recebem apenas ponteiro ou resumo curto.
2. Estado atual não carrega narrativa histórica.
3. Changelog registra o que mudou e por quê, com data.
4. Handoff contém somente trabalho ativo, evidência e próxima ação.
5. Evidência automatizada e física nunca são tratadas como equivalentes.

## Matriz por tipo de mudança

| Mudança | Atualizar |
|---|---|
| Regra do jogo | `GAME_RULES.md`, testes e `CHANGELOG.md` |
| Arquitetura ou estado da fase | `PROJECT_CONTEXT.md`, `CHANGELOG.md` |
| Próxima ação ou evidência da tarefa | `HANDOFF_ACTIVE.md` |
| Bug novo/diagnóstico recorrente | `BUGS.md`, `SUPPORT_RUNBOOK.md` se operacional |
| Melhoria ou backlog | `IMPROVEMENTS.md` |
| Formato de catálogo | `CATALOG_FORMAT.md`, validadores e `CHANGELOG.md` |
| Regra editorial | `CARDS_GUIDE.md`, validadores e `CHANGELOG.md` |
| Fluxo de suporte | `SUPPORT_RUNBOOK.md` |
| Regra de agente/Git | `AI_RULES.md` ou `AI_COLLABORATION.md`, nunca ambos |
| Comando de build/entrada do projeto | `README.md` e fonte técnica aplicável |

## Atual versus histórico

- `PROJECT_CONTEXT.md`: apenas o que vale agora.
- `HANDOFF_ACTIVE.md`: apenas a unidade corrente.
- `BUGS.md` e `IMPROVEMENTS.md`: itens vivos e armadilhas ainda úteis.
- `CHANGELOG.md`: decisões e entregas datadas, inclusive as substituídas.

Ao substituir uma decisão, remova a antiga dos documentos atuais, registre o
antes/depois no changelog e atualize links que apontavam para a seção removida.

## Checklist documental

- [ ] O assunto tem um único dono explícito no `DOCS_INDEX.md`.
- [ ] README continua correto para uma pessoa chegando agora.
- [ ] Handoff corresponde a `git status`, HEAD e testes atuais.
- [ ] Números de testes têm evidência recente.
- [ ] Pendência física continua marcada como pendente.
- [ ] Nenhum segredo, token, credencial ou dado pessoal foi incluído.
- [ ] Links relativos apontam para arquivos existentes.
- [ ] `git diff --check` está limpo.

## Validação rápida

```powershell
rg -n "PENDENTE|CONCLUÍDA|testes|Fase 4A" docs
git diff --check
git status --short
```

Para mudança de código, essa checagem documental complementa — nunca substitui
— os testes da aplicação.
