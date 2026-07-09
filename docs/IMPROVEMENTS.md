# Improvements

## 🟣 Modo Shot (implementar após a 5.1)

- **Status**: backlog — especificação registrada em 2026-07-09, **não
  implementar agora**.
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

## 🟣 Fase 5.2 — Decisões da camada Gemini (registradas em 2026-07-09)

Decisões fechadas a partir das lições de uma integração Gemini já validada em
produção (app WakeSong). Implementar na 5.2; nenhuma delas altera as decisões
já vigentes (REST direto, chave do usuário no DataStore, sem Firebase).

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
