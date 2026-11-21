// Lê uma imagem escolhida pelo usuário, reduz para no máximo 800px de largura
// e devolve o resultado como texto (data URL), pronto para ser enviado à API.
export function lerImagemRedimensionada(arquivo, larguraMaxima = 800, qualidade = 0.82) {
  return new Promise((resolver, rejeitar) => {
    if (!arquivo || !String(arquivo.type).startsWith('image/')) {
      rejeitar(new Error('Escolha um arquivo de imagem (JPG, PNG ou WEBP).'));
      return;
    }

    const leitor = new FileReader();
    leitor.onerror = () => rejeitar(new Error('Não foi possível ler a imagem.'));
    leitor.onload = () => {
      const imagem = new Image();
      imagem.onerror = () => rejeitar(new Error('A imagem escolhida é inválida.'));
      imagem.onload = () => {
        const escala = Math.min(1, larguraMaxima / imagem.width);
        const canvas = document.createElement('canvas');
        canvas.width = Math.round(imagem.width * escala);
        canvas.height = Math.round(imagem.height * escala);

        const contexto = canvas.getContext('2d');
        contexto.fillStyle = '#ffffff';
        contexto.fillRect(0, 0, canvas.width, canvas.height);
        contexto.drawImage(imagem, 0, 0, canvas.width, canvas.height);

        resolver(canvas.toDataURL('image/jpeg', qualidade));
      };
      imagem.src = leitor.result;
    };
    leitor.readAsDataURL(arquivo);
  });
}
