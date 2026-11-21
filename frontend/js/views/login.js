import * as api from '../api.js';
import { setSessao } from '../state.js';
import { toast } from '../components.js';

let modo = 'entrar';

const TIPOS_CHAVE_PIX = ['CPF/CNPJ', 'E-mail', 'Telefone', 'Chave aleatória'];

export function renderLogin(app) {
  const entrando = modo === 'entrar';

  app.innerHTML = `
    <section class="perfil-hero">
      <h1>${entrando ? 'Entrar' : 'Criar conta'}</h1>
      <p class="muted">${entrando
        ? 'Acesse com o seu e-mail e a sua senha.'
        : 'Cadastre-se para doar, apadrinhar ou pedir ajuda para animais.'}</p>
    </section>

    <div class="card auth-card">
      <form class="form" id="form-auth">
        <div class="form-grid auth-grid">
          ${entrando ? '' : `
            <label class="field full">
              <span>Eu quero</span>
              <select name="perfil" id="campo-perfil">
                <option value="USUARIO">Doar para animais</option>
                <option value="PADRINHO">Apadrinhar um animal (apoio mensal)</option>
                <option value="ADM">Pedir ajuda para animais (ONG, protetor ou pessoa)</option>
              </select>
            </label>
            <label class="field full"><span>Seu nome</span><input name="nome" required maxlength="120" autocomplete="name"></label>`}
          <label class="field full"><span>E-mail</span><input name="email" type="email" required maxlength="160" autocomplete="email"></label>
          <label class="field full"><span>Senha</span><input name="senha" type="password" required minlength="6" maxlength="72" autocomplete="${entrando ? 'current-password' : 'new-password'}"></label>
          ${entrando ? '' : '<label class="field full"><span>Repita a senha</span><input name="senha2" type="password" required minlength="6" maxlength="72" autocomplete="new-password"></label>'}
        </div>

        ${entrando ? '' : `
          <div id="bloco-ong" class="auth-bloco" hidden>
            <h2 class="auth-bloco-titulo">Sua campanha</h2>
            <p class="muted small">Estes dados aparecem para os doadores. Pode ser uma ONG, um protetor independente ou uma pessoa pedindo ajuda por um animal.</p>
            <div class="form-grid auth-grid">
              <label class="field full"><span>Nome da ONG, do protetor ou de quem pede ajuda</span><input name="ongNome" maxlength="120" data-obrigatorio></label>
              <label class="field full"><span>Apresentação (opcional)</span><textarea name="ongDescricao" placeholder="Quem você é e como cuida dos animais"></textarea></label>
              <label class="field full"><span>Chave PIX para receber as doações</span><input name="chavePix" maxlength="160" data-obrigatorio></label>
              <label class="field full">
                <span>Tipo da chave PIX</span>
                <select name="tipoChavePix">
                  ${TIPOS_CHAVE_PIX.map((tipo) => `<option>${tipo}</option>`).join('')}
                </select>
              </label>
              <label class="field full"><span>Telefone (opcional)</span><input name="telefone" maxlength="30"></label>
              <label class="field full"><span>Cidade ou endereço (opcional)</span><input name="endereco" maxlength="255"></label>
            </div>
          </div>`}

        <div class="form-actions">
          <button class="btn btn-primary btn-block" type="submit">${entrando ? 'Entrar' : 'Criar conta'}</button>
        </div>
      </form>

      <div class="auth-alt">
        <button class="btn btn-ghost btn-block" type="button" id="alternar-modo">
          ${entrando ? 'Não tenho conta: criar conta' : 'Já tenho conta: entrar'}
        </button>
      </div>
    </div>

    ${entrando ? `
      <div class="auth-hint card">
        <strong>Contas de exemplo</strong> (dados fictícios, senha <code>exemplo123</code>)
        <ul>
          <li>ONG Patas Felizes: <code>ong@example.com</code></li>
          <li>Abrigo Cantinho do Gato: <code>abrigo@example.com</code></li>
          <li>Protetora independente: <code>maria@example.com</code></li>
          <li>Doador: <code>doador@example.com</code></li>
          <li>Padrinho: <code>carlos.andrade@example.com</code></li>
        </ul>
      </div>` : ''}`;

  app.querySelector('#alternar-modo').addEventListener('click', () => {
    modo = entrando ? 'cadastro' : 'entrar';
    renderLogin(app);
  });

  const form = app.querySelector('#form-auth');
  const botao = form.querySelector('button[type="submit"]');

  const campoPerfil = form.querySelector('#campo-perfil');
  const blocoOng = form.querySelector('#bloco-ong');
  if (campoPerfil && blocoOng) {
    const atualizarBloco = () => {
      const pedeAjuda = campoPerfil.value === 'ADM';
      blocoOng.hidden = !pedeAjuda;
      blocoOng.querySelectorAll('[data-obrigatorio]').forEach((campo) => {
        campo.required = pedeAjuda;
      });
    };
    campoPerfil.addEventListener('change', atualizarBloco);
    atualizarBloco();
  }

  form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const dados = Object.fromEntries(new FormData(form));

    if (!entrando && dados.senha !== dados.senha2) {
      toast('As senhas não são iguais.', 'erro');
      return;
    }

    botao.disabled = true;

    try {
      let resposta;

      if (entrando) {
        resposta = await api.entrar(dados.email.trim(), dados.senha);
      } else {
        const pedeAjuda = dados.perfil === 'ADM';
        resposta = await api.cadastrar({
          nome: dados.nome.trim(),
          email: dados.email.trim(),
          senha: dados.senha,
          perfil: dados.perfil,
          ong: pedeAjuda
            ? {
              nome: String(dados.ongNome || '').trim(),
              descricao: String(dados.ongDescricao || '').trim(),
              chavePix: String(dados.chavePix || '').trim(),
              tipoChavePix: dados.tipoChavePix,
              telefone: String(dados.telefone || '').trim(),
              endereco: String(dados.endereco || '').trim()
            }
            : null
        });
      }

      setSessao(resposta.token, resposta.usuario);
      modo = 'entrar';
      toast(entrando ? `Bem-vindo(a), ${resposta.usuario.nome}!` : 'Conta criada com sucesso.');
      location.hash = `#/${resposta.usuario.perfil.toLowerCase()}`;
    } catch (erro) {
      toast(erro.message, 'erro');
      botao.disabled = false;
    }
  });
}
