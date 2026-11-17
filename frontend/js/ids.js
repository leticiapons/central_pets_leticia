/**
 * Compara identificadores mesmo quando um lado é number (API) e o outro é
 * string (atributos data-* do DOM). Usar sempre esta função para comparar ids.
 */
export function mesmoId(a, b) {
  if (a === null || a === undefined || b === null || b === undefined) {
    return false;
  }
  return String(a) === String(b);
}
