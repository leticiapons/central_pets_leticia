import * as api from '../api.js';
import { esc, animalCard, toast } from '../components.js';
import { abrirDoacao } from '../doacao-modal.js';
import { aplicarFiltros, filtrosIniciais, htmlFiltros, ligarFiltros } from '../filtros-animais.js';
import { ligarDetalhes, registrarAnimais } from '../detalhes-animal.js';
import { mesmoId } from '../ids.js';
import { getUsuarioLogado } from '../state.js';

// Os filtros escolhidos ficam guardados enquanto a pessoa navega pelo site.
const filtros = filtrosIniciais();

export async function renderUsuario(app) {
  const [ongs, animais] = await Promise.all([
    api.listarOngs(),
    api.listarAnimais({ status: 'ATIVO' })
  ]);
  const mapaOngs = new Map(ongs.map((ong) => [String(ong.id), ong]));

  app.innerHTML = `
    <div class="page-head">
      <div>
        <h1>Doar</h1>
        <p class="muted">Escolha um animal e faça sua doação via PIX.</p>
      </div>
    </div>

    ${animais.length
      ? `${htmlFiltros(animais, filtros)}<div id="lista-animais"></div>`
      : '<div class="empty-state"><p>Nenhum animal ativo para doação.</p></div>'}

    <p class="hint">Depois de pagar o PIX, a doação fica aguardando a confirmação de quem a recebeu. Ela só entra na meta do animal depois dessa confirmação.</p>`;

  if (!animais.length) return;

  registrarAnimais(animais, ongs);
  const lista = app.querySelector('#lista-animais');
  const resumo = app.querySelector('#resumo-filtros');

  const atualizarLista = () => {
    const visiveis = aplicarFiltros(animais, filtros);

    resumo.textContent = visiveis.length === animais.length
      ? `${animais.length} ${animais.length === 1 ? 'animal precisa' : 'animais precisam'} de ajuda.`
      : `Mostrando ${visiveis.length} de ${animais.length} animais.`;

    lista.innerHTML = visiveis.length
      ? `<div class="grid">${visiveis.map((animal) => animalCard(animal, {
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

  ligarFiltros(app, filtros, atualizarLista);
  atualizarLista();
}
