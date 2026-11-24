import * as api from '../api.js';
import {
  esc, fmtMoeda, fmtData, iniciais, barraProgresso, statusBadge,
  animalCard, toast, fotoAnimal, statusDoacaoBadge
} from '../components.js';
import { abrirDoacao } from '../doacao-modal.js';
import { mesmoId } from '../ids.js';
import { getUsuarioLogado } from '../state.js';
import { ligarDetalhes, registrarAnimais } from '../detalhes-animal.js';

export async function renderPadrinho(app) {
  const [ongs, animais] = await Promise.all([
    api.listarOngs(),
    api.listarAnimais()
  ]);
  const mapaOngs = new Map(ongs.map((ong) => [String(ong.id), ong]));

  const padrinho = getUsuarioLogado() || {};
  const temPadrinho = padrinho.id !== null && padrinho.id !== undefined;

  if (!temPadrinho) {
    app.innerHTML = `
      <div class="page-head">
        <div>
          <h1>Área do padrinho</h1>
          <p class="muted">Acompanhe seus afilhados e contribua mensalmente com cada um.</p>
        </div>
      </div>
      <div class="empty-state">
        <h2>Nenhum padrinho cadastrado</h2>
        <p class="muted">Cadastre um usuário com perfil PADRINHO para usar esta área. A aplicação cria um padrinho padrão automaticamente em um banco vazio.</p>
      </div>`;
    return;
  }

  const afilhados = animais.filter((animal) =>
    (animal.padrinhoIds || []).some((id) => mesmoId(id, padrinho.id))
  );

  const doacoes = await api.listarDoacoes({ padrinhoId: padrinho.id });
  const doacoesConfirmadas = doacoes.filter((doacao) => doacao.status === 'DOADO');

  const disponiveis = animais.filter((animal) =>
    animal.status === 'ATIVO' &&
    !afilhados.some((afilhado) => mesmoId(afilhado.id, animal.id))
  );

  const totalDoado = doacoesConfirmadas.reduce((soma, doacao) => soma + Number(doacao.valor || 0), 0);
  const mapaAnimais = new Map(animais.map((animal) => [String(animal.id), animal.nome]));
  const primeiroNome = String(padrinho.nome || 'Padrinho').trim().split(/\s+/)[0];

  app.innerHTML = `
    <div class="page-head">
      <div>
        <h1>Olá, ${esc(primeiroNome)}</h1>
        <p class="muted">Acompanhe seus afilhados e contribua mensalmente com cada um.</p>
      </div>
    </div>

    <div class="stats">
      <div class="stat"><div class="stat-label">Afilhados</div><div class="stat-value">${afilhados.length}</div></div>
      <div class="stat"><div class="stat-label">Total doado</div><div class="stat-value">${fmtMoeda(totalDoado)}</div></div>
      <div class="stat"><div class="stat-label">Doações confirmadas</div><div class="stat-value">${doacoesConfirmadas.length}</div></div>
    </div>

    <h2 class="section-title">Meus afilhados</h2>
    ${afilhados.length
      ? `<div class="grid">${afilhados.map((animal) => cardAfilhado(animal)).join('')}</div>`
      : '<div class="empty-state"><p>Você ainda não apadrinhou nenhum animal. Escolha um na lista abaixo.</p></div>'}

    <h2 class="section-title">Animais para apadrinhar</h2>
    ${disponiveis.length
      ? `<div class="grid">${disponiveis.map((animal) => animalCard(animal, {
          acoes: `<button class="btn btn-primary btn-sm" type="button" data-apadrinhar="${esc(animal.id)}">Apadrinhar ${esc(animal.nome)}</button>`
        })).join('')}</div>`
      : '<div class="empty-state"><p>Você já apadrinha todos os animais ativos. Obrigado!</p></div>'}

    <h2 class="section-title">Minhas doações</h2>
    ${doacoes.length
      ? tabelaMinhasDoacoes(doacoes, mapaAnimais)
      : '<div class="empty-state"><p>Nenhuma doação registrada ainda.</p></div>'}`;

  registrarAnimais(animais, ongs);
  ligarDetalhes(app);
  preencherUltimasAtualizacoes(app, afilhados);

  app.querySelectorAll('[data-padrinhar]').forEach((botao) => {
    botao.addEventListener('click', () => {
      const animal = afilhados.find((item) => mesmoId(item.id, botao.dataset.padrinhar));
      if (!animal) return;

      const ong = mapaOngs.get(String(animal.ongId));
      if (!ong) {
        toast('Não foi possível encontrar os dados de pagamento desta ONG.', 'erro');
        return;
      }

      abrirDoacao({
        animal,
        ong,
        perfil: 'PADRINHO',
        doador: padrinho,
        valorSugerido: animal.mensalidadeSugerida ?? 50,
        aoConcluir: () => renderPadrinho(app)
      });
    });
  });

  app.querySelectorAll('[data-apadrinhar]').forEach((botao) => {
    botao.addEventListener('click', async () => {
      const animal = disponiveis.find((item) => mesmoId(item.id, botao.dataset.apadrinhar));
      if (!animal) return;

      botao.disabled = true;
      try {
        await api.apadrinharAnimal(animal.id, padrinho.id);
        toast(`${animal.nome} agora é seu afilhado.`);
        await renderPadrinho(app);
      } catch (erro) {
        toast(erro.message, 'erro');
        botao.disabled = false;
      }
    });
  });
}

function resumir(texto, limite = 140) {
  const limpo = String(texto || '').trim();
  return limpo.length > limite ? `${limpo.slice(0, limite).trim()}…` : limpo;
}

// Mostra, em cada card de afilhado, a novidade mais recente publicada por quem cuida dele.
function preencherUltimasAtualizacoes(app, afilhados) {
  afilhados.forEach((animal) => {
    const alvo = app.querySelector(`[data-ultima="${animal.id}"]`);
    if (!alvo) return;

    api.listarAtualizacoes(animal.id)
      .then((lista) => {
        alvo.innerHTML = lista.length
          ? `<p class="muted small">Última atualização · ${fmtData(lista[0].dataPublicacao)}</p>
             <p class="ultima-texto">${esc(resumir(lista[0].texto))}</p>`
          : '<p class="muted small">Ainda não há atualizações.</p>';
      })
      .catch(() => {
        alvo.innerHTML = '';
      });
  });
}

function cardAfilhado(animal) {
  const inativo = animal.status !== 'ATIVO';
  const necessidades = (animal.necessidades || [])
    .map((item) => `<li>${esc(item)}</li>`)
    .join('') || '<li>Sem necessidades registradas.</li>';

  return `
    <article class="card animal-card">
      ${fotoAnimal(animal)}
      <div class="animal-top">
        <div class="animal-avatar">${iniciais(animal.nome)}</div>
        <div class="animal-id">
          <h3>${esc(animal.nome)}</h3>
          <p class="muted">${esc(animal.especie)} · ${esc(animal.idade)}</p>
        </div>
        ${statusBadge(animal.status)}
      </div>
      <p class="animal-desc animal-desc-resumo">${esc(animal.descricao)}</p>
      ${barraProgresso(animal)}
      <div class="needs">
        <h4>Necessidades</h4>
        <ul class="needs-list">${necessidades}</ul>
      </div>
      <p class="muted small">Apadrinhado por você · ${esc(animal.ongNome || 'ONG')}</p>
      <div class="ultima-atualizacao" data-ultima="${esc(animal.id)}">
        <p class="muted small">Buscando a última atualização…</p>
      </div>
      <div class="card-actions">
        <button class="btn btn-ghost btn-sm" type="button" data-detalhes="${esc(animal.id)}">Ver detalhes</button>
        <button class="btn btn-primary btn-sm" type="button" data-padrinhar="${esc(animal.id)}" ${inativo ? 'disabled' : ''}>
          ${inativo
            ? 'Campanha encerrada'
            : `Doar mensalidade${animal.mensalidadeSugerida ? ` (${fmtMoeda(animal.mensalidadeSugerida)})` : ''}`}
        </button>
      </div>
    </article>`;
}

function tabelaMinhasDoacoes(doacoes, mapaAnimais) {
  return `
    <div class="card table-wrap">
      <table class="table">
        <thead>
          <tr><th>Data</th><th>Animal</th><th>Valor</th><th>Situação</th></tr>
        </thead>
        <tbody>
          ${doacoes.map((doacao) => `
            <tr>
              <td>${fmtData(doacao.data)}</td>
              <td>${esc(mapaAnimais.get(String(doacao.animalId)) || '—')}</td>
              <td class="valor">${fmtMoeda(doacao.valor)}</td>
              <td>${statusDoacaoBadge(doacao.status)}</td>
            </tr>`).join('')}
        </tbody>
      </table>
    </div>`;
}
