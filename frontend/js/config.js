// Quando o site é aberto pelo `npx serve` (porta 3000, uso local), a API fica em outra porta (8080).
// Quando o site é servido pelo próprio back-end (por exemplo, na nuvem), a API fica no mesmo endereço.
const servidoPeloNpxServe = (
  ['localhost', '127.0.0.1'].includes(location.hostname) && location.port === '3000'
);

export const CONFIG = {
  USE_MOCK: false,
  MOCK_DIR: 'data',
  BASE_URL: servidoPeloNpxServe ? 'http://localhost:8080/api' : '/api'
};
