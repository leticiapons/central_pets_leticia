const SESSAO_KEY = 'ong_sessao';

export const PERFIS = {
  ADM: {
    label: 'Protetor / ONG',
    descricao: 'Cadastre os animais que precisam de ajuda, acompanhe as metas e veja as doações recebidas.'
  },
  USUARIO: {
    label: 'Doador',
    descricao: 'Faça doações via PIX para qualquer animal, no valor que quiser, e acompanhe as metas.'
  },
  PADRINHO: {
    label: 'Padrinho',
    descricao: 'Visualize seus afilhados, veja as necessidades e contribua mensalmente com cada um.'
  }
};

export function getSessao() {
  try {
    const bruto = localStorage.getItem(SESSAO_KEY);
    if (!bruto) return null;
    const sessao = JSON.parse(bruto);
    return sessao && sessao.token && sessao.usuario ? sessao : null;
  } catch {
    return null;
  }
}

export function setSessao(token, usuario) {
  localStorage.setItem(SESSAO_KEY, JSON.stringify({ token, usuario }));
}

export function atualizarUsuarioLogado(parcial) {
  const sessao = getSessao();
  if (sessao) setSessao(sessao.token, { ...sessao.usuario, ...parcial });
}

export function limparSessao() {
  localStorage.removeItem(SESSAO_KEY);
}

export function getToken() {
  const sessao = getSessao();
  return sessao ? sessao.token : null;
}

export function getUsuarioLogado() {
  const sessao = getSessao();
  return sessao ? sessao.usuario : null;
}

export function getPerfil() {
  const usuario = getUsuarioLogado();
  const perfil = usuario ? usuario.perfil : null;
  return perfil && Object.prototype.hasOwnProperty.call(PERFIS, perfil) ? perfil : null;
}
