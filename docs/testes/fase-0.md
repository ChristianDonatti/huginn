# Testes da fase 0

Marque cada item com `[x]` (deu certo) ou `[ ]` com um comentário (deu errado ou estranho). Quando algo falhar, um print ou vídeo curto ajuda muito; se o app fechar sozinho, o log do `adb logcat` mais ainda.

Versão testada (aparece no painel de baixo, aba Atalhos): `Huginn 0.0.__ · build __`

## 1. Instalação e ícone
- [ ] O APK da versão de teste instala no Thor sem erro.
- [ ] O ícone do Huginn aparece na lista de apps: corvo claro com olho dourado, fundo azul-noite.
- [ ] No tema de ícones temáticos do Android (se usar), o corvo aparece monocromático.

## 2. Abrindo sem ser o launcher padrão
Abra o Huginn pela lista de apps, ainda com o launcher atual como padrão.
- [ ] A tela de cima mostra o cabeçalho "huginn", o selo "fase 0 · teste", o relógio e o botão "Painel embaixo".
- [ ] Os apps aparecem num carrossel, em ordem alfabética, com o nome e o pacote do app selecionado à esquerda.
- [ ] O brilho atrás muda de cor conforme o app selecionado.
- [ ] O painel do Huginn abre sozinho na tela de baixo. Se não abrir, toque em "Painel embaixo" e diga se aí abriu.

## 3. Controle na tela de cima
- [ ] Direcional esquerda e direita anda pelo carrossel, e o card selecionado cresce com borda clara.
- [ ] O botão A abre o app selecionado na tela de cima.
- [ ] O botão X abre o app selecionado na tela de baixo.
- [ ] O botão B não fecha o Huginn nem faz nada estranho.

## 4. Toque na tela de cima
- [ ] Tocar num card não selecionado seleciona; tocar de novo abre.
- [ ] "Abrir" abre na tela de cima e "Abrir na outra tela" abre na de baixo.

## 5. Painel de baixo
- [ ] A barra do topo mostra hora, Wi-Fi e bateria certos.
- [ ] Aba Atalhos: "Ajustes do Android" abre os ajustes.
- [ ] Aba Atalhos: "Launcher padrão" abre a escolha do app de tela inicial.
- [ ] Aba Atalhos: "Tela de cima" traz o Huginn de volta lá em cima.
- [ ] Aba Apps: tocar num app com "Esta tela" abre na tela de baixo.
- [ ] Aba Apps: com "Outra tela", abre na tela de cima.
- [ ] O controle funciona no painel quando o foco do controle está na tela de baixo (direcional anda, A toca).

## 6. Aba Telas (mande um print desta aba)
- [ ] Aparecem duas telas. Anote o número (#) e a resolução de cada uma.
- [ ] A tela de baixo está marcada como "esta tela".
- [ ] Aparece alguma linha "Último erro"? Se sim, copie o texto.

## 7. Como launcher padrão
Ajustes do Android > Apps > Apps padrão > App de tela inicial > Huginn (ou pelo atalho "Launcher padrão").
- [ ] Apertar Home na tela de cima volta para o Huginn.
- [ ] Depois de reiniciar o Thor, o Huginn abre em cima.
- [ ] Depois de reiniciar, o painel do Huginn aparece embaixo sozinho (sem tocar em nada).
- [ ] Com um app aberto embaixo (YouTube, por exemplo), apertar Home em cima não fecha nem cobre o app de baixo.
- [ ] Mjolnir, Wayfinder ou o botão AYN continuam funcionando. Diga quais você usa.
- [ ] Dá para voltar ao launcher anterior pelo mesmo caminho dos ajustes.

## 8. Instalar e remover apps
- [ ] Instalar um app novo e voltar ao Huginn: ele aparece na lista (se não, teste "Recarregar apps").
- [ ] Desinstalar um app: ele some da lista.

## 9. Impressões
- Algo travou, piscou ou ficou lento?
- O tamanho dos cards e dos textos ficou bom na tela de cima e na de baixo?
- O que você mudaria já?
