# Formato do Catálogo de Baralhos

Este arquivo é o dono do formato canônico dos JSONs e de sua projeção no
Firestore (Fase 5A). As origens privadas de baralhos DEVEM seguir este
formato; o app o valida com o `ParserDoCatalogo` (`data/catalogo/`), que devolve
violações legíveis em português — nunca exceção crua.

## Visão geral

- O arquivo índice + um JSON por baralho continuam sendo o formato de autoria,
  validação, intercâmbio e contingência.
- A distribuição principal no app usa Firestore: manifesto publicado, versão
  imutável e blocos de cards. O adaptador remonta o mesmo JSON canônico antes
  de chamar o parser; não existe uma segunda régua editorial.
- Room continua como fonte de verdade para a partida. Firestore nunca alimenta
  a UI de jogo diretamente.
- O APK não contém baralhos editoriais. `assets/cards.json` é somente um
  envelope vazio de compatibilidade; downloads vêm do Firestore.
- Enums viajam como string (`categoria`, `estado`, `type`); valor
  desconhecido é violação de formato, não crash — permite evoluir o catálogo
  sem quebrar apps antigos de forma ilegível.

## Índice do catálogo

Lista as entradas que a tela de catálogo exibe sem baixar os baralhos.

```json
{
  "baralhos": [
    {
      "id": "cinema-classico-1",
      "nome": "Cinema Clássico",
      "categoria": "PERSONAGEM_FILME",
      "colecao": { "id": "cinema-classico", "nome": "Cinema Clássico", "icone": "🎬" },
      "versao": 1,
      "estado": "EM_DESENVOLVIMENTO",
      "quantidadeDeCards": 30,
      "url": "https://raw.githubusercontent.com/<org>/<repo>/main/baralhos/cinema-classico-1.json",
      "descricao": "Personagens clássicos do cinema, do bruxo ao vilão de respiração pesada.",
      "tamanhoEmBytes": 21500
    }
  ]
}
```

Campos da entrada (todos obrigatórios, exceto `tamanhoEmBytes`):

| Campo | Tipo | Regra |
| --- | --- | --- |
| `id` | string | Identificador **estável e imutável** do baralho (slug); participa da chave de ordenação da união determinística. |
| `nome` | string | Nome de exibição. |
| `categoria` | string | `PERSONAGEM_FILME`, `MUNDO_DA_MUSICA` ou `ESPECIAIS` (metadado interno do baralho; não existe categoria "LIVRE"). |
| `colecao` | objeto | Coleção do baralho — ver "Coleção" abaixo. |
| `versao` | int ≥ 1 | Controle técnico, não exibido ao jogador. Cresce a cada publicação de conteúdo novo do MESMO baralho e dirige a atualização por download. |
| `estado` | string | Campo legado obrigatório por compatibilidade. Novos arquivos usam `EM_DESENVOLVIMENTO`; `FINALIZADO` continua aceito, mas não bloqueia edição nem atualização. |
| `quantidadeDeCards` | int ≥ 1 | Contagem declarada, para a tela listar sem baixar. |
| `url` | string | URL do JSON completo do baralho. |
| `descricao` | string | Uma frase curta para o card da tela de catálogo (pode ser vazia). |
| `tamanhoEmBytes` | long, opcional | Tamanho do JSON do baralho; a UI exibe "~12 KB" no meta. Ausente/0 = não exibido. |

## Coleção

Metadado de **agrupamento** — o nível 1 da tela de catálogo lista temas
relacionados. Não representa uma sequência de edições, não tem regras próprias
e aparece idêntico no índice e no JSON do baralho.

| Campo | Tipo | Regra |
| --- | --- | --- |
| `id` | string | Identificador estável da coleção (slug); agrupa os baralhos na UI. |
| `nome` | string | Nome de exibição da coleção. |
| `icone` | string | Um emoji (ex.: "🎬"). |

## JSON do baralho

Conteúdo personalizado local pode usar `categoria: ESPECIAIS` e coleção
`{ "id": "especiais", "nome": "Especiais", "icone": "⭐" }`. No Setup,
o agrupamento aparece depois dos temas gerais, com escolha individual de
cada tema personalizado. Nome de empresa não vira categoria técnica própria.
Apps anteriores à inclusão do enum recusam `ESPECIAIS`. Na distribuição em
nuvem, essa categoria é privada por padrão. Room está na versão 7; a categoria
continua persistida como texto.

O arquivo apontado pela `url` do índice. Metadados repetidos do índice +
os cards. **O card herda a categoria do baralho** — não existe campo
`category` por card.

```json
{
  "id": "cinema-classico-1",
  "nome": "Cinema Clássico",
  "categoria": "PERSONAGEM_FILME",
  "colecao": { "id": "cinema-classico", "nome": "Cinema Clássico", "icone": "🎬" },
  "versao": 1,
  "estado": "EM_DESENVOLVIMENTO",
  "cards": [
    {
      "id": "pf_001",
      "type": "PESSOA",
      "answer": "Harry Potter",
      "clues": ["dica 1", "dica 2", "dica 3", "dica 4", "dica 5", "dica 6", "dica 7", "dica 8", "dica 9", "dica 10"]
    }
  ]
}
```

Campos do card (todos obrigatórios):

| Campo | Tipo | Regra |
| --- | --- | --- |
| `id` | string | Único dentro do baralho (chave da união determinística junto com o id do baralho). |
| `type` | string | `PESSOA`, `LUGAR` ou `COISA`. |
| `answer` | string | Resposta secreta, não vazia. |
| `clues` | string[10] | Exatamente 10 dicas, nenhuma vazia, nenhuma contendo a resposta (régua editorial). |

## Regras de conteúdo (validadas pelo app e pela fábrica interna)

### Acervo de dicas por resposta (2026-09-12)

O card aceita dois campos adicionais opcionais: `respostaId` (identidade
canônica compartilhada entre baralhos) e `bancoDeDicas` (lista de objetos
`{ "id": "fato-estavel", "texto": "Uma dica autossuficiente." }`).

Quando presente, o banco exige `respostaId`, 10 a 500 dicas, ids únicos,
textos distintos após normalização, até 500 caracteres por dica, nenhuma
dica nomeando a resposta e todas as dez `clues` pertencentes ao banco.
A fábrica solicita pelo menos 60 dicas. Cards legados continuam aceitos:
suas dez dicas viram um acervo inicial que se esgota após o uso.

Novos arquivos usam `EM_DESENVOLVIMENTO`; ampliação incrementa `versao`
preservando ids. O valor legado `FINALIZADO` também é atualizável nas versões
atuais do app. Apps antigos ignoram os campos extras e só utilizam `clues`; o
controle de não repetição exige esta versão do aplicativo.

O histórico local nunca usa a versão ou o id do baralho para reciclar dicas.
O formato de download continua compatível: `cards` representa referências
editoriais de respostas na coleção; `clues` mantém dez dicas de compatibilidade.
No jogo, o app reúne os acervos da mesma resposta entre os baralhos instalados
e monta a carta da rodada. A união pode exceder 500 dicas; esse teto continua
valendo para cada item recebido, não para o snapshot interno da partida.
Reservas e aliases locais não são campos novos do contrato da fábrica.
O serviço conserva um acervo canônico por dono/resposta. Contrato operacional
e ativação: `DECK_STUDIO.md`.

- **Teto atual de 500 cards por baralho** (`Baralho.MAXIMO_DE_CARDS`).
- **Sem edição final**: todo baralho pode receber novos cards e correções até
  o teto. `versao` existe somente para o app detectar conteúdo mais novo; os
  valores antigos de `estado` são compatibilidade de formato, não ciclo de vida.
- **Ids são para sempre**: o `id` do baralho e os `id`s dos cards nunca
  mudam entre versões — são a chave estável da união determinística
  (`Baralho.uniaoDeterministica`, ordena por id do baralho e id do card
  antes do embaralhamento por seed).
- Régua editorial por card: ver `docs/CARDS_GUIDE.md` (curadoria) e
  `ValidadorEditorial` (regras mecânicas).

## Projeção versionada no Firestore

```text
catalogo/{baralhoId}                         manifesto mutável
catalogo/{baralhoId}/versoes/v{N}            cabeçalho imutável
catalogo/{baralhoId}/versoes/v{N}/blocos/00  cards imutáveis em JSON
```

O manifesto repete os metadados do índice e acrescenta `schemaVersion`,
`versaoId`, `quantidadeDeBlocos`, `hashDoConteudo`, `visibilidade`, `publicado`
e `atualizadoEm`. A versão repete os metadados necessários para reconstruir o
baralho. Cada bloco contém `ordem`, `quantidadeDeCards`, `conteudoJson` e seu
próprio `hashDoConteudo`.

- Publicação é um commit atômico: versão, blocos e novo ponteiro do manifesto.
- Cada bloco tem no máximo 25 cards e limite operacional de 700 KB; o teto do
  baralho continua em 500 cards.
- A mesma `vN` não pode receber outro conteúdo. Nova edição exige incrementar
  `versao`; rollback do ponteiro para versão menor é recusado.
- O app exige sequência contínua de blocos, confere SHA-256 de cada bloco e do
  JSON completo e só então executa `ParserDoCatalogo` e grava no Room.
- `publicado = false` retira o baralho do índice sem apagar a versão armazenada.
- `PUBLICO` exige usuário Firebase autenticado. `PRIVADO` exige também
  `leitoresCatalogo/{uid}.ativo == true`. O painel administrativo precisa de
  `admins/{uid}.ativo == true` para leitura e escrita.
- Baralhos `ESPECIAIS` são publicados como `PRIVADO`; os demais, como
  `PUBLICO`.

O campo `url` do índice continua existindo no modelo. No adaptador Firestore ele
é um endereço opaco `firestore://catalogo/{id}/versoes/vN`; não é uma URL Web.
Índices JSON estáticos podem continuar usando HTTPS em ferramentas e backups.

## Acervo editorial (upstream do `catalogo/**`, 2026-09-19)

Fonte administrativa separada dos snapshots validados que o app baixa:

```text
acervoEditorial/{respostaCodificada}
acervoEditorial/{respostaCodificada}/dicas/{dicaCodificada}
```

- IDs lógicos do jogo/feedback não mudam. Caminho e campo remoto
  `respostaId`/`dicaId` usam `id_` + base64url UTF-8 sem padding.
  Python e JavaScript têm o mesmo codec injetivo; leitura decodifica de volta
  antes de editar/projetar. Exemplo: `ação / 100%` → `id_YcOnw6NvIC8gMTAwJQ`.
  O codec provisório anterior não foi migrado em produção.
- Resposta: `schemaVersion:1`, `respostaId`, `texto`, `tipo`, `origem`,
  `revisaoTecnica` e `dicaIds` (ids codificados, únicos, máximo 500).
  `referencias` conserva pares baralhoId/cardId; notas/datas são opcionais.
- Dica: `schemaVersion:1`, `dicaId`, `texto` (até 500 caracteres),
  `escopo` PUBLICO/PRIVADO, `status` ATIVA/REMOVIDA, `origem`,
  `revisaoTecnica`. Corrigir/desativar mantém a identidade. Desativadas
  contam no teto para não permitir reaproveitamento de ids.
- Legado: respostaId lógico = resposta normalizada; dicaId lógico =
  `legado:<texto normalizado>`. Mesma identidade histórica do host.
- Uma resposta é completa quando todas e somente as dicas declaradas em
  `dicaIds` estão presentes. Toda escrita em dica avança o pai no mesmo
  commit; leitura paginada relê o pai para detectar alterações concorrentes.
- Migração atômica por resposta e criar-apenas: bancos completos não são
  sobrescritos. Recuperação de pai incompleto cria faltantes, preserva textos
  remotos e exige precondição do pai. Conflitos da prévia ficam de fora.
- Edição usa a base remota realmente carregada, diff de dicas e precondições
  updateTime/exists:false. Não remove documentos. A lista do pai nunca perde
  identidades. Regras admin-only também restringem o índice a 500 e exigem
  avanço atômico do pai; leitores comuns/privados não acessam o editorial.
- Projeção filtra dicas ATIVA por escopo e exige 10–500 elegíveis. Monta
  banco/clues determinísticos em rascunho; servidor autoriza somente as
  identidades projetadas, mantendo a edição manual restrita e o Gradle como
  validador do candidato exato. Metadado `escopo` acompanha o banco na
  origem local para impedir conversão privada → pública posterior; o JSON
  de distribuição continua usando id/texto, após essa validação.
- Não há edição final nem geração automática. Correção do acervo não
  republica baralhos consumidores; projeção/publicação é por baralho.
- Guardrails operacionais: até 501 escritas por commit editorial (pai + 500
  dicas), 9 MiB por commit, HTTP local de 4 MiB. O teto de 501 é escolha
  desta aplicação, não quota oficial do serviço. A documentação oficial
  estabelece 10 MiB por requisição: [quotas Firestore](https://firebase.google.com/docs/firestore/quotas).

Estado de ativação e operação: `ADMINISTRADOR_LOCAL.md`.

## Envelope de `assets/cards.json` (bootstrap vazio)

```json
{
  "version": 8,
  "baralhos": []
}
```

- A v8 substitui o envelope v7 com conteúdo real. A lista vazia avança o
  marcador de importação sem apagar dados já instalados nem o histórico.
- Não acrescentar conteúdo real ao asset; os testes de release exigem lista
  vazia. Instalações novas precisam de internet para o primeiro download.
- A biblioteca privada legada mantém o envelope anterior fora do Git, com
  os mesmos ids e bytes. A Central não escreve no asset do APK.
