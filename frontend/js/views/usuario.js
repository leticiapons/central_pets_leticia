import * as api from '../api.js';
import { esc, animalCard, toast } from '../components.js';
import { abrirDoacao } from '../doacao-modal.js';
import { mesmoId } from '../ids.js';
import { getUsuarioLogado } from '../state.js';

export async function renderUsuario(app) {
  const [ongs, animais] = await Promise.all([
    api.listarOngs(),
    api.listarAnimais({ status: 'ATIVO' })
  ]);
  const mapaOngs = new Map(ongs.map((ong) => [String(ong.id), ong]));

  const resumo = animais.length
    ? `${animais.length} ${animais.length === 1 ? 'animal precisa' : 'animais precisam'} de ajuda.`
    : 'Nenhum animal disponível no momento.';

  app.innerHTML = `
    <div class="page-head">
      <div>
        <h1>Doar</h1>
        <p class="muted">Escolha um animal e faça sua doação via PIX. ${esc(resumo)}</p>
      </div>
    </div>

    ${animais.length
      ? `<div class="grid">${animais.map((animal) => animalCard(animal, {
          acoes: `<button class="btn btn-primary btn-sm" type="button" data-doar="${esc(animal.id)}">Doar para ${esc(animal.nome)}</button>`
        })).join('')}</div>`
      : '<div class="empty-state"><p>Nenhum animal ativo para doação.</p></div>'}

    <p class="hint">A doação é registrada como “doada” por confiança — não há confirmação real de pagamento neste MVP.</p>`;

  app.querySelectorAll('[data-doar]').forEach((botao) => {
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
}
