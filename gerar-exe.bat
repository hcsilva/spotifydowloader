@echo off
echo ========================================
echo  Gerando instalador SpotifyDownloader
echo ========================================

:: Entra na pasta target onde estao o .jar e a pasta ffmpeg
cd /d "%~dp0target"

:: Verifica se o .jar existe
if not exist "spotifydownloader-1.0.0.jar" (
    echo ERRO: spotifydownloader-1.0.0.jar nao encontrado na pasta target!
    echo Execute primeiro: mvnw clean package
    pause
    exit /b 1
)

:: Verifica se o ffmpeg existe
if not exist "ffmpeg\ffmpeg.exe" (
    echo ERRO: ffmpeg\ffmpeg.exe nao encontrado na pasta target!
    echo Verifique se a pasta target\ffmpeg existe e contem ffmpeg.exe
    pause
    exit /b 1
)

echo.
echo Gerando .exe com jpackage...
echo.

jpackage ^
  --name SpotifyDownloader ^
  --app-version 1.0.0 ^
  --vendor "Henrique Silva" ^
  --description "Spotify Playlist Downloader" ^
  --icon icon.ico ^
  --input . ^
  --main-jar spotifydownloader-1.0.0.jar ^
  --app-content ffmpeg ^
  --type exe ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --dest ..\

if %ERRORLEVEL% == 0 (
    echo.
    echo ========================================
    echo  Instalador gerado com sucesso!
    echo  Arquivo: SpotifyDownloader-1.0.0.exe
    echo ========================================
) else (
    echo.
    echo ERRO ao gerar o instalador. Verifique os logs acima.
)

pause
