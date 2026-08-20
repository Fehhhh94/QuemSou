# Handoff ativo — Fase 4A, parte 1

> Documento operacional curto. Confirme sempre contra Git e código real.
> Atualizado em 2026-08-20.

## Objetivo confirmado

Entregar o pareamento do espelho de leitura no Setup: servidor HTTP local,
cliente web, QR, reivindicação de jogadores, marcador “este aparelho” e
liberação manual de sessão morta. Nenhuma dica ou evento de jogo nesta parte.

## Estado real do Git nesta entrega

- Repositório: `Fehhhh94/QuemSou`.
- Branch: `main`.
- `HEAD` na abertura: `e1b4b8d`, igual a `origin/main` antes desta entrega.
- A Fase 4A parte 1 está no commit local `2c09cdc`.
- Esta reorganização documental é registrada no commit que contém este
  handoff.
- Nenhum desses commits foi enviado ao remoto.
- `.claude/settings.local.json` é configuração pessoal e deve ficar fora do
  Git.

Antes de continuar, executar novamente a checagem de
`docs/AI_COLLABORATION.md`; estes valores podem ficar antigos.

## O que está implementado

- Dependências Ktor CIO 3.2.4 e ZXing core.
- `data/espelho/`: registro puro, descoberta de endereço e servidor.
- Cliente offline em `assets/espelho/`.
- Setup com switch, QR, URL, estados “entrou”/“aguardando”/“este aparelho”.
- Toque contextual na linha e `ConfirmDialog` para liberar sessão.
- Proteção contra duplo toque durante a subida do servidor.
- Tentativa real das portas 8080–8089, com limpeza de falhas e cancelamento.
- SSE autenticado pelo par jogador + token.
- Documentação operacional compartilhada para Codex e Claude.

## Decisões vigentes

- Nada do espelho entra em `domain/` ou em `ConfiguracaoDaPartida`.
- O jogo pode começar sem espelho e sem jogador conectado.
- Um único jogador pode ser “este aparelho”; ele some da lista web.
- Token de sessão fica no `localStorage`; nunca deve aparecer em suporte.
- Nesta parte, sair do Setup derruba o servidor.
- Nunca fazer push.

## Evidência executada

| Verificação | Resultado |
|---|---|
| `./gradlew test` | 230 testes no Debug e os mesmos 230 no Release; 0 falhas, 0 erros e 0 ignorados |
| `./gradlew assembleDebug` | sucesso |
| `node --check assets/espelho/app.js` | sucesso |
| `git diff --check` | sem erro |
| Links locais dos Markdown | todos os alvos existem |
| Espaços finais nos Markdown | nenhum encontrado |
| `.claude/settings.local.json` | ignorado pelo Git |
| Alterações em `domain/` | nenhuma |

## Pendente

1. Validar fisicamente em aparelho e navegador na mesma rede:
   - ligar/desligar o switch;
   - ler QR e abrir URL;
   - entrar com um nome e ver ✓ ao vivo;
   - marcar/desmarcar “este aparelho” e confirmar que some/volta na web;
   - liberar lugar e reentrar;
   - sair do Setup e confirmar que a URL para de responder.
2. Registrar aparelho, API, rede e resultado no handoff/changelog.

## Próxima ação segura

Executar a validação física da lista acima. Se houver falha, coletar somente os
dados do `docs/SUPPORT_RUNBOOK.md` e corrigir a causa com regressão adequada.

## Fora de escopo ou não autorizado

- Fase 4A parte 2 (dicas/respostas durante a partida).
- Fase 4 Nearby Connections.
- Mudança em pontuação, seed, embaralhamento ou regras do jogo.
- Push, publicação ou alteração remota.
