import { esc } from './components.js';

// Busca, filtros e ordenação da lista de animais (tela do doador).

const FALTAS = {
  todas: 'Qualquer valor',
  200: 'Faltam até R$ 200',
  500: 'Faltam até R$ 500',
  1000: 'Faltam até R$ 1.000',
  mais: 'Faltam mais de R$ 1.000'
};

// A ordem de cadastro é a ordem em que os animais foram publicados no site (do número de cada um).
const SITUACOES = {
  todas: 'Todos',
  ATIVO: 'Ativos',
  INATIVO: 'Inativos'
};

const ORDENACOES = {
  recentes: 'Mais recentes primeiro',
  antigos: 'Mais antigos primeiro',
  nome: 'Nome (A a Z)',
  urgentes: 'Urgentes primeiro',
  proximos: 'Mais próximos da meta',
  precisam: 'Menos arrecadado',
  menorFalta: 'Menor valor que falta'
};

export function filtrosIniciais() {
  return { busca: '', especie: 'todas', ong: 'todas', situacao: 'todas', falta: 'todas', urgentes: false, ordem: '' };
}

function normalizar(texto) {
  return String(texto || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

function chaveEspecie(animal) {
  return normalizar(animal.especie);
}

function faltaDe(animal) {
  return Math.max(Number(animal.meta || 0) - Number(animal.arrecadado || 0), 0);
}

export function aplicarFiltros(animais, filtros) {
  const busca = normalizar(filtros.busca);

  const resultado = animais.filter((animal) => {
    if (busca) {
      const texto = normalizar(`${animal.nome} ${animal.descricao} ${animal.ongNome} ${animal.especie}`);
      if (!texto.includes(busca)) return false;
    }
    if (filtros.especie !== 'todas' && chaveEspecie(animal) !== filtros.especie) return false;
    if (filtros.ong !== 'todas' && String(animal.ongId) !== filtros.ong) return false;
    if (filtros.situacao !== 'todas' && animal.status !== filtros.situacao) return false;
    if (filtros.urgentes && !animal.urgente) return false;

    const falta = faltaDe(animal);
    if (filtros.falta === 'mais' && falta <= 1000) return false;
    if (['200', '500', '1000'].includes(String(filtros.falta)) && falta > Number(filtros.falta)) return false;

    return true;
  });

  const porNome = (a, b) => String(a.nome).localeCompare(String(b.nome), 'pt-BR');
  const porCadastro = (a, b) => Number(a.id) - Number(b.id);
  const ordenadores = {
    recentes: (a, b) => porCadastro(b, a),
    antigos: porCadastro,
    nome: porNome,
    urgentes: (a, b) => (Number(Boolean(b.urgente)) - Number(Boolean(a.urgente))) || porNome(a, b),
    proximos: (a, b) => (Number(b.percentual || 0) - Number(a.percentual || 0)) || porNome(a, b),
    precisam: (a, b) => (Number(a.percentual || 0) - Number(b.percentual || 0)) || porNome(a, b),
    menorFalta: (a, b) => (faltaDe(a) - faltaDe(b)) || porNome(a, b)
  };

  // Sem escolha, a lista fica na ordem de cadastro, que não muda quando um animal é editado.
  return [...resultado].sort(ordenadores[filtros.ordem] || porCadastro);
}

function opcoesDoMapa(mapa, selecionado) {
  return [...mapa.entries()]
    .sort((a, b) => String(a[1]).localeCompare(String(b[1]), 'pt-BR'))
    .map(([valor, rotulo]) => `<option value="${esc(valor)}" ${selecionado === valor ? 'selected' : ''}>${esc(rotulo)}</option>`)
    .join('');
}

function opcoesDoObjeto(objeto, selecionado) {
  return Object.entries(objeto)
    .map(([valor, rotulo]) => `<option value="${esc(valor)}" ${String(selecionado) === valor ? 'selected' : ''}>${esc(rotulo)}</option>`)
    .join('');
}

// opcoes.campanha: mostra o filtro de campanha (tela do doador). opcoes.situacao: mostra ativos/inativos (painel de quem cuida).
export function htmlFiltros(animais, filtros, { campanha = true, situacao = false } = {}) {
  const especies = new Map();
  const campanhas = new Map();

  animais.forEach((animal) => {
    const chave = chaveEspecie(animal);
    if (chave && !especies.has(chave)) especies.set(chave, animal.especie);
    if (animal.ongId !== null && animal.ongId !== undefined && !campanhas.has(String(animal.ongId))) {
      campanhas.set(String(animal.ongId), animal.ongNome || 'Campanha');
    }
  });

  return `
    <div class="card filtros-animais">
      <label class="field"><span>Buscar</span><input id="f-busca" type="search" placeholder="Nome, descrição ou campanha" value="${esc(filtros.busca)}"></label>
      <label class="field"><span>Espécie</span>
        <select id="f-especie"><option value="todas">Todas</option>${opcoesDoMapa(especies, filtros.especie)}</select>
      </label>
      ${campanha ? `<label class="field"><span>Campanha</span>
        <select id="f-ong"><option value="todas">Todas</option>${opcoesDoMapa(campanhas, filtros.ong)}</select>
      </label>` : ''}
      ${situacao ? `<label class="field"><span>Situação</span>
        <select id="f-situacao">${opcoesDoObjeto(SITUACOES, filtros.situacao)}</select>
      </label>` : ''}
      <label class="field"><span>Quanto falta</span>
        <select id="f-falta">${opcoesDoObjeto(FALTAS, filtros.falta)}</select>
      </label>
      <label class="field"><span>Ordenar por</span>
        <select id="f-ordem"><option value="">Ordenar por…</option>${opcoesDoObjeto(ORDENACOES, filtros.ordem)}</select>
      </label>
      <label class="field-check"><input id="f-urgentes" type="checkbox" ${filtros.urgentes ? 'checked' : ''}> Só casos urgentes</label>
      <button class="btn btn-ghost btn-sm" type="button" id="f-limpar">Limpar filtros</button>
    </div>
    <p class="muted filtros-resumo" id="resumo-filtros"></p>`;
}

export function ligarFiltros(container, filtros, aoMudar) {
  const campos = {
    busca: container.querySelector('#f-busca'),
    especie: container.querySelector('#f-especie'),
    ong: container.querySelector('#f-ong'),
    situacao: container.querySelector('#f-situacao'),
    falta: container.querySelector('#f-falta'),
    ordem: container.querySelector('#f-ordem'),
    urgentes: container.querySelector('#f-urgentes')
  };

  const ler = () => {
    filtros.busca = campos.busca.value;
    filtros.especie = campos.especie.value;
    if (campos.ong) filtros.ong = campos.ong.value;
    if (campos.situacao) filtros.situacao = campos.situacao.value;
    filtros.falta = campos.falta.value;
    filtros.ordem = campos.ordem.value;
    filtros.urgentes = campos.urgentes.checked;
    aoMudar();
  };

  campos.busca.addEventListener('input', ler);
  [campos.especie, campos.ong, campos.situacao, campos.falta, campos.ordem, campos.urgentes]
    .filter(Boolean)
    .forEach((campo) => campo.addEventListener('change', ler));

  container.querySelector('#f-limpar').addEventListener('click', () => {
    const inicial = filtrosIniciais();
    campos.busca.value = inicial.busca;
    campos.especie.value = inicial.especie;
    if (campos.ong) campos.ong.value = inicial.ong;
    if (campos.situacao) campos.situacao.value = inicial.situacao;
    campos.falta.value = inicial.falta;
    campos.ordem.value = inicial.ordem;
    campos.urgentes.checked = inicial.urgentes;
    ler();
  });
}
