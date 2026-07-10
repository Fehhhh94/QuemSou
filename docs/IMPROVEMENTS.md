# Improvements

## ✅ Modo Shot (entregue)

- **Status**: **entregue em 2026-07-09** — regra vigente registrada em
  `docs/GAME_RULES.md` ("Modo Shot (regra opcional)") e no guia
  `docs/CLAUDE.md`; detalhes da entrega no `docs/CHANGELOG.md`. Validação
  física no Z Fold pendente. A especificação original fica abaixo como
  registro.
- `RegrasPartida`: `modoShot: Boolean = false` + `quantidadeDeShots: Int = 2`
  (1–3, visível na Configuração só com o toggle ligado).
- Posições-shot sorteadas por rodada com seed derivada da fórmula existente
  (seed da partida × 31 + rodada, de `Partida.seedDasDicas`) + fator próprio —
  determinístico, sobrevive à morte de processo, sincronizável na Fase 4.
- O shot é **pedágio**: quem escolheu o número bebe → toca "Bebi!" → a dica
  da posição aparece normalmente. Pontuação intocada (11 − N e a invariante
  dos 10 pontos seguem valendo).
- Grid **não marca** a posição-shot antes do toque (surpresa, coerente com o
  grid às cegas).
- Overlay "🥃 UM SHOT!" com o nome de quem bebe, botão "Bebi!", tratamento
  de insets via `BarraDeAcaoInferior`.
- Quem bebe: **sempre quem escolheu o número**, sem exceções.
- **Nota**: referência a álcool afeta a classificação etária na Play Store.

## 🟣 Fase 5 reestruturada — Catálogo de Baralhos (2026-07-09)

- **Status**: decisão de produto fechada em 2026-07-09 — a geração de cards
  por IA sai do app e vira ferramenta interna; o app consome um catálogo
  estático de baralhos curados. Arquitetura vigente em `docs/CLAUDE.md`
  ("Fase 5 — arquitetura"); antes/depois no `docs/CHANGELOG.md`. Próximo
  passo: sub-fase 5A.
- **Baralho**: nova entidade; pertence a uma categoria existente e agrupa
  cards tematicamente (ex.: `PERSONAGEM_FILME` → baralho "Harry Potter").
- **Teto de 100 cards por baralho**: crescimento além disso = novo baralho
  ("Harry Potter 2"), preferindo subtítulos temáticos quando fizer sentido.
- **Ciclo de vida**: `EM_DESENVOLVIMENTO` (versões novas podem adicionar,
  remover ou melhorar cards; o app atualiza por versionamento) →
  `FINALIZADO` (imutável para sempre; evolução só via novo
  baralho/extensão). O catálogo e a UI sinalizam o estado (selo "em
  evolução" vs "edição final").
- **Seleção múltipla no Setup**: a partida pode usar a união dos cards de
  1+ baralhos — mesma filosofia da categoria LIVRE (filtro-união, sem
  entidade "baralho mesclado" persistida).
- **Distribuição**: índice JSON estático + um JSON por baralho, hospedados
  estaticamente (ex.: GitHub raw/releases); sem servidor, sem Firebase, sem
  chave de API no app. Tela de catálogo lista, baixa e atualiza; import via
  o versionamento do `CardsImporter`. A partida segue 100% offline — rede
  só na tela de catálogo.
- **Sub-fases**: 5A — Catálogo (domínio + Room com migração, JSONs, tela de
  catálogo com selos de estado, seleção múltipla no Setup, união
  determinística na partida; validação no Z Fold) · 5B — Fábrica interna
  (pipeline Gemini → validação → revisão; o formato — script/CLI ou
  app-side oculto — é **decisão em aberto**, a fechar no início da 5B) ·
  5C — visão comercial (backlog, abaixo).

### 🟣 Visão 5C — baralhos customizados pagos (backlog)

- Baralhos customizados pagos sob demanda. **Restrição registrada**:
  conteúdo com marca registrada (Harry Potter etc.) **não pode ser
  vendido** — customizado pago só para temas próprios do cliente.
- MVP pode ser artesanal (pedido → fábrica interna → baralho no catálogo),
  sem Play Billing.
- **⚠️ Alerta de trademark**: também os baralhos **gratuitos** com nome de
  marca são risco a avaliar antes do lançamento na Play Store.

## 🟣 Editor manual de baralhos no app (fase futura, pós-5B)

- **Status**: registrado em 2026-07-09 — **não implementar agora**; reavaliar
  depois que a experiência da fábrica interna (5B) ensinar o fluxo real de
  criação de cards.
- **Caso de uso**: baralhos pessoais tipo "da nossa turma" — o usuário cria
  cards sobre os próprios amigos/família, sem passar pelo catálogo.
- Puxa a questão de **compartilhamento** entre aparelhos: exportação de
  arquivo ou envio via Nearby (sinergia com a Fase 4) — decidir junto.
- A régua editorial mecânica já existe (`ValidadorEditorial`); o editor a
  reusaria na digitação, com as violações legíveis existentes.

## 🟣 Fábrica interna — Decisões da camada Gemini (registradas em 2026-07-09)

Decisões fechadas a partir das lições de uma integração Gemini já validada em
produção (app WakeSong). Registradas originalmente para a antiga sub-fase 5.2
(geração dentro do app); com a reestruturação da Fase 5 (2026-07-09),
**continuam valendo integralmente, agora aplicadas à fábrica interna (5B)**.
Onde os itens abaixo citarem "tela de configuração"/DataStore do app,
leia-se a configuração equivalente da ferramenta interna — e a chave da API
passa a ser a do desenvolvedor, nunca do usuário final.

1. **Nome do modelo nunca hardcoded.** Vira campo editável na tela de
   configuração da 5.2, ao lado da chave da API, persistido no DataStore, com
   default in-code `gemini-2.5-flash`. Motivo: modelos Gemini são
   descontinuados sem aviso (`gemini-2.0-flash` → 404 em produção no app de
   referência); descontinuação futura se resolve trocando o texto na tela,
   sem release.

2. **Saída estruturada na fonte.** `generationConfig` com
   `responseMimeType: "application/json"` + `responseSchema` descrevendo o
   card (resposta + 10 dicas). Mesmo assim o parsing continua defensivo
   (remover cercas ``` ```json ``` se vierem, tolerar campos ausentes) — a
   validação estrutural pré-`Card` já registrada para a 5.2 permanece
   obrigatória.

3. **Thinking tokens competem com a resposta.** Modelos `gemini-2.5-*` gastam
   `maxOutputTokens` raciocinando antes de responder; com teto baixo, a
   resposta sai truncada (JSON cortado no meio) mesmo com a chamada retornando
   sucesso. Decisão: `maxOutputTokens: 4096` e thinking LIGADO (default do
   modelo) — para dicas factualmente corretas o raciocínio vale o custo, ao
   contrário do caso de referência (texto curto de humor). Se aparecer JSON
   truncado, thinking + teto é o suspeito nº 1.

4. **Mapa de erros → mensagens legíveis em português**, exibidas na tela
   (nunca exceção crua), na filosofia do `ValidadorEditorial`:
   - `400/403` → chave inválida ou sem permissão ("confira a chave na
     configuração").
   - `404` → modelo descontinuado/inexistente ("troque o nome do modelo na
     configuração").
   - `429` → cota esgotada. A mensagem DEVE explicar que o billing do Google
     AI Studio é uma fatura pré-paga separada (ter outros serviços Google
     pagos não alimenta essa cota) — a chave é do usuário, então esse
     diagnóstico é dele.
   - Timeout / sem rede → mensagem própria, com sugestão de tentar de novo.
   - **Resposta vazia = falha**, nunca sucesso silencioso.

5. **Timeout explícito de 60 s** na chamada HTTP (geração de card é resposta
   longa; o caso de referência usava 8 s para 2 frases).

6. **Sanitização de entrada do usuário no prompt.** Se a tela de geração
   aceitar tema/texto livre, o valor passa por sanitização antes de entrar no
   prompt (remover quebras de linha e caracteres de controle, colapsar
   espaços, trocar aspas duplas por apóstrofo, truncar por tamanho máximo) —
   defesa contra prompt injection e quebra de formato.

7. **Sem fallback de conteúdo.** Falha na geração NUNCA produz card
   genérico/inventado: o resultado de falha é um estado legível na tela
   (erro ou fila vazia), preservando a regra de que conteúdo só entra no Room
   após aprovação humana na tela de revisão.

## Texto do botão da tela Anuncio: "Próxima jogada"

- **Motivação**: feedback do teste de jogo físico (Z Fold) — "próxima
  jogada" é a linguagem que o grupo usa à mesa; "próximo turno" soava mais
  técnico.
- **Mudança**: só o texto exibido no botão (`R.string.partida_anuncio_proximo_turno`
  em `strings.xml`, de "Próximo turno" para "Próxima jogada"). Nomes de
  classes/funções do domínio (`Turno`, `PartidaViewModel.proximoTurno()`
  etc.) e o id do recurso de string permanecem inalterados — a mudança é
  só de linguagem voltada ao jogador.
