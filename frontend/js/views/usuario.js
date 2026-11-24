import * as api from '../api.js';
import { esc, animalCard, fmtData, fmtMoeda, statusDoacaoBadge, toast } from '../components.js';
import { abrirDoacao } from '../doacao-modal.js';
import { ligarDetalhes, registrarAnimais } from '../detalhes-animal.js';
import { aplicarFiltros, filtrosIniciais, htmlFiltros, ligarFiltros } from '../filtros-animais.js';
import { mesmoId } from '../ids.js';
import { getUsuarioLogado } from '../state.js';

// Os filtros e a aba escolhidos ficam guardados enquanto a pessoa navega pelo site.
const filtros = filtrosIniciais();
let abaAtiva = 'animais';

// Para cada animal, diz se a pessoa já ajudou ('confirmada') ou se há só doação aguardando ('pendente').
export function mapaDeAjuda(doacoes) {
  const ajuda = new Map();
  doacoes.forEach((doacao) => {
    const id = String(doacao.animalId);
    if (doacao.status === 'DOADO') {
      ajuda.set(id, 'confirmada');
    } else if (doacao.status === 'PENDENTE' && ajuda.get(id) !== 'confirmada') {
      ajuda.set(id, 'pendente');
    }
  });
  return ajuda;
}

function seloDeAjuda(situacao) {
  if (situacao === 'confirmada') return '<span class="badge badge-ok">Você já ajudou</span>';
  if (situacao === 'pendente') return '<span class="badge badge-warn">Sua doação aguarda confirmação</span>';
  return '';
}

export async function renderUsuario(app) {
  const [ongs, todos, doacoes] = await Promise.all([
    api.listarOngs(),
    api.listarAnimais(),
    api.listarDoacoes()
  ]);
  const animais = todos.filter((animal) => animal.status === 'ATIVO');
  const mapaOngs = new Map(ongs.map((ong) => [String(ong.id), ong]));
  const ajuda = mapaDeAjuda(doacoes);
  const pendentes = doacoes.filter((doacao) => doacao.status === 'PENDENTE').length;

  registrarAnimais(todos, ongs);

  app.innerHTML = `
    <div class="page-head">
      <div>
        <h1>Doar</h1>
        <p class="muted">Escolha um animal e faça sua doação via PIX.</p>
      </div>
    </div>

    <nav class="tabs">
      <button class="tab" type="button" data-aba="animais">Animais para ajudar</button>
      <button class="tab" type="button" data-aba="doacoes">Minhas doações${pendentes ? ` (${pendentes})` : ''}</button>
    </nav>

    <section id="aba-doador"></section>

    <p class="hint">Depois de pagar o PIX, a doação fica aguardando a confirmação de quem a recebeu. Ela só entra na meta do animal depois dessa confirmação.</p>`;

  const conteudo = app.querySelector('#aba-doador');
  const abas = [...app.querySelectorAll('.tab')];

  const ativar = (aba) => {
    abaAtiva = aba;
    abas.forEach((botao) => botao.classList.toggle('active', botao.dataset.aba === aba));
    if (aba === 'doacoes') {
      renderMinhasDoacoes(conteudo, doacoes, todos);
    } else {
      renderAnimaisParaAjudar(app, conteudo, { animais, mapaOngs, ajuda });
    }
  };

  abas.forEach((botao) => botao.addEventListener('click', () => ativar(botao.dataset.aba)));
  ativar(abaAtiva);
}

function renderAnimaisParaAjudar(app, conteudo, { animais, mapaOngs, ajuda }) {
  if (!animais.length) {
    conteudo.innerHTML = '<div class="empty-state"><p>Nenhum animal ativo para doação.</p></div>';
    return;
  }

  conteudo.innerHTML = `${htmlFiltros(animais, filtros)}<div id="lista-animais"></div>`;

  const lista = conteudo.querySelector('#lista-animais');
  const resumo = conteudo.querySelector('#resumo-filtros');

  const atualizarLista = () => {
    const visiveis = aplicarFiltros(animais, filtros);

    resumo.textContent = visiveis.length === animais.length
      ? `${animais.length} ${animais.length === 1 ? 'animal precisa' : 'animais precisam'} de ajuda.`
      : `Mostrando ${visiveis.length} de ${animais.length} animais.`;

    lista.innerHTML = visiveis.length
      ? `<div class="grid">${visiveis.map((animal) => animalCard(animal, {
        selo: seloDeAjuda(ajuda.get(String(animal.id))),
        acoes: `<button class="btn btn-primary btn-sm" type="button" data-doar="${esc(animal.id)}">Doar para ${esc(animal.nome)}</button>`
      })).join('')}</div>`
      : '<div class="empty-state"><p>Nenhum animal encontrado com esses filtros.</p></div>';

    ligarDetalhes(lista);

    lista.querySelectorAll('[data-doar]').forEach((botao) => {
      botao.addEventListener('click', () => {
        const animal = animais.find((item) => mesmoId(item.id, botao.dataset.doar));
        if (!animal) return;
        const ong = mapaOngs.get(String(animal.ongId));
        if (!ong) {
          toast('Não foi possível encontrar os dados de pagamento desta ONG.', 'erro');
          return;
        }
        abrirDoacao({
          animal,
          ong,
          perfil: 'USUARIO',
          doador: getUsuarioLogado(),
          aoConcluir: () => renderUsuario(app)
        });
      });
    });
  };

  ligarFiltros(conteudo, filtros, atualizarLista);
  atualizarLista();
}

function renderMinhasDoacoes(conteudo, doacoes, todos) {
  if (!doacoes.length) {
    conteudo.innerHTML = `
      <div class="empty-state">
        <h2>Você ainda não fez nenhuma doação</h2>
        <p class="muted">Quando você doar, o histórico aparece aqui.</p>
      </div>`;
    return;
  }

  const mapaAnimais = new Map(todos.map((animal) => [String(animal.id), animal]));
  const confirmadas = doacoes.filter((doacao) => doacao.status === 'DOADO');
  const pendentes = doacoes.filter((doacao) => doacao.status === 'PENDENTE');
  const totalConfirmado = confirmadas.reduce((soma, doacao) => soma + Number(doacao.valor || 0), 0);

  conteudo.innerHTML = `
    <div class="stats">
      <div class="stat"><div class="stat-label">Total doado (confirmado)</div><div class="stat-value">${fmtMoeda(totalConfirmado)}</div></div>
      <div class="stat"><div class="stat-label">Doações confirmadas</div><div class="stat-value">${confirmadas.length}</div></div>
      <div class="stat"><div class="stat-label">Aguardando confirmação</div><div class="stat-value">${pendentes.length}</div></div>
    </div>

    <div class="card table-wrap">
      <table class="table">
        <thead>
          <tr><th>Data</th><th>Animal</th><th>Campanha</th><th>Valor</th><th>Situação</th></tr>
        </thead>
        <tbody>
          ${doacoes.map((doacao) => {
    const animal = mapaAnimais.get(String(doacao.animalId));
    return `
            <tr>
              <td>${fmtData(doacao.data)}</td>
              <td>${esc(animal ? animal.nome : '—')}</td>
              <td>${esc(animal && animal.ongNome ? animal.ongNome : '—')}</td>
              <td class="valor">${fmtMoeda(doacao.valor)}</td>
              <td>${statusDoacaoBadge(doacao.status)}</td>
            </tr>`;
  }).join('')}
        </tbody>
      </table>
    </div>

    <p class="hint">Doações aguardando confirmação só entram na meta do animal depois que quem as recebeu confirmar o PIX.</p>`;
}
