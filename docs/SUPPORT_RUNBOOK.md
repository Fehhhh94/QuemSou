# Runbook de suporte — QuemSou

Guia para diagnosticar problemas sem perder dados, confundir build antigo com
bug ou prometer evidência que não foi coletada.

## 1. Informações mínimas do atendimento

Antes de sugerir correção, registrar:

- versão exibida no rodapé da Home;
- modelo do aparelho e versão/API do Android;
- tela e ação exata em que ocorreu;
- resultado esperado e resultado observado;
- frequência: sempre, às vezes ou uma vez;
- se o app foi atualizado por cima ou instalado do zero;
- quantidade de jogadores, baralhos e rodadas quando envolver partida;
- estado da rede quando envolver catálogo ou espelho;
- screenshot ou vídeo curto, sem token, QR legível ou dados pessoais.

Não pedir “logs completos” por padrão. Colete a janela e as tags necessárias ao
sintoma para reduzir exposição e ruído.

## 2. Segurança antes de diagnosticar

- Não publicar token do espelho nem URL com `token=`.
- O endereço local `192.168.x.x` não é credencial, mas deve ser mascarado em
  material público se não for necessário.
- Não pedir `local.properties`, keystore ou arquivos de configuração pessoal.
- Limpar dados ou reinstalar remove baralhos baixados, feedback e preferências.
  Só propor depois de avisar e obter concordância.
- Não editar diretamente o banco Room de um usuário para “destravar” suporte.

## 3. Triagem rápida

| Sintoma | Verificar primeiro | Documento/caminho |
|---|---|---|
| App fecha ou não abre | versão, stack `AndroidRuntime`, instalação limpa/update | seção 6 |
| “Começar partida” bloqueado | nomes, baralhos selecionados, cards × rodadas | `GAME_RULES.md`, `SetupUiState` |
| Baralho não aparece | versão do asset ou download, estado do catálogo | `CATALOG_FORMAT.md` |
| Catálogo sem novidades | internet, cache, URL do índice, cache GitHub raw | `BUGS.md` |
| Jogo mudou ordem/pontos | seed, seleção e passos exatos; tratar como crítico | `GAME_RULES.md`, `domain/` |
| Partida voltou à Home | distinguir morte de processo de swipe nos recentes | `BUGS.md`, seção 7 |
| Switch do espelho desliga | Wi-Fi/interface local ou portas 8080–8089 | seção 4 |
| QR/URL não abre | mesma rede, isolamento de clientes, VPN, HTTP e porta | seção 4 |
| Nome indisponível | “este aparelho” ou sessão antiga ainda ocupando | seção 4 |
| ✓ não aparece | POST aceito, app ainda no Setup, mesma instância do servidor | seção 4 |
| Modo dev/feedback não aparece | Switch “Modo dev” e contagem na Home | `PROJECT_CONTEXT.md` |

## 4. Espelho de leitura

### Checklist de rede

1. Confirmar que o switch está ligado e a URL aparece no Setup.
2. Confirmar que anfitrião e navegador estão na mesma rede Wi-Fi/local.
3. Digitar exatamente `http://IP:PORTA`; não trocar por HTTPS.
4. Desativar VPN temporariamente para o teste.
5. Testar primeiro no navegador de um PC da mesma rede.
6. Se nenhum dispositivo alcançar o host, verificar “isolamento de clientes”,
   “AP isolation” ou rede de convidados no roteador.
7. Não concluir que é bug do app só porque outro dispositivo não alcança o IP;
   confirmar se a rede permite comunicação lateral.

### Sintomas do pareamento

**O switch volta a desligado:** o app não encontrou interface local aceita ou
nenhuma porta entre 8080 e 8089 pôde ser ocupada. Registrar a mensagem mostrada
e interfaces do aparelho antes de alterar código.

**O nome sumiu da lista web:** pode estar marcado como “este aparelho”; isso é
esperado. Desmarcar no Setup deve fazê-lo voltar em até três segundos.

**O nome está “já entrou”, mas a sessão foi perdida:** tocar no jogador no
Setup e confirmar “Liberar lugar”. O navegador novo poderá reivindicar depois
da próxima atualização da lista.

**A URL continua respondendo após sair do Setup:** defeito de ciclo de vida.
Registrar passos e horário; o servidor da parte 1 deve cair no `onCleared` do
`SetupViewModel`.

**SSE responde 401:** o par jogador/token não pertence à sessão atual. Voltar à
seleção ou limpar somente as chaves `quemsou.espelho.*` do site no navegador;
não é necessário limpar dados do app anfitrião.

### Limite atual

A parte 1 transmite somente `{"fase":"aguardando"}`. Não esperar dica ou
resposta no navegador até a parte 2 estar implementada.

## 5. Catálogo e baralhos

- A partida funciona offline com os baralhos já presentes.
- Catálogo remoto indisponível deve preservar o último índice válido e os
  downloads existentes.
- Depois de publicar versão nova no GitHub raw, cache pode atrasar a
  visualização; não repetir publicação nem reescrever histórico por impulso.
- Baralho inválido é recusado antes de entrar no Room. Rodar a régua local e
  guardar a saída exata:

```powershell
.\gradlew.bat validarBaralho -Parquivo=<arquivo.json>
.\gradlew.bat validarCatalogo -Ppasta=<pasta-do-catalogo>
```

Se `assets/cards.json` mudou e o aparelho não refletiu, conferir se o campo
`version` foi incrementado.

## 6. Coleta técnica Android

Usar um aparelho por vez ou indicar o serial explicitamente.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb -s <SERIAL> logcat -c
# reproduzir o problema
& $adb -s <SERIAL> logcat -d -v threadtime |
  Select-String "AndroidRuntime|com.quemsou.app|QuemSou|CardsImporter|Ktor|CIO"
```

Para confirmar pacote e versão instalada:

```powershell
& $adb -s <SERIAL> shell dumpsys package com.quemsou.app |
  Select-String "versionName|versionCode|firstInstallTime|lastUpdateTime"
```

Para morte de processo sem remover a task:

```powershell
& $adb -s <SERIAL> shell am kill com.quemsou.app
```

Depois, reabrir pelo ícone. Não usar `am start -n` para validar restauração.

## 7. Verificações locais do projeto

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Relatar separadamente:

- total de testes, falhas e ignorados;
- sucesso de compilação/APK;
- warnings relevantes;
- o que não foi validado em aparelho.

O JDK está fixado por `gradle.properties`. O wrapper possui customizações UTF-8;
se houver problema ao regenerá-lo, consultar `docs/BUGS.md`, seção 5.

## 8. Classificação e escalonamento

| Severidade | Critério | Ação |
|---|---|---|
| Crítica | perda/corrupção de dados, regra de pontos/seed errada, crash recorrente | preservar evidência, parar entrega e investigar imediatamente |
| Alta | partida impossível, servidor órfão, conteúdo secreto exposto | isolar cenário, criar regressão e bloquear commit |
| Média | recurso acessório falha com alternativa disponível | registrar, diagnosticar e priorizar pela frequência |
| Baixa | texto, alinhamento ou melhoria sem bloqueio | registrar em `IMPROVEMENTS.md` |

Escalonamento deve conter:

```text
Resumo:
Versão/modelo/API:
Pré-condições:
Passos mínimos:
Esperado:
Observado:
Frequência:
Evidência anexada:
Dados preservados/perdidos:
Hipótese confirmada ou ainda não confirmada:
Validações já executadas:
```

## 9. Critério de encerramento

Um atendimento só fecha quando:

- o sintoma e a causa estão separados;
- há correção, orientação reproduzível ou limitação conhecida;
- teste de regressão foi adicionado quando aplicável;
- evidência corresponde ao nível afirmado;
- documentação dona foi atualizada;
- nenhuma limpeza, commit, push ou publicação ocorreu sem autorização.
