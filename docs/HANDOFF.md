# Huginn e Muninn: passagem de bastão

Documento para outra IA continuar o projeto de onde parou. Atualizado em 18/11/2026.

Leia tudo antes de mexer no código. A seção "Como trabalhar com o Christian" é tão importante quanto a parte técnica.

---

## 1. Resumo em 30 segundos

- **O que é:** um launcher de duas telas para o portátil **AYN Thor** (Android 13, tela de cima 1920x1080 e tela de baixo 1240x1080). O visual vem do **Fwip**, o app leitor de quadrinhos do próprio Christian.
- **Nomes:**
  - **Huginn:** o launcher no Thor (repositório `ChristianDonatti/huginn`).
  - **Muninn:** a parte de transferência de arquivos entre Thor, celular, PC e NAS. Ainda não começou (fase 3).
- **Quem faz o quê:** a IA escreve todo o código. O Christian dá a direção ("vibe-code"), instala o APK no Thor, testa e devolve um checklist preenchido.
- **Onde parou:**
  - Fases 0 e 1 entregues.
  - A fase 1.1 (build 7, de 30/10/2026) corrige a abertura dos jogos nos emuladores e acrescenta capas, tempo de jogo e outros ajustes.
  - O checklist `docs/testes/fase-1.1.md` ainda não voltou. **O primeiro passo é pedir ou processar esse checklist.**
- **Licença:** GPL-3.0 ou posterior, como o Fwip.

---

## 2. Como trabalhar com o Christian

- **Quem é:** estudante de Engenharia de Computação na PUCPR, em Curitiba.
- **Idioma e tom:** escreva em português do Brasil, com linguagem simples, direta e natural. **Nunca use travessão (—).** Prefira textos prontos para copiar ou imprimir.
- **Fluxo combinado:**
  - Cada etapa entregue termina com um **checklist de testes** em `docs/testes/fase-N.md`, com caixas `[ ]`, uma tabela quando fizer sentido e uma seção de impressões no fim.
  - O Christian preenche o checklist e devolve. Uma fase só fecha quando roda bem no aparelho.
- **Bugs:** ele relata de forma curta ("os jogos não abrem direito"). Peça detalhes objetivos: o relatório da aba Telas (botão "Copiar relatório"), prints, ou `adb logcat` com a tag `Huginn`.
- **O que ele gosta:** iniciativa ("já aproveite e implemente mais coisas") e funções inspiradas em outros launchers, principalmente o **Cocoon**, que é o que ele usa hoje no Thor.
- **O que ele faz:** só testa e decide. Não espere que ele edite código ou rode comandos de build.
- **Página do projeto:** existe uma página (artifact no claude.ai) com mockups, arquitetura e roadmap. Ele pediu que ela seja atualizada a cada mudança relevante. Link: https://claude.ai/artifact/DTrfNkYyF7m4jTHngp1con. Se você não tiver acesso a ela, avise e siga com o README e este arquivo.

---

## 3. Decisões já tomadas (não reabrir sem ele pedir)

- **Stack:** Kotlin, Jetpack Compose e Kotlin Multiplatform. O módulo `shared` vai servir ao Muninn depois.
- **Visual do Fwip:** carrossel com brilho atrás tirado da cor do item selecionado. Fontes **Unbounded** (títulos) e **Manrope** (texto).
- **Paleta:** Noite `#14110D`, Ardósia `#16314C`, Fiorde `#2A7FB0`, Ouro `#E1B552`, Mel `#F0CF72`, Papel `#F4EEE3`.
- **Ícones (par A, corvos em busto):**
  - **Huginn:** corvo virado para a direita, cor Papel sobre degradê azul, olho Ouro, com um corte no pescoço que lembra a dobradiça do Thor.
  - **Muninn:** espelhado, escuro sobre degradê dourado, com um cartucho no bico.
- **Duas homes:**
  - **Simples:** a atual, no estilo Fwip.
  - **Personalizada:** blocos no estilo Cocoon, iiSU e ZNeko (fase 4).
  - No primeiro uso, as duas aparecem lado a lado, sem nenhuma pré-selecionada.
- **Muninn no PC:** página web servida pelo Thor (`thor.local:8765`), sem app nativo por enquanto. No celular, o mesmo APK com um modo celular.
- **"Quanto tempo você tem?" e bateria em horas de jogo:** entram na fase 5.
  - Exemplo: "1 h 35 de GameCube" em vez de só a porcentagem.
  - Já existem `formatMinutes` e `batteryMinutes` em `shared/Durations.kt`.
- **Fora do escopo:** rede social, loja e mensagens.
- **Emulador padrão (desde a 1.1):** o emulador próprio (standalone) que estiver instalado, na ordem do ES-DE. O RetroArch só é usado quando não há emulador próprio para o console. A pessoa pode trocar por console (Ajustes) ou por jogo (página do jogo).
- **Pastas de ROMs:**
  - O Huginn usa "Acesso a todos os arquivos" (MANAGE_EXTERNAL_STORAGE), como o ES-DE.
  - A pasta também é **liberada pelo seletor do Android** (SAF) para o Huginn repassar a permissão aos emuladores, como o Cocoon faz.

---

## 4. Roadmap

Estimativa em dias de sessão com a IA programando.

| Fase | Conteúdo | Situação |
| --- | --- | --- |
| 0 · Fundação (2) | Projeto, tema, ícone, launcher nas duas telas | Entregue e aprovada |
| 1 · MVP (5) | Pastas de ROMs, biblioteca, emuladores, home simples, página do jogo | Entregue. A 1.1 aguarda teste |
| 2 · NAS e capas (3) | RomM no NAS, capas melhores, manuais em PDF, caixa 3D | Próxima. As capas do libretro já foram adiantadas na 1.1 |
| 3 · Muninn e modo celular (5) | Transferências entre Thor, celular, PC e NAS; pareamento por código de 6 dígitos ou QR; página no navegador do PC | A fazer |
| 4 · Home personalizada (3) | Blocos, edição, boas-vindas com as duas homes lado a lado | A fazer |
| 5 · Painel, tempo e bateria (5) | Painel em jogo na tela de baixo, "Quanto tempo você tem?", bateria em horas de jogo por emulador e jogo | A fazer |
| 6 · Sync e extras (3) | Sync de estatísticas e saves (só RetroArch no começo), widget, modo sofá | A fazer |
| 7 · Ponte de companions (4+) | Protocolo para mods de PC usarem a tela de baixo | A fazer |

**Ideias guardadas para depois:**
- Fechar a tampa salva o jogo.
- Passar o jogo do Thor para o celular no mesmo ponto (save state via Muninn).
- Captura das duas telas numa imagem só.
- Manuais com a leitura guiada do Fwip.
- Status do RetroAchievements no painel.
- Diário e retrospectiva de jogos.
- Estante física.

---

## 5. Contexto do Christian que importa para as próximas fases

- **Pastas de ROMs:** ficam no Thor em **3 pastas raiz**, cada uma dividida por console (alguns consoles se repetem entre elas). Os caminhos exatos ainda não foram informados; o checklist da fase 1 pedia isso.
- **NAS:** Asustor AS3302T v2 (ARM64, 2 GB de RAM, ADM 5). Ele está configurando o **RomM** via Docker/Portainer, que ainda não estava funcionando da última vez. A fase 2 depende disso: ele precisa passar o endereço e as credenciais do RomM.
- **Outros jogos que o launcher deve listar:**
  - Jogos de PC pelo **GameNative**. As regras do ES-DE já trazem o comando, por arquivos `.steam` nos sistemas steam/windows.
  - Apps Android que ele mesmo marcar como jogo. Isso já existe: na aba Apps, tocar e segurar, ou apertar Y.
- **No Thor:** ele usa o Wayfinder e o botão AYN, que continuaram funcionando com o Huginn como launcher padrão.

---

## 6. Repositório e build

- **GitHub:** https://github.com/ChristianDonatti/huginn, branch `main`. A IA anterior fazia commit direto na `main`.
- **CI (`.github/workflows/build.yml`):**
  - Cada push roda `./gradlew :shared:testDebugUnitTest assembleRelease`.
  - O APK sai como `huginn.apk` e é republicado na prerelease **`teste`** (Releases > "Versão de teste (build N)"). O Christian baixa de lá no Thor.
  - `versionCode` é o número do run e `versionName` é `0.0.<run>`, ambos vindos das variáveis `HUGINN_VERSION_CODE` e `HUGINN_VERSION_NAME`.
- **Assinatura:** chave de desenvolvimento pública de propósito, em `keystore/huginn-dev.jks` (senha e alias `huginndev`), para que cada build instale por cima da anterior.
- **Versões:** Gradle 8.14.3, AGP 8.9.1, Kotlin 2.1.20, Compose BOM 2025.02.00, compileSdk/targetSdk 35, minSdk 26, JDK 17.
- **Em caso de falha no CI:** o workflow publica os erros de compilação como anotações. No ambiente anterior dava para lê-los com:
  `gh api repos/ChristianDonatti/huginn/check-runs/<job_id>/annotations`
- **Limitação do ambiente anterior:** não dava para rodar Gradle localmente, porque Maven e o repositório do Google estavam bloqueados. O app só compilava no CI.
  - O módulo `shared` era testado localmente com o compilador Kotlin baixado do GitHub, usando um shim para `kotlin.test.Test`.
  - Se o seu ambiente tiver Gradle e o Android SDK, melhor: compile antes de dar push.
- **Commits:** mensagens em português, com corpo explicando o que mudou.

---

## 7. Arquitetura do código

**Pacote:** `io.github.christiandonatti.huginn`

### 7.1 Módulo `shared` (Kotlin puro, com testes em `commonTest`)

| Arquivo | O que faz |
| --- | --- |
| `LaunchCommand.kt` | Lê os comandos de lançamento do ES-DE (`%EMULATOR_X% %ACTION%=… %EXTRA_k%=… %DATA%=…`) e gera um `LaunchSpec`. Também tem `expandValue` (variáveis e `%INJECT%=arquivo`), `splitArrayValue` e `splitComponent`. Lê os 735 comandos do ES-DE sem erro. |
| `Titles.kt` | `cleanTitle`: tira extensão, tags `(USA)` e `[!]`, e desfaz "Zelda, The". Também `titleTags`, `regionOf` e `sortKey`. |
| `StoragePaths.kt` | Converte caminho em document id do SAF e vice-versa (`primary:ROMs/gba`, `1A2B-3C4D:...`). Também `safDocumentUri` e `percentEncode`. |
| `RomFiles.kt` | `referencedFiles`: lista os discos de `.m3u` e as faixas de `.cue`/`.gdi`, para esconder da biblioteca os arquivos que fazem parte de outro jogo. |
| `Systems.kt` | Apelidos de pasta (`3ds` → `n3ds`, `gamecube` → `gc`, `ps1` → `psx`...), nomes curtos, cor de cada console, `defaultCommand` (emulador próprio primeiro) e `emulatorName`. |
| `Durations.kt` | `formatMinutes` ("1 h 35") e `batteryMinutes`. |

### 7.2 App: lógica

| Arquivo | O que faz |
| --- | --- |
| `MainActivity` | Home na tela de cima (display 0). `singleTask` e categoria HOME. Ao abrir pela lista de apps, força o painel de baixo; pelo botão Home, não cobre um app aberto embaixo. Encerra a sessão de tempo de jogo no `onResume`. Em `onUserLeaveHint` e `onDestroy`, fecha o painel de baixo quando o Huginn não é o launcher padrão. |
| `BottomActivity` | Painel na tela de baixo (display #4, marcado como apresentação). Categoria SECONDARY_HOME, `singleTop`, com `taskAffinity` própria. Guarda `livesOn`, `visible` e uma referência fraca para `close()`. |
| `HuginnActivity` | Base das duas activities. Traz o seletor de pasta (`OpenDocumentTree`) com callback e converte o botão A do controle em confirmar (`Gamepad.translate`). |
| `DualScreen` | Lista as telas e abre activities numa tela com `ActivityOptions.setLaunchDisplayId`. `startOrError` devolve a exceção quando falha. Também tem `isDefaultHome`, `lastStartAt` e `markStart` (para distinguir "o Huginn abriu algo" de "a pessoa saiu"). |
| `AppRepository` | Lista os apps com ícone e cor (Palette) e a flag `flaggedAsGame` (categoria GAME do Android). O contador `version` sobe quando a lista muda. |
| `Rules` | Lê `assets/esde/rules.json`: cerca de 180 sistemas e 105 emuladores, do ES-DE (MIT). `installed(emulador)` acha o componente instalado (com cache). `installedEmulators()` alimenta a seção "Emuladores encontrados" dos Ajustes. |
| `Library` | Escaneia as pastas de ROMs: acha as pastas de console até 2 níveis abaixo da raiz e os arquivos até 3 níveis dentro de cada uma. Pula `media`, `bios` e similares. Esconde discos e faixas. Guarda o resultado em `filesDir/library.json`. Também `androidGames()` e `autoDetect()`. Depois de escanear, dispara `Covers.fetch`. |
| `StorageAccess` (em `Library.kt`) | Permissão de todos os arquivos e busca automática de pastas `ROMs`/`Emulation` nos volumes. |
| `FolderAccess` | Árvores SAF liberadas ao Huginn (`persistedUriPermissions`). `treeFor(path)` e `documentUri(tree, path)` geram `content://…/tree/<pasta>/document/<arquivo>`. `onPicked` torna a permissão persistente. |
| `GameLauncher` | Escolhe o emulador (`choice`/`systemChoice`) e monta a Intent a partir do `LaunchSpec`. Detalhes na seção 8. |
| `Covers` | Capas do libretro thumbnails. Baixa a lista de nomes por sistema (cache de 14 dias), casa por nome exato, depois por título e região. Guarda em `filesDir/covers/<sistema>/<sha1>.png`. `rememberCover()` usa um LRU em memória. |
| `Prefs` | SharedPreferences `huginn`, espelhadas em estado Compose. Chaves: `romRoots`, `systemEmulator`, `gameEmulator`, `lastPlayed`, `favorites`, `markedGames`, `unmarkedGames`, `autoDetected`, `askedFolders`, `playMinutes`, `downloadCovers`. |

### 7.3 App: UI (`ui/`)

| Arquivo | O que faz |
| --- | --- |
| `TopScreen` | Cabeçalho; fileira de filtros (Todos, Recentes, Favoritos, cada console, Apps); painel de informações à esquerda; carrossel de capas à direita; `GamePage` sobreposta. Controle: L1/R1 troca o filtro, A joga, X joga na outra tela, Y abre os detalhes, B volta. |
| `GamePage` | Capa grande, tags, arquivo, tamanho, última vez jogado, tempo jogado e seletor de emulador por jogo (esquerda/direita). Botões Jogar, Jogar na outra tela, Favoritar e Voltar. |
| `Games.kt` | Objeto `Home` (filtro compartilhado entre as telas), `rememberGames`, `filtersFor`, `gamesFor`, `playGame` (trata `NeedsFolder` abrindo o seletor) e `GameCover` (capa gerada ou imagem). |
| `BottomScreen` | Barra de status e as abas Consoles, Apps, Ajustes e Telas. A aba Telas mostra o diagnóstico e o relatório da última tentativa de abrir jogo, com botão de copiar. |
| `BottomTabs` | `ConsolesTab` e `SettingsTab`: acesso a arquivos; pastas com status "liberada" e os botões Liberar e Remover; emuladores encontrados; emulador por console; capas; atalhos do launcher; versão e créditos. |
| `Setup.kt` | Cartão de primeiro uso (permitir acesso, escolher pasta, procurar, escanear de novo). Também `rememberFolderPicker` e `requestStorageAccess`. |
| `Components.kt` | `PillButton`, `SmallChip`, `Segmented`, `ActionTile`, `focusRing`, `rememberClock`, `findActivity`. |
| `theme/Theme.kt` | `HColors`, fontes e `HuginnTheme`. |

### 7.4 Outros arquivos

- **Strings:** `res/values/strings.xml` (inglês) e `res/values-pt/strings.xml` (português). Toda string nova vai nos dois.
- **FileProvider:** `${applicationId}.files`, configurado em `res/xml/file_paths.xml`.
- **Permissões:** MANAGE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE (até a API 32), QUERY_ALL_PACKAGES, INTERNET, KILL_BACKGROUND_PROCESSES, ACCESS_NETWORK_STATE.
- **Regras do ES-DE:** `tools/import_esde.py` gera o `rules.json` a partir de `resources/systems/android` do ES-DE. O espelho no GitHub é `RetroDECK/ES-DE`. A licença fica em `assets/esde/LICENSE-ES-DE.txt`, e os créditos aparecem nos Ajustes.

---

## 8. Como um jogo é aberto (o ponto mais sensível)

1. `GameLauncher.choice` monta a lista de comandos do sistema que estão instalados. Os comandos "Native port" (`%ANDROIDAPP%`) só valem para arquivos `.app`. A escolha segue esta ordem: emulador escolhido para o jogo, depois o escolhido para o console, depois o automático (`Systems.defaultCommand`).
2. `buildIntent` monta a Intent a partir do `LaunchSpec` e resolve estas variáveis:
   - `%ROM%` e `%ROMRAW%`: caminho absoluto do arquivo.
   - `%ROMSAF%`: URI de documento **dentro da árvore liberada** (`/tree/<raiz>/document/<arquivo>`). Sem árvore liberada, volta para `content://com.android.externalstorage.documents/document/<id>` e marca `needsFolder`.
   - `%ROMPROVIDER%`: URI do FileProvider, com permissão de leitura.
   - `%BASENAME%`, `%GAMEDIR%`, `%ROMPATH%`: nome sem extensão, pasta do jogo, pasta raiz das ROMs.
   - `%EXTERNALDATA%` = `/storage/emulated/<user>` e `%INTERNALDATA%` = `/data/user/<user>`.
   - `%ANDROIDPACKAGE%`: pacote do emulador.
   - `%FILEINJECT%` e `%INJECT%=arquivo`: conteúdo de um arquivo de até 4 KB.
   - Dados que começam com `/` viram `file://`, com o StrictMode relaxado, para emuladores antigos.
3. Se `needsFolder` estiver marcado e a pasta ainda não foi pedida, `play` devolve `Result.NeedsFolder`. Aí `playGame` mostra um aviso, abre o seletor do Android já na pasta e depois tenta de novo. O Huginn só pede uma vez por pasta (`Prefs.askedFolders`).
4. **RetroArch:** ganha `CLEAR_TASK` e `CLEAR_TOP`, e o processo em segundo plano é encerrado com `killBackgroundProcesses`, como o Cocoon faz.
5. **Permissões repassadas:** `attempts()` tenta abrir em ordem:
   1. URIs do jogo e da árvore em um ClipData, com leitura, escrita e prefixo;
   2. só o jogo, com leitura e escrita;
   3. só leitura;
   4. sem nenhuma permissão.

   O ClipData é necessário porque vários emuladores recebem a URI num extra e não no `data`.
6. Abre na tela escolhida (`DualScreen.startOrError`), grava `lastPlayed` e a sessão de tempo de jogo, e guarda o relatório em `GameLauncher.lastLaunch` (também no logcat, tag `Huginn`).

**Histórico do problema:**
- Na build 3, os jogos não abriam direito nos emuladores. O Christian não mandou relatório, só disse que não funcionava.
- A IA anterior estudou o Cocoon (código aberto em `inssekt/CocoonFE`, que usa a base de plataformas do Daijisho).
- Diferenças encontradas: o Cocoon usa a URI dentro da árvore SAF e reinicia o RetroArch.
- A 1.1 implementou isso, mas **ainda não foi testada no aparelho**. Se continuar falhando, peça o relatório da aba Telas para cada combinação de console e emulador e compare com os `amStartArguments` do Cocoon (`platforms/*.json` no repositório dele).

**Pontos de atenção que a IA anterior não conseguiu confirmar:**
- Se o Android aceita repassar a permissão com prefixo (`FLAG_GRANT_PREFIX_URI_PERMISSION`). Existe uma tentativa sem prefixo como plano B.
- Se emuladores como melonDS, Dolphin, Azahar, DraStic e DuckStation aceitam a URI no formato `tree/…/document/…`. O Cocoon e o Daijisho usam esse formato, então deve funcionar.
- No RetroArch, o núcleo (core) precisa estar baixado lá dentro. O caminho do core vem do comando do ES-DE (`/data/user/0/<pacote>/cores/<core>_libretro_android.so`).
- Capas: o formato da listagem de `thumbnails.libretro.com` foi presumido (links `href` para `.png`), porque não deu para testar desse ambiente. Se não vier nenhuma capa, comece verificando isso. A alternativa é o raw do GitHub: `libretro-thumbnails/<Sistema_com_underscores>`.

---

## 9. O que fazer agora

1. **Pedir ou processar o checklist `docs/testes/fase-1.1.md`.** Ele cobre: painel de baixo fechando junto com o Huginn; emuladores encontrados; liberar a pasta; uma tabela de console × emulador × abriu ou não; capas; tempo de jogo.
2. Corrigir o que ele apontar, publicar uma build nova e entregar um checklist curto do que mudou.
3. Pegar com ele os caminhos das 3 pastas de ROMs e o emulador que ele usa em cada console. Se alguma pasta de console não for reconhecida, adicione o apelido em `Systems.aliases`, no `shared`.
4. Fechar a fase 1 e começar a **fase 2**:
   - Cliente do RomM (API REST do RomM; precisa do endereço e do usuário dele).
   - Capas por várias fontes, por exemplo ScreenScraper e SteamGridDB, como o Cocoon.
   - Manuais em PDF.
   - Caixa 3D da mídia física.
   - Capas para arcade: os nomes curtos dos arquivos (`sf2.zip`) precisam de um mapeamento para o nome completo.
5. A cada fase:
   - Atualizar o README (tabela de fases) e a página do projeto.
   - Atualizar o documento de decisões do projeto, se você tiver acesso. Na conta dele no claude.ai é `claude/decisoes-launcher.md`, no Projeto "Ayn Thor - Launcher".

---

## 10. Referências

- **ES-DE**, regras de Android (MIT): `RetroDECK/ES-DE` no GitHub, pasta `resources/systems/android`. A documentação das variáveis está no `INSTALL.md`, seção Android.
- **Cocoon:** `github.com/inssekt/CocoonFE`. As plataformas e os comandos estão em `platforms/*.json`, no formato do Daijisho. Wiki: `cocoon-shell.com/wiki`.
- **melonDS Android:** `rafaelvcaetano/melonDS-android`. A ação `me.magnum.melonds.LAUNCH_ROM` com o extra `uri` casa o jogo pelo caminho.
- **libretro thumbnails:** `thumbnails.libretro.com/<Sistema>/Named_Boxarts/<Nome>.png`.
- **Fwip** (código de onde vieram o tema e os componentes): repositório do Christian, `ChristianDonatti/fwip`.
