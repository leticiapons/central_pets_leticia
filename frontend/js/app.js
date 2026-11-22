import * as api from './api.js';
import { atualizarUsuarioLogado, getPerfil, getToken, getUsuarioLogado, limparSessao, PERFIS } from './state.js';
import { esc, toast } from './components.js';
import { renderLogin } from './views/login.js';
import { renderAdm } from './views/adm.js';
import { renderUsuario } from './views/usuario.js';
import { renderPadrinho } from './views/padrinho.js';

const app = document.getElementById('app');
const perfilBadge = document.getElementById('perfil-badge');
const botaoSair = document.getElementById('sair');
const nomeUsuario = document.getElementById('usuario-nome');

const ROTAS = {
  '': { perfil: null, render: renderLogin },
  adm: { perfil: 'ADM', render: renderAdm },
  usuario: { perfil: 'USUARIO', render: renderUsuario },
  padrinho: { perfil: 'PADRINHO', render: renderPadrinho }
};

function rotaAtual() {
  return location.hash.replace(/^#\/?/, '').split('?')[0].trim();
}

function rotaDoPerfil(perfil) {
  return perfil ? perfil.toLowerCase() : '';
}

function atualizarHeader() {
  const usuario = getUsuarioLogado();
  const perfilInfo = usuario ? PERFIS[usuario.perfil] : null;
  const exibir = Boolean(perfilInfo);
  perfilBadge.hidden = !exibir;
  botaoSair.hidden = !exibir;
  nomeUsuario.hidden = !exibir;
  if (exibir) {
    perfilBadge.textContent = perfilInfo.label;
    nomeUsuario.textContent = usuario.ongNome || usuario.nome;
  }
}

async function render() {
  const rota = rotaAtual();
  const destino = ROTAS[rota];

  if (!destino) {
    location.hash = '#/';
    return;
  }

  const perfil = getPerfil();
  if (destino.perfil && perfil !== destino.perfil) {
    location.hash = perfil ? `#/${rotaDoPerfil(perfil)}` : '#/';
    return;
  }
  if (!destino.perfil && perfil) {
    location.hash = `#/${rotaDoPerfil(perfil)}`;
    return;
  }

  atualizarHeader();
  app.innerHTML = '<div class="loading">Carregando…</div>';

  try {
    await destino.render(app);
  } catch (erro) {
    if (!getPerfil()) {
      // A sessão foi encerrada (por exemplo, expirou): a tela de login será exibida.
      toast('Sua sessão expirou. Entre novamente.', 'erro');
      return;
    }

    app.innerHTML = `
      <div class="empty-state">
        <h2>Não foi possível carregar</h2>
        <p class="muted">${esc(erro.message)}</p>
        <p class="muted small">Dica: sirva a pasta <code>frontend/</code> via HTTP (ex.: <code>npx serve frontend -l 3000</code>) e mantenha o back-end em <code>http://localhost:8080</code>.</p>
      </div>`;
  }
}

botaoSair.addEventListener('click', async (evento) => {
  evento.preventDefault();
  await api.sair();
  limparSessao();
  location.hash = '#/';
});

window.addEventListener('hashchange', render);

// Ao abrir ou atualizar a página, confere se a sessão guardada no navegador ainda vale.
async function verificarSessao() {
  if (!getToken()) return;

  try {
    const usuario = await api.usuarioAtual();
    if (usuario) atualizarUsuarioLogado(usuario);
  } catch {
    if (!getToken()) {
      toast('Sua sessão expirou. Entre novamente.', 'erro');
    }
  }
}

window.addEventListener('DOMContentLoaded', async () => {
  await verificarSessao();

  if (!location.hash) {
    location.hash = '#/';
    return;
  }

  render();
});
