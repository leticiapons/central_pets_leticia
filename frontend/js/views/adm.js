import * as api from '../api.js';
import { mesmoId } from '../ids.js';
import {
  esc, fmtMoeda, fmtData, animalCard, abrirModal,
  toast, confirmar, statusDoacaoBadge
} from '../components.js';
import { lerImagemRedimensionada } from '../imagem.js';
import { getUsuarioLogado, atualizarUsuarioLogado } from '../state.js';

const ROTULOS_ABA = {
  animais: 'Animais',
  novo: 'Novo animal',
  doacoes: 'Doações',
  perfil: 'Meu perfil'
};

const PERFIL_DOADOR = {
  USUARIO: 'Doador',
  PADRINHO: 'Padrinho'
};

const PERIODOS = {
  tudo: 'Todo o período',
  7: 'Últimos 7 dias',
  30: 'Últimos 30 dias',
  90: 'Últimos 90 dias',
  mes: 'Este mês',
  personalizado: 'Escolher datas'
};

const SITUACOES = {
  todas: 'Todas as situações',
  PENDENTE: 'Aguardando confirmação',
  DOADO: 'Confirmadas',
  CANCELADA: 'Não recebidas'
};

let abaAtiva = 'animais';
let filtroSituacao = 'todas';
let animalEmEdicao = null;
let filtroPeriodo = 'tudo';
let filtroDe = '';
let filtroAte = '';

export async function renderAdm(app) {
  const usuario = getUsuarioLogado();

  if (!usuario || !usuario.ongId) {
    app.innerHTML = `
      <div class="empty-state">
        <h2>Conta sem campanha</h2>
        <p class="muted">Esta conta não está ligada a uma ONG ou protetor.</p>
      </div>`;
    return;
  }

  const [ong, todosAnimais, doacoes] = await Promise.all([
    api.obterOng(usuario.ongId),
    api.listarAnimais(),
    api.listarDoacoesDaMinhaOng()
  ]);
  const animais = todosAnimais.filter((animal) => mesmoId(animal.ongId, usuario.ongId));

  const pendentes = doacoes.filter((doacao) => doacao.status === 'PENDENTE').length;
  const rotuloAba = (aba) => {
    if (aba === 'novo' && animalEmEdicao) return 'Editar animal';
    if (aba === 'doacoes' && pendentes) return `Doações (${pendentes})`;
    return ROTULOS_ABA[aba];
  };

  app.innerHTML = `
    <div class="page-head">
      <div>
        <h1>Meu painel</h1>
        <p class="muted">${esc(ong.nome)} · gestão de animais, necessidades e doações</p>
      </div>
    </div>

    <nav class="tabs">
      ${Object.keys(ROTULOS_ABA).map((aba) => `<button class="tab" type="button" data-aba="${aba}">${rotuloAba(aba)}</button>`).join('')}
    </nav>

    <section id="aba-conteudo"></section>`;

  const conteudo = app.querySelector('#aba-conteudo');
  const abas = [...app.querySelectorAll('.tab')];
  const contexto = { ong, animais, doacoes, app };

  const ativar = (aba) => {
    abaAtiva = aba;
    abas.forEach((botao) => botao.classList.toggle('active', botao.dataset.aba === aba));
    renderAba(conteudo, aba, contexto);
  };

  abas.forEach((botao) => botao.addEventListener('click', () => {
    if (animalEmEdicao) {
      animalEmEdicao = null;
      abas.find((item) => item.dataset.aba === 'novo').textContent = ROTULOS_ABA.novo;
    }
    ativar(botao.dataset.aba);
  }));
  ativar(abaAtiva);
}

function renderAba(container, aba, contexto) {
  if (aba === 'animais') return renderAnimais(container, contexto);
  if (aba === 'novo') return renderNovoAnimal(container, contexto);
  if (aba === 'doacoes') return renderDoacoes(container, contexto);
  return renderPerfilOng(container, contexto);
}

function renderAnimais(container, contexto) {
  const { animais, doacoes } = contexto;

  if (!animais.length) {
    container.innerHTML = '<div class="empty-state"><p>Nenhum animal cadastrado ainda.</p></div>';
    return;
  }

  container.innerHTML = `<div class="grid">${animais.map((animal) => animalCard(animal, {
    acoes: `
      <button class="btn btn-sm btn-ghost" type="button" data-ver="${esc(animal.id)}">Ver doações</button>
      <button class="btn btn-sm btn-ghost" type="button" data-editar="${esc(animal.id)}">Editar</button>
      ${animal.status === 'ATIVO'
        ? `<button class="btn btn-sm ${animal.metaAtingida ? 'btn-danger' : 'btn-disabled'}" type="button"
                  data-inativar="${esc(animal.id)}" ${animal.metaAtingida ? '' : 'disabled title="Disponível quando a meta for atingida"'}>Inativar</button>`
        : `<button class="btn btn-sm btn-primary" type="button" data-reativar="${esc(animal.id)}">Reativar</button>`}`
  })).join('')}</div>`;

  container.querySelectorAll('[data-ver]').forEach((botao) => {
    botao.addEventListener('click', () => {
      const animal = animais.find((item) => mesmoId(item.id, botao.dataset.ver));
      if (!animal) return;
      const doacoesDoAnimal = doacoes.filter((doacao) => mesmoId(doacao.animalId, animal.id));
      abrirModal({
        titulo: `Doações para ${animal.nome}`,
        largo: true,
        conteudo: doacoesDoAnimal.length
          ? tabelaDoacoes(doacoesDoAnimal)
          : '<div class="empty-state"><p>Nenhuma doação registrada para este animal.</p></div>'
      });
    });
  });

  container.querySelectorAll('[data-editar]').forEach((botao) => {
    botao.addEventListener('click', async () => {
      const animal = animais.find((item) => mesmoId(item.id, botao.dataset.editar));
      if (!animal) return;
      animalEmEdicao = animal;
      abaAtiva = 'novo';
      await renderAdm(contexto.app);
    });
  });

  container.querySelectorAll('[data-reativar]').forEach((botao) => {
    botao.addEventListener('click', async () => {
      const animal = animais.find((item) => mesmoId(item.id, botao.dataset.reativar));
      if (!animal) return;
      const confirmou = await confirmar({
        titulo: 'Reativar animal',
        mensagem: `Reativar ${animal.nome}? Ele voltará a aparecer para novas doações.`,
        textoConfirmar: 'Reativar'
      });
      if (!confirmou) return;

      try {
        await api.reativarAnimal(animal.id);
        toast(animal.metaAtingida
          ? `${animal.nome} foi reativado, mas a meta já foi atingida. Use Editar para aumentar a meta.`
          : `${animal.nome} foi reativado.`);
        await renderAdm(contexto.app);
      } catch (erro) {
        toast(erro.message, 'erro');
      }
    });
  });

  container.querySelectorAll('[data-inativar]').forEach((botao) => {
    botao.addEventListener('click', async () => {
      const animal = animais.find((item) => mesmoId(item.id, botao.dataset.inativar));
      if (!animal) return;
      const confirmou = await confirmar({
        titulo: 'Inativar animal',
        mensagem: `Inativar ${animal.nome}? Ele deixará de aparecer para novas doações, mas o histórico permanece.`,
        textoConfirmar: 'Inativar',
        perigo: true
      });
      if (!confirmou) return;

      try {
        await api.inativarAnimal(animal.id);
        toast(`${animal.nome} foi inativado.`);
        await renderAdm(contexto.app);
      } catch (erro) {
        toast(erro.message, 'erro');
      }
    });
  });
}

function renderNovoAnimal(container, contexto) {
  const animal = animalEmEdicao;

  container.innerHTML = `
    <form class="card form" id="form-animal">
      ${animal ? `<p class="muted">Editando <strong>${esc(animal.nome)}</strong>. Altere o que precisar e salve.</p>` : ''}
      <div class="form-grid">
        <label class="field"><span>Nome</span><input name="nome" required maxlength="60" placeholder="Ex.: Mel" value="${esc(animal ? animal.nome : '')}"></label>
        <label class="field"><span>Espécie</span><input name="especie" required maxlength="40" placeholder="Cachorro, Gato..." value="${esc(animal ? animal.especie : '')}"></label>
        <label class="field"><span>Idade</span><input name="idade" maxlength="30" placeholder="Ex.: 3 anos" value="${esc(animal && animal.idade ? animal.idade : '')}"></label>
        <label class="field"><span>Meta de arrecadação (R$)</span><input name="meta" type="number" min="1" step="0.01" required placeholder="1200" value="${esc(animal ? animal.meta : '')}"></label>
        <label class="field full"><span>Descrição</span><textarea name="descricao" required placeholder="História e situação do animal">${esc(animal ? animal.descricao : '')}</textarea></label>
        <label class="field full"><span>Necessidades (separadas por vírgula)</span><input name="necessidades" placeholder="Ração, consultas, castração" value="${esc(animal ? (animal.necessidades || []).join(', ') : '')}"></label>
        <label class="field full"><span>Foto do animal (opcional)</span><input name="foto" type="file" accept="image/*"></label>
        <div class="field full">
          <img id="previa-foto" class="animal-photo animal-photo-preview" alt="Prévia da foto" hidden>
          <div><button class="btn btn-sm btn-ghost" type="button" id="remover-foto" hidden>Remover foto</button></div>
        </div>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" type="submit">${animal ? 'Salvar alterações' : 'Cadastrar animal'}</button>
        ${animal ? '<button class="btn btn-ghost" type="button" id="cancelar-edicao">Cancelar</button>' : ''}
      </div>
    </form>`;

  let fotoUrl = animal && animal.fotoUrl ? animal.fotoUrl : null;
  const campoFoto = container.querySelector('input[name="foto"]');
  const previaFoto = container.querySelector('#previa-foto');
  const botaoRemoverFoto = container.querySelector('#remover-foto');

  const mostrarPrevia = () => {
    if (fotoUrl) {
      previaFoto.src = fotoUrl;
      previaFoto.hidden = false;
      botaoRemoverFoto.hidden = false;
    } else {
      previaFoto.removeAttribute('src');
      previaFoto.hidden = true;
      botaoRemoverFoto.hidden = true;
    }
  };
  mostrarPrevia();

  campoFoto.addEventListener('change', async () => {
    const arquivo = campoFoto.files[0];
    if (!arquivo) return;

    try {
      fotoUrl = await lerImagemRedimensionada(arquivo);
      mostrarPrevia();
    } catch (erro) {
      campoFoto.value = '';
      toast(erro.message, 'erro');
    }
  });

  botaoRemoverFoto.addEventListener('click', () => {
    fotoUrl = null;
    campoFoto.value = '';
    mostrarPrevia();
  });

  const cancelar = container.querySelector('#cancelar-edicao');
  if (cancelar) {
    cancelar.addEventListener('click', async () => {
      animalEmEdicao = null;
      abaAtiva = 'animais';
      await renderAdm(contexto.app);
    });
  }

  container.querySelector('#form-animal').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const dados = Object.fromEntries(new FormData(evento.target));

    const payload = {
      nome: dados.nome.trim(),
      especie: dados.especie.trim(),
      idade: String(dados.idade || '').trim() || 'Não informada',
      descricao: dados.descricao.trim(),
      meta: Number(dados.meta),
      necessidades: String(dados.necessidades || '')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean),
      mensalidadeSugerida: animal && animal.mensalidadeSugerida != null ? animal.mensalidadeSugerida : 50,
      fotoUrl
    };

    try {
      if (animal) {
        await api.atualizarAnimal(animal.id, payload);
        toast('Alterações salvas.');
      } else {
        await api.criarAnimal(payload);
        toast('Animal cadastrado com sucesso.');
      }
      animalEmEdicao = null;
      abaAtiva = 'animais';
      await renderAdm(contexto.app);
    } catch (erro) {
      toast(erro.message, 'erro');
    }
  });
}

function filtrarPorPeriodo(doacoes) {
  const agora = new Date();
  let inicio = null;
  let fim = null;

  if (['7', '30', '90'].includes(String(filtroPeriodo))) {
    inicio = new Date(agora);
    inicio.setDate(inicio.getDate() - Number(filtroPeriodo));
  } else if (filtroPeriodo === 'mes') {
    inicio = new Date(agora.getFullYear(), agora.getMonth(), 1);
  } else if (filtroPeriodo === 'personalizado') {
    if (filtroDe) inicio = new Date(`${filtroDe}T00:00:00`);
    if (filtroAte) fim = new Date(`${filtroAte}T23:59:59.999`);
  }

  return doacoes.filter((doacao) => {
    const data = new Date(doacao.data);
    if (inicio && data < inicio) return false;
    if (fim && data > fim) return false;
    return true;
  });
}

function renderDoacoes(container, contexto) {
  const { doacoes, animais } = contexto;
  const noPeriodo = filtrarPorPeriodo(doacoes);
  const filtradas = filtroSituacao === 'todas'
    ? noPeriodo
    : noPeriodo.filter((doacao) => doacao.status === filtroSituacao);
  const total = noPeriodo
    .filter((doacao) => doacao.status === 'DOADO')
    .reduce((soma, doacao) => soma + Number(doacao.valor || 0), 0);
  const pendentes = noPeriodo.filter((doacao) => doacao.status === 'PENDENTE').length;
  const ativos = animais.filter((animal) => animal.status === 'ATIVO').length;
  const mapaAnimais = new Map(animais.map((animal) => [String(animal.id), animal.nome]));

  let listagem = tabelaDoacoes(filtradas, mapaAnimais, true);
  if (!doacoes.length) {
    listagem = '<div class="empty-state"><p>Nenhuma doação recebida ainda.</p></div>';
  } else if (!filtradas.length) {
    listagem = '<div class="empty-state"><p>Nenhuma doação com esse filtro.</p></div>';
  }

  container.innerHTML = `
    <div class="card filtro-periodo">
      <label class="field">
        <span>Período</span>
        <select id="filtro-periodo">
          ${Object.entries(PERIODOS).map(([valor, rotulo]) => `<option value="${valor}" ${String(filtroPeriodo) === valor ? 'selected' : ''}>${rotulo}</option>`).join('')}
        </select>
      </label>
      ${filtroPeriodo === 'personalizado' ? `
        <label class="field"><span>De</span><input type="date" id="filtro-de" value="${esc(filtroDe)}"></label>
        <label class="field"><span>Até</span><input type="date" id="filtro-ate" value="${esc(filtroAte)}"></label>` : ''}
      <label class="field">
        <span>Situação</span>
        <select id="filtro-situacao">
          ${Object.entries(SITUACOES).map(([valor, rotulo]) => `<option value="${valor}" ${filtroSituacao === valor ? 'selected' : ''}>${rotulo}</option>`).join('')}
        </select>
      </label>
      <p class="muted filtro-resumo">Mostrando ${filtradas.length} de ${doacoes.length} doações</p>
    </div>
    <div class="stats">
      <div class="stat"><div class="stat-label">Total recebido (confirmado)</div><div class="stat-value">${fmtMoeda(total)}</div></div>
      <div class="stat"><div class="stat-label">Aguardando confirmação</div><div class="stat-value">${pendentes}</div></div>
      <div class="stat"><div class="stat-label">Doações listadas</div><div class="stat-value">${filtradas.length}</div></div>
      <div class="stat"><div class="stat-label">Animais ativos</div><div class="stat-value">${ativos}</div></div>
    </div>
    ${listagem}`;

  container.querySelector('#filtro-periodo').addEventListener('change', (evento) => {
    filtroPeriodo = evento.target.value;
    renderDoacoes(container, contexto);
  });

  container.querySelector('#filtro-situacao').addEventListener('change', (evento) => {
    filtroSituacao = evento.target.value;
    renderDoacoes(container, contexto);
  });

  const campoDe = container.querySelector('#filtro-de');
  const campoAte = container.querySelector('#filtro-ate');
  if (campoDe && campoAte) {
    const atualizarDatas = () => {
      filtroDe = campoDe.value;
      filtroAte = campoAte.value;
      if (filtroDe && filtroAte && filtroDe > filtroAte) {
        toast('A data inicial não pode ser depois da data final.', 'erro');
        return;
      }
      renderDoacoes(container, contexto);
    };
    campoDe.addEventListener('change', atualizarDatas);
    campoAte.addEventListener('change', atualizarDatas);
  }

  const alterarStatus = async (id, status, mensagem) => {
    try {
      await api.atualizarStatusDoacao(id, status);
      toast(mensagem);
      await renderAdm(contexto.app);
    } catch (erro) {
      toast(erro.message, 'erro');
    }
  };

  container.querySelectorAll('[data-confirmar]').forEach((botao) => {
    botao.addEventListener('click', () => alterarStatus(botao.dataset.confirmar, 'DOADO', 'Recebimento confirmado.'));
  });
  container.querySelectorAll('[data-recusar]').forEach((botao) => {
    botao.addEventListener('click', () => alterarStatus(botao.dataset.recusar, 'CANCELADA', 'Doação marcada como não recebida.'));
  });
  container.querySelectorAll('[data-desfazer]').forEach((botao) => {
    botao.addEventListener('click', () => alterarStatus(botao.dataset.desfazer, 'PENDENTE', 'A doação voltou para aguardando confirmação.'));
  });
}

function acoesDaDoacao(doacao) {
  const id = esc(doacao.id);
  if (doacao.status === 'PENDENTE') {
    return `
      <button class="btn btn-sm btn-primary" type="button" data-confirmar="${id}">Confirmar</button>
      <button class="btn btn-sm btn-ghost" type="button" data-recusar="${id}">Não recebi</button>`;
  }
  return `<button class="btn btn-sm btn-ghost" type="button" data-desfazer="${id}">Desfazer</button>`;
}

function tabelaDoacoes(doacoes, mapaAnimais = null, comAcoes = false) {
  return `
    <div class="card table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>Data</th>
            ${mapaAnimais ? '<th>Animal</th>' : ''}
            <th>Doador</th>
            <th>Perfil</th>
            <th>Valor</th>
            <th>Situação</th>
            ${comAcoes ? '<th>Ações</th>' : ''}
          </tr>
        </thead>
        <tbody>
          ${doacoes.map((doacao) => `
            <tr>
              <td>${fmtData(doacao.data)}</td>
              ${mapaAnimais ? `<td>${esc(mapaAnimais.get(String(doacao.animalId)) || '—')}</td>` : ''}
              <td>${esc(doacao.doadorNome || 'Anônimo')}</td>
              <td>${esc(PERFIL_DOADOR[doacao.perfil] || doacao.perfil)}</td>
              <td class="valor">${fmtMoeda(doacao.valor)}</td>
              <td>${statusDoacaoBadge(doacao.status)}</td>
              ${comAcoes ? `<td class="acoes-doacao">${acoesDaDoacao(doacao)}</td>` : ''}
            </tr>`).join('')}
        </tbody>
      </table>
    </div>`;
}

function renderPerfilOng(container, contexto) {
  const { ong } = contexto;
  const tipos = ['CPF/CNPJ', 'E-mail', 'Telefone', 'Chave aleatória'];

  container.innerHTML = `
    <form class="card form" id="form-ong">
      <div class="form-grid">
        <label class="field"><span>Nome (ONG, protetor ou pessoa)</span><input name="nome" required value="${esc(ong.nome)}"></label>
        <label class="field"><span>Telefone</span><input name="telefone" value="${esc(ong.telefone || '')}"></label>
        <label class="field full"><span>Descrição</span><textarea name="descricao">${esc(ong.descricao || '')}</textarea></label>
        <label class="field"><span>Chave PIX</span><input name="chavePix" required value="${esc(ong.chavePix || '')}"></label>
        <label class="field">
          <span>Tipo de chave PIX</span>
          <select name="tipoChavePix">
            ${tipos.map((tipo) => `<option ${ong.tipoChavePix === tipo ? 'selected' : ''}>${tipo}</option>`).join('')}
          </select>
        </label>
        <label class="field full"><span>Endereço</span><input name="endereco" value="${esc(ong.endereco || '')}"></label>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" type="submit">Salvar dados da ONG</button>
      </div>
    </form>`;

  container.querySelector('#form-ong').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const dados = Object.fromEntries(new FormData(evento.target));

    try {
      await api.atualizarMinhaOng(dados);
      atualizarUsuarioLogado({ ongNome: dados.nome });
      toast('Seus dados foram atualizados.');
      await renderAdm(contexto.app);
    } catch (erro) {
      toast(erro.message, 'erro');
    }
  });
}
