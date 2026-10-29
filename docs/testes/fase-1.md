# Testes da fase 1

Marque cada item com `[x]` (deu certo) ou `[ ]` com um comentário (deu errado ou estranho). Print ou vídeo curto ajuda muito quando algo falha; se o app fechar sozinho, o log do `adb logcat` mais ainda.

Versão testada (painel de baixo, aba Ajustes, no fim): `Huginn 0.0.__ · build __`

Antes de começar, anote:
- Onde ficam suas pastas de ROMs (ex.: `/storage/emulated/0/ROMs`, cartão SD):
- Quais emuladores você usa em cada console (ex.: GBA no RetroArch, DS no melonDS):

## 1. Correção da fase 0
Feche o Huginn de baixo abrindo outro app na tela de baixo (YouTube, por exemplo).
- [ ] Abrir o Huginn pela lista de apps traz o painel de volta embaixo, toda vez (teste umas 5 vezes).
- [ ] Como launcher padrão, apertar Home em cima continua sem cobrir o app aberto embaixo.

## 2. Primeiro uso: acesso e pastas
- [ ] Sem permissão, a tela de cima mostra "O Huginn precisa ver suas pastas de jogos" com o botão "Permitir acesso".
- [ ] "Permitir acesso" abre a página do Android para o Huginn; ao ligar e voltar, o aviso some sozinho.
- [ ] O Huginn achou sua pasta de ROMs sozinho. Se não achou: "Procurar pastas" achou? Se também não, "Escolher pasta" funcionou?
- [ ] Na aba Ajustes, a pasta aparece com o número de jogos.
- [ ] Os nomes das suas pastas de console foram reconhecidos (gba, snes, psx, n3ds...). Diga quais NÃO apareceram e como a pasta se chama.

## 3. Biblioteca
- [ ] Os jogos aparecem no carrossel com capas coloridas geradas (cor do console, nome e inicial).
- [ ] Os títulos aparecem limpos, sem `(USA)`, `[!]` e extensão.
- [ ] Jogos de vários discos com `.m3u` aparecem uma vez só (não um por disco).
- [ ] Jogos `.cue` + `.bin` aparecem uma vez só (o `.bin` não aparece separado).
- [ ] Arquivos que não são jogos (imagens, `.txt`, pastas `media`, `bios`) não aparecem.
- [ ] Fechar e abrir o Huginn de novo: os jogos aparecem na hora, sem esperar outra busca.
- [ ] Quantos jogos e consoles a aba Ajustes mostra? Bate com o que você tem?

## 4. Home de jogos (tela de cima)
- [ ] A fileira de filtros mostra Todos, cada console com o número de jogos e Apps no fim.
- [ ] L1 e R1 trocam o filtro, e o carrossel mostra só aquele console.
- [ ] Tocar num filtro também troca.
- [ ] O brilho do fundo muda para a cor do console do jogo selecionado.
- [ ] À esquerda aparece o console, o título, o emulador que vai abrir, a região e o tamanho.
- [ ] O filtro Apps mostra os apps como na fase 0, e A/X abrem em cima/embaixo.

## 5. Jogar
Teste pelo menos um jogo de cada console que você tem. Anote o console, o emulador e o que aconteceu.
- [ ] A abre o jogo direto no emulador certo, na tela de cima.
- [ ] X abre o jogo na tela de baixo.
- [ ] Depois de jogar, aparece o filtro Recentes com esse jogo.
- [ ] Um console sem emulador instalado mostra "Nenhum emulador instalado" em dourado, e A avisa em vez de travar.

| Console | Emulador | Abriu o jogo? | Observação |
| --- | --- | --- | --- |
| | | | |
| | | | |
| | | | |

Se um emulador abrir mas não carregar o jogo (fica no menu dele), confira se ele tem acesso à pasta de ROMs nos ajustes dele. Se mesmo assim não carregar, anote o console e o emulador.

No RetroArch, o núcleo (core) precisa estar baixado dentro do RetroArch. Se aparecer erro de core, anote qual console.

## 6. Página do jogo
- [ ] Y (ou "Detalhes") abre a página com capa grande, título, tags como `USA` e `Rev 1`, nome do arquivo, tamanho e pasta.
- [ ] O botão Jogar já vem selecionado.
- [ ] No seletor de emulador, esquerda/direita troca entre "Automático" e os emuladores instalados. A troca vale só para esse jogo e fica salva.
- [ ] Y favorita; depois aparece o filtro Favoritos.
- [ ] B volta ao carrossel, no mesmo jogo.

## 7. Painel de baixo
- [ ] Aba Consoles: um bloco por console com a cor dele; tocar troca o filtro lá em cima.
- [ ] Aba Apps: tocar e segurar (ou Y) marca um app como jogo; aparece uma bolinha dourada e ele passa a aparecer no filtro Android.
- [ ] Jogos Android que você já tem (Genshin, Minecraft...) apareceram sozinhos no filtro Android? Algum app que não é jogo apareceu lá?
- [ ] Aba Ajustes: "Emulador por console" lista seus consoles; tocar troca o emulador do console inteiro.
- [ ] Aba Ajustes: remover uma pasta tira os jogos dela; adicionar de novo traz de volta.
- [ ] Os atalhos antigos (Ajustes do Android, Launcher padrão, Tela de cima) continuam funcionando.

## 8. Impressões
- Algo travou, piscou ou ficou lento (principalmente com muitos jogos)?
- O tamanho das capas e dos textos ficou bom?
- O que você mudaria já?
