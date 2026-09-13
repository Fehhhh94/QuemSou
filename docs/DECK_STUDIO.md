# Criação automática de baralhos

> Felipe autorizou a implementação da fábrica após o conceito de partidas e
> confirmou que ela pode depender deste computador ligado. Cliente e servidor
> foram ampliados e testados localmente. HTTPS, conta executora isolada e uma
> geração real continuam pendentes; implementação não significa serviço ativado.

## Fluxo implementado localmente

Home → **Criar baralhos** → tema, quantidade e preferências → **Pedir meu
baralho**. Estados: `NA_FILA`, `GERANDO`, `REVISANDO`, `PRONTO` ou `FALHOU`.
O app instala o resultado validado automaticamente ao sincronizar essa tela.
Não há compartilhamento de prompt nem importação manual nesse fluxo.

Antes do envio, o pedido completo e o snapshot opcional das avaliações ficam
no DataStore privado, fora do backup. Timeout não cria outro id: **Reenviar
tentativa guardada** envia exatamente o mesmo conteúdo. A consulta também
confirma uma tentativa já recebida pelo servidor. **Esquecer reenvio** exige
confirmação e não cancela um trabalho já aceito pelo computador.

O cache mantém os pedidos visíveis offline. **Pronto para jogar** só aparece
depois da instalação confirmada no Room. Um resultado inválido não bloqueia
os demais; **Tentar receber novamente** retoma a instalação. O cliente recusa
HTTP, redirecionamentos, versões divergentes e conteúdo inválido.

O serviço continua trabalhando com o app fechado. Ao voltar à tela, o app
consulta os pedidos; enquanto aberta, atualiza a cada 15 segundos. Não há
notificação push nem sincronização Android em background nesta entrega.

A avaliação após a rodada guarda voto, comentário, versão, carta preparada
e dicas reveladas. O pedido oferece a opção explícita de enviar essas
avaliações ao gerador. **Pedir mais dicas e aplicar avaliações** amplia o
mesmo baralho, preservando a identidade das respostas.
Acrescenta até 60 dicas por resposta, limitado ao espaço restante até 500;
atingido o limite de alguma resposta, a ampliação desse baralho é desabilitada.

## Histórico e qualidade

A garantia local é por instalação/celular anfitrião, não por pessoa física
ou nome do jogador. O espelho não tem histórico individual. Limpar dados ou
reinstalar pode apagar histórico; partidas anteriores à atualização não são
conhecidas. Um backup restaurado pode carregar o histórico.

Regras de consumo/esgotamento: `GAME_RULES.md`. Formato e compatibilidade:
`CATALOG_FORMAT.md`. Qualidade e paráfrases: `CARDS_GUIDE.md`.

## Serviço e provisionamento

`fabrica/server.py` mantém uma fila SQLite e executa o Codex CLI de forma
sequencial. O operador autentica o Codex no host. Modelo e esforço não são
substituídos pelo código; nenhuma chave da OpenAI vai para o APK.

O serviço escuta `/pedidos` somente em **127.0.0.1:8087**. O celular exige
proxy HTTPS com certificado confiável. O app não aceita HTTP ou
redirecionamentos. Não expor diretamente a porta HTTP.

O host deve usar uma conta de serviço isolada, sem acesso aos checkouts e
segredos pessoais do operador: o sandbox read-only impede escrita, mas não
substitui isolamento de leitura. Pedidos são dados externos não confiáveis.
O provisionamento desse isolamento faz parte da ativação pendente.

A listagem retorna somente metadados. O resultado é baixado por
`GET /pedidos/{id}/baralho`, autenticado pelo mesmo dono, e somente quando
a versão ainda não está instalada. Assim o polling não retransmite acervos.

| Variável do operador | Conteúdo |
|---|---|
| `QUEMSOU_CLIENTS_FILE` | Arquivo JSON privado: SHA-256 do token → id interno do dono |
| `QUEMSOU_DB` | Caminho do SQLite persistente, com backup privado |
| `QUEMSOU_CODEX_BIN` | Executável Codex; padrão `codex` |
| `QUEMSOU_PORT` | Porta loopback; padrão 8087 |

Gerar token aleatório de pelo menos 32 bytes por dono. O código entregue ao
celular tem a forma `https://endereco-do-gerador|token`. O servidor guarda
somente o hash na configuração. O token fica no armazenamento privado do
app, excluído do backup. Não registrar o código em logs, screenshots, Git,
URL de navegador ou linha de comando. Revogar removendo o hash e reiniciando
o serviço.

Com o ambiente preparado, executar `python fabrica/server.py` no host.
O worker usa `codex exec --sandbox read-only --ephemeral --output-schema`
em diretório temporário, com pedido pelo stdin e argumentos fixos.
Referência: [modo não interativo do Codex](https://learn.chatgpt.com/docs/non-interactive-mode).

Geração e revisão precisam ser aprovadas para liberar o baralho. Revisão
pelo modelo não equivale à curadoria humana. Cada dono tem seu acervo
canônico na tabela `respostas`; ele é reaproveitado entre baralhos privados.
Conteúdo de donos diferentes nunca é combinado. Até três pedidos pendentes
por dono; um reenvio com o mesmo id não cria outro. Interrupção vira `FALHOU`,
sem repetir automaticamente uma execução que possa ter consumido uso.
Id reapresentado com conteúdo diferente é recusado. A admissão da fila é
transacional, inclusive com pedidos simultâneos. A listagem não oculta pedidos
antigos após vinte entradas. Validações são explícitas e continuam ativas
com Python `-O`; erros devolvem códigos seguros, sem prompt ou credencial.

## Ativação pendente

O host escolhido é este computador, que precisa ficar ligado para receber e
gerar. Falta definir o acesso HTTPS privado também fora da rede de casa e
preparar a conta executora isolada com autenticação do Codex.

Nesta entrega não houve publicação, provisionamento de conexão, execução
real de geração pelo Codex ou teste ponta a ponta no celular. A tela mostra
a conexão necessária; não simula uma geração bem-sucedida.

### Preparação disponível

`fabrica/operador.py` prepara uma configuração privada por conta executora,
confere arquivos/executável e inicia o serviço. Não instala VPN, não faz login
no Codex, não altera modelo/esforço e não configura autostart do Windows.

1. Preparar a conta isolada e nela disponibilizar Python, Codex autenticado e
   os arquivos de `fabrica/`. Não usar os checkouts pessoais como diretório de execução.
2. Configurar um HTTPS privado para `127.0.0.1:8087`. Uma opção ainda não
   escolhida/instalada é [Tailscale Serve](https://tailscale.com/docs/features/tailscale-serve),
   com computador e celular na mesma rede privada Tailscale. O comando proposto
   é `tailscale serve --bg http://127.0.0.1:8087`; conferir a URL HTTPS resultante
   e o acesso privado. [Referência do comando](https://tailscale.com/docs/reference/tailscale-cli/serve).
3. Na conta executora, a partir da cópia do projeto, executar
   `python fabrica/operador.py preparar --url <URL-HTTPS-REAL>` e depois
   `python fabrica/operador.py verificar`. Substituir o marcador pela URL real.
   `--codex` permite indicar o caminho do executável. O teste de arquivos não
   comprova login, isolamento ou conectividade.
4. Executar `python fabrica/operador.py iniciar`. O padrão Windows guarda estado
   em `%LOCALAPPDATA%/QuemSou/fabrica`, privado dessa conta. Não sobrescreve pasta
   existente. O script informa somente o caminho de `conexao.txt`, nunca o token.
5. Transferir o código privado ao campo **Conexão** do app, sem colocá-lo em
   logs, mensagens públicas ou screenshots. Fazer um pedido de dez respostas,
   aguardar a revisão e conferir o recebimento, jogo e ampliação com avaliações.

Esses passos estão preparados para ativação; não foram executados com um
endereço real nesta unidade. Só depois do ciclo completo registrar operação
real validada, separadamente dos testes simulados.

## Validação

Plano de execução, casos e critérios de aprovação:
[DECK_STUDIO_TEST_PLAN.md](DECK_STUDIO_TEST_PLAN.md).

`python -m unittest discover -s fabrica -v`: fila, isolamento, idempotência,
revisão, repetição e SQL real da migração contra o schema Room exportado.
A geração usa fake nos testes.

`.\gradlew.bat test assembleDebug assembleDebugAndroidTest`: testes Kotlin,
APK e compilação do teste Android de upgrade/consumo/restauração.
`connectedDebugAndroidTest` depende de aparelho/emulador. A evidência atual
dos testes e da inspeção visual limitada está em `HANDOFF_ACTIVE.md`.
Pendentes: Fold, TalkBack, demais estados conectados da interface, HTTPS
real e pedido → Codex → recebimento → jogo → avaliação → ampliação.
