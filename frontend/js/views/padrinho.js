import * as api from '../api.js';
import {
  esc, fmtMoeda, fmtData, iniciais, barraProgresso, statusBadge,
  animalCard, toast
} from '../components.js';
import { abrirDoacao } from '../doacao-modal.js';
import { mesmoId } from '../ids.js';

export async function renderPadrinho(app) {
  const [usuarios, ong, animais] = await Promise.all([
    api.getUsuarios(),
    api.getOng(),
    api.listarAnimais()
  ]);

  const padrinho = usuarios?.padrinhoAtual || {};
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
  const doacoesValidas = doacoes.filter((doacao) => doacao.status !== 'CANCELADA');

  const disponiveis = animais.filter((animal) =>
    animal.status === 'ATIVO' &&
    !afilhados.some((afilhado) => mesmoId(afilhado.id, animal.id))
  );

  const totalDoado = doacoesValidas.reduce((soma, doacao) => soma + Number(doacao.valor || 0), 0);
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
      <div class="stat"><div class="stat-label">Doações realizadas</div><div class="stat-value">${doacoesValidas.length}</div></div>
    </div>

    <h2 class="section-title">Meus afilhados</h2>
    ${afilhados.length
      ? `<div class="grid">${afilhados.map((animal) => cardAfilhado(animal, ong)).join('')}</div>`
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

  app.querySelectorAll('[data-padrinhar]').forEach((botao) => {
    botao.addEventListener('click', () => {
      const animal = afilhados.find((item) => mesmoId(item.id, botao.dataset.padrinhar));
      if (!animal) return;

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

function cardAfilhado(animal, ong) {
  const inativo = animal.status !== 'ATIVO';
  const necessidades = (animal.necessidades || [])
    .map((item) => `<li>${esc(item)}</li>`)
    .join('') || '<li>Sem necessidades registradas.</li>';

  return `
    <article class="card animal-card">
      <div class="animal-top">
        <div class="animal-avatar">${iniciais(animal.nome)}</div>
        <div class="animal-id">
          <h3>${esc(animal.nome)}</h3>
          <p class="muted">${esc(animal.especie)} · ${esc(animal.idade)}</p>
        </div>
        ${statusBadge(animal.status)}
      </div>
      <p class="animal-desc">${esc(animal.descricao)}</p>
      ${barraProgresso(animal)}
      <div class="needs">
        <h4>Necessidades</h4>
        <ul class="needs-list">${necessidades}</ul>
      </div>
      <p class="muted small">Apadrinhado por você · ONG ${esc(ong.nome)}</p>
      <div class="card-actions">
        <button class="btn btn-primary btn-sm" type="button" data-padrinhar="${esc(animal.id)}" ${inativo ? 'disabled' : ''}>
          ${inativo
            ? 'Meta concluída'
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
          <tr><th>Data</th><th>Animal</th><th>Valor</th><th>Status</th></tr>
        </thead>
        <tbody>
          ${doacoes.map((doacao) => {
            const cancelada = doacao.status === 'CANCELADA';
            return `
            <tr>
              <td>${fmtData(doacao.data)}</td>
              <td>${esc(mapaAnimais.get(String(doacao.animalId)) || '—')}</td>
              <td class="valor">${fmtMoeda(doacao.valor)}</td>
              <td><span class="badge ${cancelada ? 'badge-muted' : 'badge-ok'}">${cancelada ? 'Cancelada' : 'Doado'}</span></td>
            </tr>`;
          }).join('')}
        </tbody>
      </table>
    </div>`;
}
