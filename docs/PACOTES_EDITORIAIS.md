# Lotes editoriais embarcados

## 2026-10-02 — três novos baralhos de séries na Central local

Por pedido de Felipe, foram criados três lotes manuais no agrupamento existente
**Cinema e TV**, sem ativar a fábrica. Cada baralho tem vinte respostas e
duzentos fatos, dez por carta, com identidades de resposta e dicas estáveis.

| Baralho | Id | Versão local | Recorte |
| --- | --- | --- | --- |
| The Big Bang Theory | `the-big-bang-theory-1` | 1 | Série principal, doze temporadas |
| Stranger Things | `stranger-things-1` | 1 | Temporadas 1–4; exclui a quinta e obras derivadas |
| The Office (EUA) | `the-office-1` | 1 | Versão americana, nove temporadas; exclui séries derivadas |

As descrições avisam sobre spoilers. O lote de Stranger Things inclui um lugar
e uma criatura; Pessoa/Lugar/Coisa permanecem tipos internos. As sessenta
respostas foram conferidas antes da redação contra 314 ocorrências nas origens
locais, incluindo a biblioteca legada: zero colisões normalizadas. Não foram
copiados diálogos nem letras de música. A revisão corrigiu uma relação entre
irmãos por casamento e uma dupla de vendas, e substituiu pistas redundantes.

`validarBaralho` aprovou cada lote de vinte cartas. `validarCatalogo` aprovou
os nove baralhos do catálogo preparado e da origem depois da integração,
incluindo quantidades, versões e tamanhos exatos em UTF-8. Backup anterior à
integração em `backups/series-20261002-092301`: dezesseis arquivos anteriores
continuam idênticos por SHA-256; somente o índice existente mudou. Os seis
baralhos anteriores, a biblioteca legada e os nove rascunhos foram preservados.
Foram acrescentados três rascunhos de edição alinhados às novas origens, sem
aprovação reaproveitada. Os rascunhos anteriores conservam seu índice-base;
qualquer conflito posterior deve ser resolvido pela revisão da Central.

Conteúdo, fontes por resposta, manifesto com hashes, revisão HTML e recibo de
integração em
`%LOCALAPPDATA%/QuemSou/administrador/temporarios/series-20261002`.
Os novos arquivos vivem em `origens/catalogo/baralhos/`, fora do Git. O índice
usa o endereço opaco Firestore do contrato, sem criar URLs públicas fictícias.

Pesquisa pública consultada, com fontes específicas de cada resposta no manifesto:

- [The Big Bang Theory Wiki — personagens](https://bigbangtheory.fandom.com/wiki/List_of_The_Big_Bang_Theory_characters)
- [Stranger Things Wiki — personagens e acontecimentos](https://strangerthings.fandom.com/wiki/Mike_Wheeler)
- [Netflix Tudum — recorte e acontecimentos da quarta temporada](https://www.netflix.com/tudum/articles/stranger-things-season-4-recap)
- [Netflix Tudum — apresentação musical de Eddie](https://www.netflix.com/tudum/articles/what-song-does-eddie-play-stranger-things-season-4-finale)
- [Dunderpedia — personagens da versão americana](https://theoffice.fandom.com/wiki/List_of_The_Office_Characters)
- [Peacock — série, elenco e episódios](https://www.peacocktv.com/stream-tv/the-office/characters)
- [Peacock — Jim e Darryl como colegas de apartamento](https://www.peacocktv.com/watch-online/tv/the-office-superfan-episodes/8229469043710582112/seasons/9/episodes/vandalism-extended-cut-episode-14/4bf91ff5-a82f-3064-8e10-58fb93bb4c5e)

Validação de formato não comprova diversão, dificuldade ou precisão factual
humana de todas as dicas. Revisão durante partidas pendente. Não houve
publicação Firebase nem alteração no telefone. Para distribuir os lotes,
validar novamente o conteúdo exato pela Central e obter autorização explícita
de publicação. Friends local v2 e os demais conteúdos continuam preservados.

## 2026-10-02 — Friends: expansão local para 50 respostas

Felipe confirmou o pedido de mais vinte respostas para Friends. A origem
privada `friends-1` passa localmente de v1 para v2: cinquenta cartas, oitocentos
fatos. Novos ids `fr_031`–`fr_050`, dez dicas por resposta com `respostaId` e
ids de fatos estáveis. As trinta cartas anteriores e seus seiscentos fatos
foram preservados integralmente; não foram reduzidos seus bancos de vinte.
O lote combina personagens, um animal nomeado, um objeto e um local da série.
Pessoa/Lugar/Coisa continuam como tipos internos, no mesmo tema Cinema e TV.

Inventário de 294 respostas nas origens locais, incluindo a biblioteca legada,
sem colisão normalizada com as novas. Dicas redigidas em português, sem copiar
diálogos ou letras de música. Cada resposta nova tem dez fatos; não foi
reativada a fábrica nem aplicada a meta de geração automática de sessenta.

`validarBaralho` aprovou as cinquenta cartas. `validarCatalogo` aprovou a
pasta de preparação e a origem integrada, incluindo tamanho exato do JSON.
Backup integral em `backups/friends-expansao-20261002-084535`; índice e
rascunho de edição sincronizados. Quatorze arquivos das origens e dos
rascunhos anteriores permaneceram idênticos; somente arquivo Friends,
índice e seu rascunho de edição mudaram. Conteúdo, revisão HTML, fontes por
resposta e manifesto em
`%LOCALAPPDATA%/QuemSou/administrador/temporarios/friends-expansao-20261002`.
SHA-256 do JSON integrado:
`8db916344ddd54d12bab25c85b5bb5203869bf4b7ffebb5123b1f88d4378a5c3`.

Fontes públicas consultadas na pesquisa e checagem dos fatos:

- [Friends Central — família e visitas de Nora](https://friends.fandom.com/wiki/Nora_Tyler_Bing)
- [Friends Central — família e visitas de Sandra](https://friends.fandom.com/wiki/Sandra_Greene)
- [Friends Central — episódios de Amy](https://friends.fandom.com/wiki/Amy_Greene)
- [Friends Central — episódios de Jill](https://friends.fandom.com/wiki/Jill_Greene)
- [Friends Central — família de Alice](https://friends.fandom.com/wiki/Alice_Knight)
- [Friends Central — história de Emma](https://friends.fandom.com/wiki/Emma_Geller-Greene)
- [Friends Central — processo de adoção](https://friends.fandom.com/wiki/Erica)
- [Friends Central — viagem à praia](https://friends.fandom.com/wiki/Bonnie)
- [Friends Central — namoro com o policial](https://friends.fandom.com/wiki/Gary)
- [Friends Central — vizinho e viagem](https://friends.fandom.com/wiki/Danny)
- [Friends Central — namoro com a aluna](https://friends.fandom.com/wiki/Elizabeth_Stevens)
- [Friends Central — pai da aluna](https://friends.fandom.com/wiki/Paul_Stevens)
- [Friends Central — cliente da Bloomingdale's](https://friends.fandom.com/wiki/Joshua_Burgin)
- [Friends Central — colega de apartamento](https://friends.fandom.com/wiki/Janine_Lecroix)
- [Friends Central — retorno ao trabalho](https://friends.fandom.com/wiki/Gavin_Mitchell)
- [Friends Central — reencontro do colégio](https://friends.fandom.com/wiki/Will_Colbert)
- [Friends Central — macaco de Ross](https://friends.fandom.com/wiki/Marcel)
- [Friends Central — cafeteria](https://friends.fandom.com/wiki/Central_Perk)
- [Parque Warner — cenário licenciado da cafeteria](https://www.parquewarner.com/blog/friends-photo-experience)
- [Friends Central — pelúcia de Joey](https://friends.fandom.com/wiki/Hugsy)
- [Friends Central — mãe biológica](https://friends.fandom.com/wiki/Phoebe_Abbott)

Validação automatizada não comprova diversão ou precisão factual humana de
cada dica. Revisão durante partidas pendente. Nenhuma publicação remota:
Firebase e Fold continuam com v1, trinta cartas e seiscentos fatos. Para
distribuir a v2, a Central precisa validar o conteúdo exato novamente e
receber autorização explícita de publicação.

## 2026-09-20 — Cinema e TV: Friends

Baralho privado de autoria `friends-1`, em desenvolvimento v1, criado por pedido
explícito de Felipe enquanto a fábrica automática permanece em hold. São 30
personagens, ids `fr_001`–`fr_030`, e 20 fatos originais por resposta (600 no
total). `clues` conserva dez fatos para compatibilidade e `bancoDeDicas` traz
os vinte; a partida sorteia dez no aparelho. Nenhum trecho de diálogo foi
copiado e nenhuma dica contém a resposta completa.

A varredura das 200 respostas presentes nas origens locais encontrou zero
colisões normalizadas. O arquivo candidato e o rascunho exato foram aprovados
pelo `validarBaralho` real. Isso comprova formato e regras automatizadas, não
diversão, dificuldade ou revisão factual humana de todos os 600 fatos.

Fontes públicas consultadas para personagens e episódios, com textos
reescritos em português:

- [Hachette — materiais licenciados de Friends](https://hachettebookgroup.com/wp-content/uploads/2024/01/Friends-TV-Show.pdf)
- [TVmaze — guia de episódios](https://www.tvmaze.com/shows/431/friends/episodeguide)
- [Lista de personagens e recorrências](https://en.wikipedia.org/wiki/List_of_Friends_and_Joey_characters)
- [Visão geral da série](https://en.wikipedia.org/wiki/Friends)
- [Time — guia de episódios e personagens recorrentes](https://time.com/3635722/friends-viewing-guide-for-newbies/)

O rascunho permanece em `%LOCALAPPDATA%/QuemSou/administrador/rascunhos`, com
cópias recuperáveis em `backups/friends-rascunho-*`. Após autorização explícita,
o conteúdo exato aprovado foi integrado ao índice privado com backup integral
`backups/friends-catalogo-20260920-201526`, e o catálogo completo passou no
`validarCatalogo` real. A Central aprovou novamente a origem aplicada e publicou
`friends-1` v1 como `PUBLICO` no Firebase. Auditoria posterior reconstruiu os
dois blocos, confirmou 30 cards/600 dicas, hash
`19fc891654c47cd49a63d8c02ba8fc05d277209ba64473a656e10df64732a85c`
e igualdade exata com a projeção local. Nenhum conteúdo entrou no Git. A
instalação no aparelho foi concluída em 2026-09-24 no Fold/API 36 pela interface
real do catálogo. A UI confirmou `Baixado`; o Room confirmou `friends-1` v1,
30 cards e 20 dicas por resposta, preservando feedbacks e histórico existentes.
Uma partida física controlada posterior, somente com Friends, completou duas
rodadas com respostas distintas. Cada turno sorteou dez dicas únicas do banco de
vinte e persistiu somente a dica revelada; reservas finais zero e nenhum
feedback artificial. Isso valida o mecanismo no aparelho, não substitui a
revisão humana de diversão, dificuldade e precisão dos 600 fatos.

## 2026-09-16 — Especiais: Kimberly-Clark — Finanças

Asset v6 acrescenta o tema `kimberly-clark-financas` à coleção `especiais`
(⭐ Especiais), categoria interna `ESPECIAIS`, em desenvolvimento v1.
30 respostas, ids `kcf_001`–`kcf_030`, dez dicas por resposta, fatos estáveis
`kcf_NNN_d01`–`d10`. Nenhum texto foi alterado em relação ao rascunho entregue
a Felipe. Os quatro baralhos anteriores e seus ids foram preservados.

A lista foi aprovada e Felipe autorizou seguir com a inclusão em Especiais.
Isso não representa validação de diversão ou aprovação oficial da empresa:
a revisão das dicas em partidas reais continua pendente. Uso pessoal/local,
sem informações confidenciais, sem catálogo remoto, sem fábrica ativada.

O tema mistura 10 respostas acessíveis de finanças, 10 da rotina técnica e
10 da empresa/marcas/produtos. O recorte de marcas pode ser global; não se
presume disponibilidade de toda linha no Brasil. Humor restrito à rotina
genérica de escritório, sem atribuir ocorrências a funcionários reais.

Fontes públicas consultadas na preparação das dicas:

- [Kimberly-Clark — apresentação](https://www.kimberly-clark.com/en-us/company/about)
- [Kimberly-Clark — portfólio](https://careers.kimberly-clark.com/pt-br/quem-somos/marcas)
- [História: primeira venda e 150 anos, comunicado de 2022](https://www.investor.kimberly-clark.com/node/35321/pdf)
- [Huggies — catálogo](https://www.huggies.com.br/mapa-del-site)
- [Huggies — núcleo absorvente e indicador](https://www.huggies.com/en-us/resources/parenting/diaper-rash/the-science-behind-huggies-diapers)
- [Intimus — produtos](https://www.intimus.com.br/produtos)
- [Intimus — campanha Se Joga encerrada](https://www.promo.intimus.com.br/)
- [Plenitud — produtos](https://www.vivaplenitud.com.br/produtos)
- [Plenitud — linhas Femme e Protect Plus](https://www.vivaplenitud.com.br/blog/incontinencia/incontinencia-urinaria-descubra-como-escolher-o-produto-certo)
- [Kotex — história no Smithsonian](https://americanhistory.si.edu/explore/exhibitions/menstrual-history-collection/pads)
- [Kotex — She Can](https://www.kimberly-clark.com/en-us/sustainability/kotex-she-can-initiative)
- [Kleenex — história](https://www.kleenex.com/en-us/about-us/our-history)
- [Sebrae — fluxo de caixa](https://agenciasebrae.com.br/economia-e-politica/cinco-dicas-para-organizar-o-fluxo-de-caixa-dos-pequenos-negocios/)
- [Portal NF-e — perguntas frequentes](https://www.nfe.fazenda.gov.br/portal/perguntasFrequentes.aspx?AspxAutoDetectCookieSupport=1&tipoConteudo=3Ow1nfTBzIo%3D)
- [Microsoft — referências de células](https://support.microsoft.com/pt-br/excel/create-or-change-a-cell-reference)
- [IFRS — IAS 37](https://www.ifrs.org/issued-standards/list-of-standards/ias-37-provisions-contingent-liabilities-and-contingent-assets/)
- [CVM — CPC 27](https://conteudo.cvm.gov.br/export/sites/cvm/menu/regulados/normascontabeis/cpc/CPC_27_rev_14.pdf)
- [IFRS — IAS 1](https://www.ifrs.org/issued-standards/list-of-standards/ias-1-presentation-of-financial-statements/)
- [PCAOB — responsabilidades e evidências](https://pcaobus.org/oversight/standards/auditing-standards/details/as-1000--general-responsibilities-of-the-auditor-in-conducting-an-audit)

## 2026-09-16 — primeiro lote para avaliação durante o jogo

Asset v5. Os dois baralhos anteriores da v4 foram preservados integralmente.
Os novos foram acrescentados para receber feedback em partidas reais.

| Pacote | Respostas | Dicas novas |
| --- | --- | --- |
| Animação | Woody, Rémy (Ratatouille), WALL-E, Nemo | 40 |
| Instrumentos | Violino, Trompete, Flauta transversal, Guitarra | 40 |

Cada referência traz dez fatos com ids editoriais. Guitarra é uma extensão da
identidade já instalada: as dez pistas novas não substituem as dez anteriores.
Demais respostas são novas. Cada pacote sozinho serve a uma partida de até
quatro rodadas se as respostas ainda tiverem dez dicas disponíveis; combinar
pacotes amplia a seleção. Isso não garante disponibilidade após consumo.

São pistas originais em português, sem letras de música. Fontes primárias
consultadas para personagens, instrumentos e mecanismos (não geração remota):

- [Pixar — Toy Story](https://www.pixar.com/toy-story)
- [Pixar — Ratatouille](https://www.pixar.com/ratatouille)
- [Pixar — WALL-E](https://www.pixar.com/wall-e)
- [Pixar — Finding Nemo](https://www.pixar.com/finding-nemo)
- [Yamaha — violino](https://www.yamaha.com/en/musical_instrument_guide/violin/mechanism/)
- [Yamaha — história do violino](https://www.yamaha.com/en/musical_instrument_guide/violin/structure/)
- [Yamaha — trompete](https://www.yamaha.com/en/musical_instrument_guide/trumpet/mechanism/)
- [Yamaha — história do trompete](https://www.yamaha.com/en/musical_instrument_guide/trumpet/structure/)
- [Yamaha — flauta](https://www.yamaha.com/en/musical_instrument_guide/flute/mechanism/)
- [Yamaha — história da flauta](https://www.yamaha.com/en/musical_instrument_guide/flute/structure/)
- [Yamaha — guitarra elétrica](https://www.yamaha.com/en/musical_instrument_guide/electric_guitar/mechanism/)

Validação estrutural não comprova diversão, dificuldade ou ausência de
paráfrases semânticas. Próximos lotes devem usar as avaliações de dicas para
revisão factual/clareza e variar os assuntos. Nenhum feedback foi inventado
nem usado como evidência de preferência de jogadores neste primeiro lote.
