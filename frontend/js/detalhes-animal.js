import * as api from './api.js';
import { abrirModal, barraProgresso, esc, fmtData, fotoAnimal } from './components.js';
import { htmlLinhaDoTempo } from './atualizacoes.js';

// Página (janela) com todos os detalhes de um animal e as atualizações de quem cuida dele.

let animaisPorId = new Map();
let ongsPorId = new Map();

export function registrarAnimais(animais, ongs = []) {
  animaisPorId = new Map(animais.map((animal) => [String(animal.id), animal]));
  ongsPorId = new Map(ongs.map((ong) => [String(ong.id), ong]));
}

// Liga os botões "Ver detalhes" que existirem dentro do elemento informado.
export function ligarDetalhes(container) {
  container.querySelectorAll('[data-detalhes]').forEach((botao) => {
    botao.addEventListener('click', () => {
      const animal = animaisPorId.get(String(botao.dataset.detalhes));
      if (animal) abrirDetalhes(animal);
    });
  });
}

export function abrirDetalhes(animal) {
  const ong = ongsPorId.get(String(animal.ongId)) || null;
  const necessidades = (animal.necessidades || [])
    .map((item) => `<li>${esc(item)}</li>`)
    .join('');

  abrirModal({
    titulo: animal.nome,
    largo: true,
    conteudo: `
      ${fotoAnimal(animal, 'animal-photo-lg')}
      <p class="muted">
        ${esc(animal.especie)} · ${esc(animal.idade)}
        ${animal.dataCadastro ? ` · Publicado em ${fmtData(animal.dataCadastro)}` : ''}
      </p>
      ${animal.urgente && animal.status === 'ATIVO' ? '<p><span class="badge badge-urgente">Caso urgente</span></p>' : ''}
      ${barraProgresso(animal)}

      <h3 class="titulo-secao">Sobre ${esc(animal.nome)}</h3>
      <p class="detalhe-texto">${esc(animal.descricao)}</p>

      ${necessidades ? `<h3 class="titulo-secao">Do que precisa</h3><ul class="needs-list">${necessidades}</ul>` : ''}

      ${ong ? `
        <h3 class="titulo-secao">Quem cuida</h3>
        <div class="detalhe-ong">
          <strong>${esc(ong.nome)}</strong>
          ${ong.descricao ? `<p class="detalhe-texto">${esc(ong.descricao)}</p>` : ''}
          ${ong.telefone ? `<p class="muted small">Telefone: ${esc(ong.telefone)}</p>` : ''}
          ${ong.endereco ? `<p class="muted small">Endereço: ${esc(ong.endereco)}</p>` : ''}
        </div>` : ''}

      <h3 class="titulo-secao">Atualizações</h3>
      <div id="atualizacoes-animal"><p class="muted">Carregando…</p></div>`,
    async onMount(modal) {
      const alvo = modal.querySelector('#atualizacoes-animal');
      try {
        const itens = await api.listarAtualizacoes(animal.id);
        alvo.innerHTML = htmlLinhaDoTempo(itens);
      } catch (erro) {
        alvo.innerHTML = `<p class="muted">${esc(erro.message)}</p>`;
      }
    }
  });
}
