# Huginn

Launcher de duas telas para o AYN Thor, com a cara do [Fwip](https://github.com/ChristianDonatti/fwip). O Huginn fica no Thor; o Muninn, que vem depois, leva jogos, saves e manuais entre o Thor, o celular, o PC e o NAS.

> Em desenvolvimento, fase 1. Já acha os jogos nas suas pastas de ROMs, abre cada um no emulador certo e tem página do jogo. Capas de verdade chegam na fase 2.

*A dual-screen launcher for the AYN Thor, in early development.*

## Instalar a versão de teste

1. No Thor, abra a [versão de teste](https://github.com/ChristianDonatti/huginn/releases/tag/teste) e baixe `huginn.apk`.
2. Abra o arquivo e permita instalar dessa origem quando o Android pedir.
3. Para usar como launcher: Ajustes do Android > Apps > Apps padrão > App de tela inicial > Huginn.
   Para voltar ao launcher anterior, faça o mesmo caminho, ou use o atalho "Launcher padrão" no painel de baixo.

Cada push no `main` gera um APK novo, que instala por cima do anterior.

## Fases

| Fase | O que entra |
| --- | --- |
| 0 · Fundação | Projeto, tema do Fwip, ícone, launcher abrindo nas duas telas |
| 1 · MVP | Pastas de ROMs, biblioteca, emuladores, home simples, página do jogo |
| 2 · NAS e capas | RomM, capas, manuais em PDF, caixa 3D |
| 3 · Muninn | Transferências entre Thor, celular, PC e NAS; modo celular; página no navegador |
| 4 · Home personalizada | Blocos, edição, boas-vindas com as duas homes |
| 5 · Painel, tempo e bateria | Painel em jogo, "Quanto tempo você tem?", bateria em horas de jogo |
| 6 · Sync e extras | Sync de estatísticas e saves, widget, modo sofá |
| 7 · Ponte de companions | Protocolo para mods de PC usarem a tela de baixo |

Os testes de cada fase ficam em [`docs/testes`](docs/testes).

## Compilar

Precisa do JDK 17 e do Android SDK.

```
./gradlew :shared:testDebugUnitTest assembleRelease
```

O APK sai em `app/build/outputs/apk/release/`. As versões de teste são assinadas com a chave de desenvolvimento em `keystore/`, pública de propósito para que uma versão instale por cima da outra.

## Pastas de ROMs e emuladores

O Huginn usa os mesmos nomes de pasta do ES-DE (`gba`, `snes`, `psx`, `n3ds`...), com alguns apelidos comuns (`3ds`, `gamecube`, `ps1`). Os comandos que abrem cada emulador vêm das regras do [ES-DE](https://es-de.org) para Android, sob licença MIT (`app/src/main/assets/esde`). Para atualizar:

```
python3 tools/import_esde.py <pasta do ES-DE>/resources/systems/android
```

## Tecnologia

Kotlin, Jetpack Compose e Kotlin Multiplatform (módulo `shared`, que depois também atende o Muninn).

## Licença

GPL-3.0 ou posterior, como o Fwip. Fontes Unbounded e Manrope sob a SIL Open Font License 1.1 (veja `FONTS-OFL.txt`).
