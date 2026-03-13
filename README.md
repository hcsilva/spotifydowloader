# 🎵 Spotify Downloader

Um aplicativo web simples e intuitivo para baixar suas playlists musicais favoritas! Com ele, você fornece o link de uma playlist e o sistema baixa automaticamente as músicas em formato MP3 (usando o áudio com a melhor qualidade disponível) direto para o seu computador.

---

## ✨ Como Funciona?

O **Spotify Downloader** atua como uma ponte entre você, suas playlists e suas músicas baixadas:
1. Ele lê a sua playlist usando as ferramentas oficiais do serviço de streaming musical.
2. Com a lista de nomes das músicas e artistas em mãos, ele realiza uma busca inteligente e baixa o áudio automaticamente usando uma ferramenta super rápida chamada `yt-dlp`.
3. Tudo é salvo em formato MP3, prontinho para você ouvir offline, passar para o celular ou pen drive!

---

## 🚀 Como Usar (Passo a Passo)

### 1. Requisitos Básicos
Antes de rodar o programa, certifique-se de ter instalado no seu computador:
- **Java 25** (ou superior)
- Um navegador web moderno (Google Chrome, Firefox, Edge, etc.)

*(Nota: O programa baixa e instala a ferramenta de download `yt-dlp` automaticamente no diretório `C:\tools\yt-dlp.exe` caso você já não a tenha.)*

### 2. Rodando o Aplicativo
1. Faça o download ou clone este projeto em seu computador.
2. Abra a pasta do projeto e inicie a aplicação Spring Boot (usando sua IDE favorita como IntelliJ, VS Code, ou através do comando Maven `./mvnw spring-boot:run`).
3. Abra o seu navegador e acesse: **`http://localhost:8080`**

### 3. Configurando o Sistema (Primeiro Uso)
Para que o sistema consiga ler as suas playlists, ele precisa de uma "chave de acesso".
1. Acesse a aba **Configurações** (⚙️) no menu.
2. Você precisará de duas chaves: **Client ID** e **Client Secret**.
3. Se você não tem essas chaves:
   - Vá no painel de desenvolvedores do serviço de música.
   - Crie um "Pequeno App" novo (App / Application).
   - Copie os valores `Client ID` e `Client Secret` fornecidos lá.
4. Cole as chaves na página de Configurações e clique em **Salvar Credenciais**. *(Esas chaves ficam salvas no seu navegador, você não precisará digitá-las toda vez!)*

### 4. Baixando suas Músicas!
1. Vá para a aba **Home**.
2. Copie o link completo da playlist que você quer baixar (ex: `https://open.spotify.com/playlist/...`).
3. Cole no campo de busca e clique em **🔍 Buscar**.
4. Uma lista com todas as músicas vai aparecer! Você pode escolher baixar todas ou apenas algumas clicando nas caixinhas.
5. Clique em **⬇️ Fazer Download**.
6. Uma janelinha vai abrir pedindo para você escolher a **Pasta de Destino** (onde as músicas serão salvas no seu PC).
7. Clique em **Baixar** e pronto! O sistema vai baixar as músicas para a pasta escolhida em formato MP3.