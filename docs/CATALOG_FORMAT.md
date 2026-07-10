# Formato do Catálogo de Baralhos

Este arquivo é o dono do formato dos JSONs do catálogo estático (Fase 5A).
O repositório separado de baralhos DEVE seguir este formato; o app o valida
com o `ParserDoCatalogo` (`data/catalogo/`), que devolve violações legíveis
em português — nunca exceção crua.

## Visão geral

- O catálogo é **um arquivo índice** + **um JSON por baralho**, hospedados
  estaticamente (ex.: GitHub raw/releases). Sem servidor, sem chave de API.
- `assets/cards.json` embarca baralhos no **mesmo formato do baralho do
  catálogo**, dentro de um envelope com `version` (o versionamento do
  `CardsImporter`).
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
      "nome": "Cinema Clássico — Edição 1",
      "categoria": "PERSONAGEM_FILME",
      "colecao": { "id": "cinema-classico", "nome": "Cinema Clássico", "icone": "🎬" },
      "versao": 1,
      "estado": "FINALIZADO",
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
| `categoria` | string | `PERSONAGEM_FILME` ou `MUNDO_DA_MUSICA` (a categoria é metadado do baralho; não existe categoria "LIVRE"). |
| `colecao` | objeto | Coleção do baralho — ver "Coleção" abaixo. |
| `versao` | int ≥ 1 | Cresce a cada publicação de conteúdo novo do MESMO baralho; dirige a atualização por download. |
| `estado` | string | `EM_DESENVOLVIMENTO` (pode mudar entre versões; selo "em evolução") ou `FINALIZADO` (imutável para sempre; selo "edição final"). |
| `quantidadeDeCards` | int ≥ 1 | Contagem declarada, para a tela listar sem baixar. |
| `url` | string | URL do JSON completo do baralho. |
| `descricao` | string | Uma frase curta para o card da tela de catálogo (pode ser vazia). |
| `tamanhoEmBytes` | long, opcional | Tamanho do JSON do baralho; a UI exibe "~12 KB" no meta. Ausente/0 = não exibido. |

## Coleção

Metadado de **agrupamento** — o nível 1 da tela de catálogo lista coleções
(ex.: "Cinema Clássico" 🎬 reúne "Edição 1", "Edição 2"…). Não é entidade
com regras próprias; aparece idêntica no índice e no JSON do baralho.

| Campo | Tipo | Regra |
| --- | --- | --- |
| `id` | string | Identificador estável da coleção (slug); agrupa os baralhos na UI. |
| `nome` | string | Nome de exibição da coleção. |
| `icone` | string | Um emoji (ex.: "🎬"). |

## JSON do baralho

O arquivo apontado pela `url` do índice. Metadados repetidos do índice +
os cards. **O card herda a categoria do baralho** — não existe campo
`category` por card.

```json
{
  "id": "cinema-classico-1",
  "nome": "Cinema Clássico — Edição 1",
  "categoria": "PERSONAGEM_FILME",
  "colecao": { "id": "cinema-classico", "nome": "Cinema Clássico", "icone": "🎬" },
  "versao": 1,
  "estado": "FINALIZADO",
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

- **Teto de 100 cards por baralho** (`Baralho.MAXIMO_DE_CARDS`): crescimento
  além disso vira baralho novo (ex.: "Harry Potter 2"), preferindo
  subtítulos temáticos quando fizer sentido.
- **Ciclo de vida**: `EM_DESENVOLVIMENTO` → `FINALIZADO`, sem volta. Um
  baralho `FINALIZADO` nunca muda de conteúdo — nem de `versao`; evolução só
  via novo baralho ou extensão.
- **Ids são para sempre**: o `id` do baralho e os `id`s dos cards nunca
  mudam entre versões — são a chave estável da união determinística
  (`Baralho.uniaoDeterministica`, ordena por id do baralho e id do card
  antes do embaralhamento por seed).
- Régua editorial por card: ver `docs/CARDS_GUIDE.md` (curadoria) e
  `ValidadorEditorial` (regras mecânicas).

## Envelope de `assets/cards.json` (baralhos embarcados)

```json
{
  "version": 3,
  "baralhos": [ { "...": "mesmo formato do JSON do baralho acima" } ]
}
```

- `version` é a versão do CONJUNTO embarcado (regra inegociável: editar
  cards = incrementar `version`, senão a mudança não chega ao banco Room).
- Cada item de `baralhos` segue exatamente o formato do baralho do catálogo
  (sem `url`, que é campo do índice).
