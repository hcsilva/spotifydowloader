// ── estado ────────────────────────────────────────────────
let playlistName = '';

// ── helpers ───────────────────────────────────────────────
function showAlert(id, msg) {
    const el = document.getElementById(id);
    if (!el) return;
    if (msg !== undefined) el.textContent = msg;
    el.classList.remove('d-none');
    setTimeout(() => el.classList.add('d-none'), 6000);
}

function hideAlerts() {
    ['alertError', 'alertSuccess', 'alertDownloadError'].forEach(id =>
        document.getElementById(id)?.classList.add('d-none'));
}

function extrairId(url) {
    const m = url.match(/playlist\/([a-zA-Z0-9]+)/);
    return m ? m[1] : (/^[a-zA-Z0-9]+$/.test(url.trim()) ? url.trim() : null);
}

// ── buscar playlist ───────────────────────────────────────
async function buscarPlaylist() {
    const url = document.getElementById('playlistUrl').value.trim();
    const id = extrairId(url);
    if (!id) { showAlert('alertError', '❌ Link inválido. Use o formato: https://open.spotify.com/playlist/...'); return; }

    hideAlerts();
    document.getElementById('resultsCard').classList.add('d-none');
    document.getElementById('loadingBusca').classList.remove('d-none');
    document.getElementById('btnLimpar').style.display = 'none';

    try {
        const res = await fetch(`/api/playlist/${encodeURIComponent(id)}`);
        if (!res.ok) throw new Error(await res.text());
        const result = await res.json();
        playlistName = result.playlistName;
        preencherLista(result); // Passa o objeto "result" inteiro

        // Salva estado da busca
        localStorage.setItem('spotify_playlist_url', url);
        localStorage.setItem('spotify_playlist_name', playlistName);
        localStorage.setItem('spotify_playlist_data', JSON.stringify(result));

        document.getElementById('btnLimpar').style.display = '';
        document.getElementById('resultsCard').classList.remove('d-none');
    } catch (e) {
        document.getElementById('loadingBusca').classList.add('d-none'); // Corrected from buscarSpinner
        showAlert('alertError', '❌ ' + (e.message || 'Erro ao buscar playlist.'));
    } finally {
        document.getElementById('loadingBusca').classList.add('d-none');
    }
}

// ── preencher lista com checkboxes ────────────────────────
function preencherLista(data) {
    const container = document.getElementById('trackList');
    container.innerHTML = '';
    const tracks = data.musicList || [];

    tracks.forEach((t, i) => {
        const label = document.createElement('label');
        // Removido 'form-check' para não conflitar com d-flex
        label.className = 'd-flex align-items-center gap-2 rounded mb-1 w-100 border border-transparent flex-wrap flex-md-nowrap';
        label.style.cssText = 'cursor:pointer; padding:10px 12px; transition:all 0.1s; margin:0;';

        const refreshStyle = (hover) => {
            const checked = cb.checked;
            if (checked) {
                label.style.background = '#d4edda'; // Verde claro
                label.style.borderColor = '#c3e6cb';
            } else if (hover) {
                label.style.background = '#f8f9fa'; // Cinza claro (hover)
                label.style.borderColor = '#dee2e6';
            } else {
                label.style.background = 'transparent';
                label.style.borderColor = 'transparent';
            }
        };

        label.onmouseenter = () => refreshStyle(true);
        label.onmouseleave = () => refreshStyle(false);
        label.innerHTML = `
            <input class="form-check-input flex-shrink-0 track-cb m-0" type="checkbox"
                   style="width:1.25em; height:1.25em; cursor:pointer;"
                   data-artist="${escHtml(t.artistName)}" data-track="${escHtml(t.trackName)}"/>
            <span class="text-muted" style="min-width:28px;font-size:.9rem">${String(i + 1).padStart(2, '0')}.</span>
            <span style="font-size:1rem; word-break: break-word;"><strong>${escHtml(t.artistName)}</strong> — ${escHtml(t.trackName)}</span>
        `;
        const cb = label.querySelector('input');
        cb.addEventListener('change', () => { refreshStyle(false); atualizarSelecionadas(); });
        container.appendChild(label);
    });

    document.getElementById('playlistTitle').textContent = data.playlistName || 'Músicas';
    document.getElementById('trackCountBadge').textContent = tracks.length;
    atualizarSelecionadas();
}

function escHtml(str) {
    return (str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ── seleção ───────────────────────────────────────────────
function selecionarTodas() {
    document.querySelectorAll('.track-cb').forEach(cb => cb.checked = true);
    atualizarBackgrounds();
    atualizarSelecionadas();
}
function deselecionarTodas() {
    document.querySelectorAll('.track-cb').forEach(cb => cb.checked = false);
    atualizarBackgrounds();
    atualizarSelecionadas();
}

function atualizarBackgrounds() {
    document.querySelectorAll('#trackList label').forEach(lbl => {
        const checked = lbl.querySelector('input').checked;
        lbl.style.background = checked ? '#d4edda' : 'transparent';
        lbl.style.borderColor = checked ? '#c3e6cb' : 'transparent';
    });
}

function atualizarSelecionadas() {
    const n = document.querySelectorAll('.track-cb:checked').length;
    document.getElementById('selectedInfo').textContent = n + ' selecionada(s)';
    document.getElementById('btnDownload').disabled = n === 0;
}

// ── modal pasta ───────────────────────────────────────────
function abrirModalPasta() {
    const pastaSugestao = 'C:\\musicas\\' + (playlistName.replace(/[\\/:*?"<>|]/g, '_') || 'Playlist');
    document.getElementById('pastaDestino').value = pastaSugestao;
    new bootstrap.Modal(document.getElementById('modalPasta')).show();
}

async function escolherPasta() {
    try {
        if ('showDirectoryPicker' in window) {
            const dirHandle = await window.showDirectoryPicker({
                mode: 'readwrite'
            });
            // O File System API do navegador por segurança não expõe o caminho absoluto direto.
            // Para backend local (Spring Boot no mesmo PC), sugerimos pedir ao usuário que 
            // digite ou cole o caminho absoluto se a API não funcionar corretamente para o Spring.
            // Mas para fins de UI, vamos exibir o nome da pasta.
            document.getElementById('pastaDestino').value = dirHandle.name;

            // Mas ATENÇÃO: o Java precisa do path completo. 
            // Como navegador web não dá o path completo (ex: C:\Users\...), 
            // vamos transformar o input em editável de novo para caso ele queira colar.
            document.getElementById('pastaDestino').removeAttribute('readonly');
        } else {
            alert('Seu navegador não suporta seleção de pasta nativa. Por favor, digite o caminho.');
            document.getElementById('pastaDestino').removeAttribute('readonly');
        }
    } catch (e) {
        console.error(e);
        document.getElementById('pastaDestino').removeAttribute('readonly');
    }
}

// ── download ──────────────────────────────────────────────
async function iniciarDownload() {
    const pasta = document.getElementById('pastaDestino').value.trim();
    if (!pasta) { alert('Informe a pasta de destino.'); return; }

    const selecionadas = [...document.querySelectorAll('.track-cb:checked')]
        .map(cb => ({ artistName: cb.dataset.artist, trackName: cb.dataset.track }));
    if (!selecionadas.length) return;

    // Fecha o modal
    bootstrap.Modal.getInstance(document.getElementById('modalPasta'))?.hide();

    hideAlerts();
    document.getElementById('downloadProgress').classList.remove('d-none');
    document.getElementById('btnDownload').disabled = true;

    try {
        const res = await fetch('/api/download', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ tracks: selecionadas, destinationFolder: pasta })
        });
        const result = await res.json();
        if (!res.ok) throw new Error(result.error || 'Erro ao iniciar download');
        
        localStorage.setItem('spotify_download_active', 'true');
        checkDownloadStatus();
    } catch (e) {
        document.getElementById('downloadProgress').classList.add('d-none');
        atualizarSelecionadas();
        showAlert('alertDownloadError', '❌ ' + (e.message || 'Erro no download.'));
    }
}

// ── polling ───────────────────────────────────────────────
let pollingInterval = null;

async function checkDownloadStatus() {
    try {
        const res = await fetch('/api/download/status');
        if (!res.ok) return;
        const status = await res.json();
        
        const progressAlert = document.getElementById('downloadProgress');
        const btnDownload = document.getElementById('btnDownload');
        
        if (status.isDownloading) {
            localStorage.setItem('spotify_download_active', 'true');
            if (progressAlert) {
                progressAlert.classList.remove('d-none');
                let msg = `Baixando músicas: ${status.downloaded + status.failed} de ${status.total} (Erros: ${status.failed}). Aguarde...`;
                progressAlert.innerHTML = `<div class="spinner-border spinner-border-sm me-2" role="status"></div>${msg}`;
            }
            if (btnDownload) btnDownload.disabled = true;
            
            if (!pollingInterval) {
                pollingInterval = setInterval(checkDownloadStatus, 2000);
            }
        } else {
            // Não está baixando
            if (pollingInterval) {
                clearInterval(pollingInterval);
                pollingInterval = null;
            }
            if (progressAlert) progressAlert.classList.add('d-none');
            if (btnDownload) atualizarSelecionadas();
            
            // Verifica se um download acabou de terminar
            if (localStorage.getItem('spotify_download_active') === 'true') {
                localStorage.removeItem('spotify_download_active');
                if (status.total > 0) {
                    if (status.failed > 0) {
                        showAlert('alertSuccess', `⚠️ ${status.downloaded} baixada(s), mas ${status.failed} falharam na pasta: ${status.destinationFolder}`);
                    } else {
                        showAlert('alertSuccess', `✅ ${status.downloaded} música(s) baixada(s) com sucesso em: ${status.destinationFolder}`);
                    }
                }
            }
        }
    } catch (e) {
        console.error("Erro ao verificar status do download", e);
    }
}

// ── limpar ────────────────────────────────────────────────
function limpar() {
    localStorage.removeItem('spotify_playlist_url');
    localStorage.removeItem('spotify_playlist_name');
    localStorage.removeItem('spotify_playlist_data');

    document.getElementById('playlistUrl').value = '';
    document.getElementById('trackList').innerHTML = '';
    document.getElementById('resultsCard').classList.add('d-none');
    document.getElementById('btnLimpar').style.display = 'none';
    playlistName = '';
    hideAlerts();
    atualizarSelecionadas();
}

// ── configurações ─────────────────────────────────────────
function salvarCredenciais() {
    const id = document.getElementById('clientId')?.value;
    const sec = document.getElementById('clientSecret')?.value;
    if (!id || !sec) { showAlert('settingsAlertError', '❌ Preencha os dois campos.'); return; }

    fetch('/api/credenciais', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientId: id, clientSecret: sec })
    })
        .then(r => {
            if (!r.ok) throw new Error();
            // Salvar no localstorage
            localStorage.setItem('spotify_client_id', id);
            localStorage.setItem('spotify_client_secret', sec);
            showAlert('settingsAlertSuccess');
        })
        .catch(() => showAlert('settingsAlertError', '❌ Erro ao salvar credenciais.'));
}

function limparCredenciais() {
    localStorage.removeItem('spotify_client_id');
    localStorage.removeItem('spotify_client_secret');
    if (document.getElementById('clientId')) {
        document.getElementById('clientId').value = '';
        document.getElementById('clientSecret').value = '';
    }
}

function toggleVisibilidade(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    if (input.type === 'password') {
        input.type = 'text';
        btn.innerHTML = '<i class="bi bi-eye-slash"></i>';
    } else {
        input.type = 'password';
        btn.innerHTML = '<i class="bi bi-eye"></i>';
    }
}

// ── init (restore state on page load) ─────────────────────
document.addEventListener('DOMContentLoaded', () => {
    // Retomar polling se necessário
    checkDownloadStatus();
    // Restaurar Configurações
    const elClientId = document.getElementById('clientId');
    const elClientSecret = document.getElementById('clientSecret');
    if (elClientId && elClientSecret) {
        const savedId = localStorage.getItem('spotify_client_id');
        const savedSec = localStorage.getItem('spotify_client_secret');
        if (savedId) elClientId.value = savedId;
        if (savedSec) elClientSecret.value = savedSec;
    }

    // Restaurar Playlist
    const elPlaylistUrl = document.getElementById('playlistUrl');
    if (elPlaylistUrl) {
        const savedUrl = localStorage.getItem('spotify_playlist_url');
        const savedName = localStorage.getItem('spotify_playlist_name');
        const savedData = localStorage.getItem('spotify_playlist_data');
        if (savedUrl && savedData) {
            elPlaylistUrl.value = savedUrl;
            playlistName = savedName || '';
            try {
                const result = JSON.parse(savedData);
                preencherLista(result);
                document.getElementById('btnLimpar').style.display = '';
                document.getElementById('resultsCard').classList.remove('d-none');
            } catch (e) {
                console.error('Erro ao restaurar playlist salva', e);
            }
        }
    }
});
