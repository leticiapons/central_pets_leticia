import { CONFIG } from './config.js';
import * as mock from './mock.js';
import { mesmoId } from './ids.js';
import { getToken, limparSessao } from './state.js';

const USAR_MOCK = CONFIG.USE_MOCK;

async function http(caminho, opcoes = {}) {
  const requisicao = { ...opcoes };
  const token = getToken();

  requisicao.headers = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(opcoes.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
    ...(opcoes.headers || {})
  };

  const resposta = await fetch(`${CONFIG.BASE_URL}${caminho}`, requisicao);

  if (!resposta.ok) {
    let mensagem = `Erro ${resposta.status} ao chamar ${caminho}.`;
    try {
      const corpo = await resposta.json();
      mensagem = corpo.mensagem || corpo.message || corpo.detail || mensagem;
    } catch {
      /* resposta sem corpo JSON */
    }

    // Sessão vencida (ou perdida porque o back-end foi reiniciado): volta para o login.
    if (resposta.status === 401 && token) {
      limparSessao();
      location.hash = '#/';
    }

    throw new Error(mensagem);
  }

  if (resposta.status === 204) return null;
  return resposta.json();
}

function enriquecerAnimal(animal, doacoes) {
  const arrecadado = doacoes
    .filter((d) => mesmoId(d.animalId, animal.id) && d.status === 'DOADO')
    .reduce((soma, d) => soma + Number(d.valor || 0), 0);
  const meta = Number(animal.meta || 0);
  const percentual = meta > 0 ? Math.min(100, Math.round((arrecadado / meta) * 100)) : 0;

  return {
    ...animal,
    arrecadado,
    percentual,
    metaAtingida: meta > 0 && arrecadado >= meta
  };
}

export async function entrar(email, senha) {
  if (USAR_MOCK) {
    throw new Error('O login está disponível apenas com a API real.');
  }

  return http('/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }) });
}

export async function cadastrar(dados) {
  if (USAR_MOCK) {
    throw new Error('O cadastro está disponível apenas com a API real.');
  }

  return http('/auth/cadastro', { method: 'POST', body: JSON.stringify(dados) });
}

export async function usuarioAtual() {
  if (USAR_MOCK) return null;
  return http('/auth/me');
}

export async function sair() {
  if (USAR_MOCK) return;

  try {
    await http('/auth/logout', { method: 'POST' });
  } catch {
    /* mesmo que a API não responda, a sessão local é encerrada */
  }
}

export async function listarOngs() {
  return USAR_MOCK ? [await mock.obterOng()] : http('/ongs');
}

export async function obterOng(id) {
  return USAR_MOCK ? mock.obterOng() : http(`/ongs/${id}`);
}

export async function atualizarMinhaOng(dados) {
  return USAR_MOCK
    ? mock.atualizarOng(dados)
    : http('/ongs/minha', { method: 'PUT', body: JSON.stringify(dados) });
}

export async function listarDoacoesDaMinhaOng() {
  if (USAR_MOCK) return listarDoacoes();

  const doacoes = await http('/ongs/minha/doacoes');
  return [...doacoes].sort((a, b) => new Date(b.data) - new Date(a.data));
}

export async function listarAnimais(filtro = {}) {
  let lista;

  if (USAR_MOCK) {
    const animais = await mock.obterAnimaisBrutos();
    const doacoes = await mock.obterDoacoesBrutas();
    lista = animais.map((animal) => enriquecerAnimal(animal, doacoes));
  } else {
    // A API já devolve arrecadado, percentual e metaAtingida de cada animal.
    lista = await http('/animais');
  }

  if (filtro.status) lista = lista.filter((animal) => animal.status === filtro.status);
  return lista;
}

export async function obterAnimal(id) {
  const animais = await listarAnimais();
  return animais.find((animal) => mesmoId(animal.id, id)) || null;
}

export async function criarAnimal(dados) {
  const payload = { ...dados, status: 'ATIVO' };

  if (USAR_MOCK) {
    const animal = { id: `animal-${Date.now()}`, ...payload };
    return mock.inserirAnimal(animal);
  }

  return http('/animais', { method: 'POST', body: JSON.stringify(payload) });
}

export async function atualizarAnimal(id, dados) {
  if (USAR_MOCK) {
    throw new Error('A edição de animais está disponível apenas com a API real.');
  }

  return http(`/animais/${id}`, { method: 'PUT', body: JSON.stringify(dados) });
}

export async function reativarAnimal(id) {
  if (USAR_MOCK) {
    return mock.alterarStatusAnimal(id, 'ATIVO');
  }

  return http(`/animais/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: 'ATIVO' }) });
}

export async function inativarAnimal(id) {
  const animal = await obterAnimal(id);
  if (!animal) throw new Error('Animal não encontrado.');
  if (!animal.metaAtingida) {
    throw new Error('Só é possível inativar animais que atingiram a meta de doações.');
  }

  return USAR_MOCK
    ? mock.alterarStatusAnimal(id, 'INATIVO')
    : http(`/animais/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: 'INATIVO' }) });
}

export async function listarDoacoes(filtro = {}) {
  let doacoes = USAR_MOCK ? await mock.obterDoacoesBrutas() : await http('/doacoes/minhas');

  doacoes = [...doacoes].sort((a, b) => new Date(b.data) - new Date(a.data));
  if (filtro.animalId != null) {
    doacoes = doacoes.filter((d) => mesmoId(d.animalId, filtro.animalId));
  }
  if (filtro.padrinhoId != null) {
    doacoes = doacoes.filter((d) => mesmoId(d.padrinhoId, filtro.padrinhoId));
  }
  return doacoes;
}

export async function atualizarStatusDoacao(id, status) {
  if (USAR_MOCK) {
    throw new Error('A confirmação de doações está disponível apenas com a API real.');
  }

  return http(`/doacoes/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });
}

export async function criarDoacao(dados) {
  const payload = { ...dados };

  if (USAR_MOCK) {
    const doacao = { id: `doacao-${Date.now()}`, data: new Date().toISOString(), ...payload };
    return mock.inserirDoacao(doacao);
  }

  return http('/doacoes', { method: 'POST', body: JSON.stringify(payload) });
}

export async function listarAfilhados(padrinhoId) {
  if (padrinhoId == null) return [];

  const animais = await listarAnimais();
  return animais.filter((animal) =>
    (animal.padrinhoIds || []).some((id) => mesmoId(id, padrinhoId))
  );
}

export async function apadrinharAnimal(animalId, padrinhoId) {
  if (USAR_MOCK) {
    throw new Error('O apadrinhamento está disponível apenas com a API real.');
  }

  return http(`/animais/${animalId}/padrinhos`, {
    method: 'POST',
    body: JSON.stringify({ usuarioId: padrinhoId })
  });
}

export async function desapadrinharAnimal(animalId, padrinhoId) {
  if (USAR_MOCK) {
    throw new Error('O apadrinhamento está disponível apenas com a API real.');
  }

  return http(`/animais/${animalId}/padrinhos/${padrinhoId}`, { method: 'DELETE' });
}
