# Central de Baralhos

Painel local no navegador para consultar, editar e validar os baralhos desta
máquina. É separado da fábrica automática, que permanece em hold. O painel
não publica no GitHub, não gera APK e não instala nada no telefone. Uma ação
explícita pode publicar a versão validada no catálogo Firestore.

## Como abrir

Na raiz de `C:\Dev\QuemSou`, dê dois cliques em
`Abrir-Administrador.cmd`. O navegador abre em `127.0.0.1`; mantenha a janela
preta aberta enquanto estiver usando e feche-a para encerrar o painel.

Todo conteúdo local fica em `%LOCALAPPDATA%\QuemSou\administrador`, fora do
Git: `origens/embarcados-legado.json`, `origens/catalogo/`, rascunhos, feedbacks
e backups. A chave interna `asset:` foi mantida para preservar os rascunhos,
mas não aponta mais para o APK. Pastas dentro de repositórios são recusadas.

O Firebase continua distribuindo os baralhos. Uma instalação nova do app
precisa de internet para baixar os primeiros; depois joga offline. Atualizações
preservam conteúdo e histórico já instalados. Nenhuma edição exige incluir
cards no Git ou gerar um novo APK.

### Migração de outra máquina com conteúdo legado

Antes de esvaziar o asset antigo, com a Central fechada e sem edições pendentes:

```powershell
python -B administrador/migrar_conteudo_local.py --app C:/Dev/QuemSou --catalogo-legado C:/Dev/QuemSou-Baralhos --dados "$env:LOCALAPPDATA/QuemSou/administrador"
```

A operação copia e confere os bytes, cria `backups/separacao-git-v1/manifesto.json`
com hashes SHA-256 e recusa destinos divergentes. Não apaga nem publica.
Nesta máquina essa migração já foi concluída: **não repetir depois de editar
as cópias privadas ou esvaziar o asset**. O backup original permanece recuperável.
Configurações opcionais `--dados` e `--catalogo` também devem apontar para fora
do Git. Não usar o checkout `QuemSou-Baralhos` como origem ativa.

As versões antigas do GitHub ainda podem conter conteúdo. A separação atual
não remove histórico remoto, forks ou cópias anteriores.

## Fluxo de uso

1. Escolha uma origem e um baralho na biblioteca.
2. Edite o conteúdo desejado. Os valores antigos `EM_DESENVOLVIMENTO` e
   `FINALIZADO` são aceitos igualmente; não existe mais bloqueio de edição final.
3. Salve o rascunho. Isso ainda não altera a origem.
4. Valide pela régua real do app.
5. Para um baralho existente, confira o destino e use **Aplicar na origem
   local**. O painel incrementa a versão e cria backup antes de gravar.
6. Para um rascunho novo, valide e use **Exportar JSON**. Guarde o arquivo fora do Git e integre-o deliberadamente na origem privada
   do catálogo; então valide e publique.
7. Com o painel autorizado, valide novamente o conteúdo salvo e use
   **Publicar/atualizar no Firestore**. A confirmação informa versão, quantidade
   de cards e acesso público/privado.

A publicação cria uma versão imutável e aponta o manifesto para ela. Baralhos
`ESPECIAIS` são privados; os demais são públicos. **Retirar do catálogo no
Firestore** pede o id exato e apenas oculta o manifesto, sem apagar as versões.

## Feedbacks e revisão de dicas

Quando o Firebase está configurado, o app envia em segundo plano somente as
avaliações de dicas. O painel consulta esses registros ao abrir e novamente a
cada 30 segundos; não é necessário exportar ou importar uma lista.

1. Abra o painel e confira **Feedbacks do app**. Se ele ainda não estiver
   autorizado, use **Copiar UID do painel** e cadastre esse valor como o id do
   documento `admins/{uid}`, com o campo booleano `ativo = true`, no Firestore.
2. Abra o baralho. Abaixo de cada dica aparecem os votos e comentários da fonte
   **Firestore — automático**.
3. Em uma dica com feedback, use **Copiar pedido para o Codex** e cole o texto
   nesta conversa. O pedido traz o contexto da dica e as regras editoriais;
   não inclui o identificador da sessão do jogo.
4. Revise a sugestão do Codex, edite a dica manualmente, salve e valide antes
   de aplicar.

O botão apenas copia o pedido (ou baixa um `.md` se a área de transferência
for bloqueada). Ele não chama IA automaticamente e não altera a dica sem sua
revisão. Feedbacks gerais de card continuam contabilizados na lista, mas só
avaliações feitas em uma dica aparecem abaixo dela.

Para ligar a sincronização, o projeto Firebase `borajogar-app` precisa ter o
provedor **Anônimo** ativado, as regras de `firestore.rules` publicadas e o
arquivo do app Android salvo localmente em
`C:\Dev\QuemSou\app\google-services.json`. Esse arquivo é ignorado pelo Git.
Enquanto alguma dessas etapas faltar, o app continua funcionando e guardando
os votos no Room. A opção **Importação manual de contingência** aceita o export
`quemsou-feedback` v3 caso a nuvem esteja indisponível.

Para o catálogo, as regras e `firestore.indexes.json` precisam estar publicados.
Um aparelho autenticado pode ler baralhos públicos. Para receber também os
privados, seu UID deve existir em `leitoresCatalogo/{uid}` com `ativo = true`.

## Acervo de respostas e dicas

O acervo está ativo no Firebase desde 2026-09-19: 195 respostas e 1.980 dicas
migradas, com acesso editorial exclusivo de administrador. Ele é separado
dos baralhos distribuídos no catálogo e dos feedbacks.

1. Abra a seção **Acervo de respostas e dicas** e confira a prévia. Ela não
   grava nada nem gera cards; conflitos/técnicos ficam fora da migração.
2. **Migrar respostas seguras para o Firestore** cria
   respostas/bancos atomicamente. Repetir não sobrescreve bancos completos.
3. Escolha a resposta. Se já existe no Firestore, o formulário mostra o banco
   remoto; qualquer rascunho local divergente fica preservado e exige decisão.
4. Edite, acrescente, desative ou reative dicas. **Salvar dica** grava no
   Firestore quando o banco é remoto; sem configuração Firebase salva local.
   O teto é 500, incluindo desativadas. Busca/paginação preservam o texto.
5. Feedback fica sob a dica. **Copiar pedido para o Codex** prepara o pedido
   de revisão sem executar IA nem alterar automaticamente o conteúdo.
6. Na Biblioteca, abra o baralho e use **Trazer bancos de dicas do Firestore
   para este baralho**. Isso só prepara o rascunho. Confira cards ignorados.
7. Valide, aplique na origem, valide o conteúdo aplicado e publique
   explicitamente. Atualizações no acervo não republicam baralhos sozinhas.

Conflito/falha mantém o texto para conferência: reabra antes de tentar salvar
novamente. Nunca force uma revisão nova sobre texto antigo. Reinicie o servidor
local após atualizar o código. Detalhes e limites: `docs/ADMINISTRADOR_LOCAL.md`.

## Remover um baralho

Em um baralho existente, use **Remover baralho da origem local**. A tela pede
duas confirmações, incluindo digitar o id exato. Antes de remover, o painel
cria um backup em `%LOCALAPPDATA%\QuemSou\administrador\backups`.

Na biblioteca local legada, só o item escolhido é retirado e a versão do envelope sobe. No
catálogo, o arquivo local e sua entrada no índice são retirados juntos. Uma
falha restaura os arquivos do backup. A remoção não faz commit, push,
publicação, APK nem alteração no telefone. Rascunhos novos usam **Descartar
rascunho**, pois ainda não têm uma origem real.

Se o arquivo, o índice ou outra aba mudar a revisão em uso, a operação é
recusada para não sobrescrever trabalho mais novo. Uma edição posterior à
validação exige validar novamente.

## Limites deliberados

- nenhuma publicação automática, push, geração de card ou ativação da fábrica;
- nenhuma integração automática com Codex ou outro serviço de IA;
- nenhum autostart ou acesso pela rede: o servidor escuta só em `127.0.0.1`;
- o servidor local não recebe credenciais Firebase; o navegador se autentica
  diretamente e só administra se o próprio UID estiver em `admins`;
- no máximo 500 cards por baralho;
- ids, estado, versão e identidade dos cards não são editáveis;
- aplicar em qualquer origem altera só a cópia privada, nunca o APK;
- aplicar na cópia do catálogo não prova nem altera o remoto publicado.

## Testes de desenvolvimento

```powershell
python -B -m unittest discover -s administrador -v
node --check administrador/estaticos/app.js
node --check administrador/estaticos/acervo.js
node --check administrador/estaticos/acervo-firestore.js
node --check administrador/estaticos/acervo-ui.js
node --test administrador/estaticos/acervo.test.js
node --test administrador/estaticos/acervo-firestore.test.js
```

A suíte comum usa fixtures e Gradle simulado. A integração opt-in com Gradle
real está em `test_integracao_real.py` e exige uma cópia de staging do
catálogo; ela nunca deve receber o caminho do acervo real como destino.
`acervo-firestore.test.js` usa um transporte HTTP falso (não bate na rede);
um emulador Firestore local é complementar, opt-in, e nunca deve apontar
para o projeto de produção.

Arquitetura, segurança e evidências: [ADMINISTRADOR_LOCAL.md](../docs/ADMINISTRADOR_LOCAL.md).
