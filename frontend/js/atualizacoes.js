import * as api from './api.js';
import { abrirModal, esc, fmtData, toast } from './components.js';
import { lerImagemRedimensionada } from './imagem.js';

// Linha do tempo das novidades de um animal (usada na página do animal e no painel de quem cuida dele).
export function htmlLinhaDoTempo(atualizacoes, { removivel = false } = {}) {
  if (!atualizacoes.length) {
    return '<p class="muted">Ainda não há atualizações para este animal.</p>';
  }

  return `
    <ol class="linha-tempo">
      ${atualizacoes.map((atualizacao) => `
        <li class="atualizacao">
          <div class="atualizacao-data">${fmtData(atualizacao.dataPublicacao)}</div>
          <p class="atualizacao-texto">${esc(atualizacao.texto)}</p>
          ${atualizacao.fotoUrl
    ? `<img class="atualizacao-foto" src="${esc(atualizacao.fotoUrl)}" alt="Foto da atualização" loading="lazy" onerror="this.remove()">`
    : ''}
          ${removivel
    ? `<button class="btn btn-sm btn-ghost" type="button" data-excluir-atualizacao="${esc(atualizacao.id)}">Excluir</button>`
    : ''}
        </li>`).join('')}
    </ol>`;
}

// Janela do painel da ONG ou protetor: publicar e excluir atualizações de um animal.
export function abrirGerenciadorAtualizacoes(animal) {
  abrirModal({
    titulo: `Atualizações de ${animal.nome}`,
    largo: true,
    conteudo: `
      <form class="form" id="form-atualizacao">
        <label class="field">
          <span>Nova atualização</span>
          <textarea name="texto" required maxlength="1000" placeholder="Conte as novidades: tratamento, recuperação, adoção..."></textarea>
        </label>
        <label class="field"><span>Foto (opcional)</span><input name="foto" type="file" accept="image/*"></label>
        <img id="previa-atualizacao" class="animal-photo animal-photo-preview" alt="Prévia da foto" hidden>
        <div class="form-actions"><button class="btn btn-primary" type="submit">Publicar</button></div>
      </form>
      <h3 class="titulo-secao">Já publicadas</h3>
      <div id="lista-atualizacoes"><p class="muted">Carregando…</p></div>`,
    onMount(modal) {
      const form = modal.querySelector('#form-atualizacao');
      const campoFoto = modal.querySelector('input[name="foto"]');
      const previa = modal.querySelector('#previa-atualizacao');
      const lista = modal.querySelector('#lista-atualizacoes');
      let fotoUrl = null;

      const carregar = async () => {
        try {
          const itens = await api.listarAtualizacoes(animal.id);
          lista.innerHTML = htmlLinhaDoTempo(itens, { removivel: true });

          lista.querySelectorAll('[data-excluir-atualizacao]').forEach((botao) => {
            botao.addEventListener('click', async () => {
              // Primeiro clique pede confirmação; o segundo exclui.
              if (!botao.dataset.confirmar) {
                botao.dataset.confirmar = '1';
                botao.textContent = 'Clique de novo para excluir';
                return;
              }
              try {
                await api.excluirAtualizacao(animal.id, botao.dataset.excluirAtualizacao);
                toast('Atualização excluída.');
                await carregar();
              } catch (erro) {
                toast(erro.message, 'erro');
              }
            });
          });
        } catch (erro) {
          lista.innerHTML = `<p class="muted">${esc(erro.message)}</p>`;
        }
      };

      campoFoto.addEventListener('change', async () => {
        const arquivo = campoFoto.files[0];
        fotoUrl = null;
        previa.hidden = true;
        if (!arquivo) return;

        try {
          fotoUrl = await lerImagemRedimensionada(arquivo, 900);
          previa.src = fotoUrl;
          previa.hidden = false;
        } catch (erro) {
          campoFoto.value = '';
          toast(erro.message, 'erro');
        }
      });

      form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        const botao = form.querySelector('button[type="submit"]');
        botao.disabled = true;

        try {
          await api.publicarAtualizacao(animal.id, {
            texto: form.elements.texto.value.trim(),
            fotoUrl
          });
          toast('Atualização publicada.');
          form.reset();
          fotoUrl = null;
          previa.hidden = true;
          await carregar();
        } catch (erro) {
          toast(erro.message, 'erro');
        } finally {
          botao.disabled = false;
        }
      });

      carregar();
    }
  });
}
