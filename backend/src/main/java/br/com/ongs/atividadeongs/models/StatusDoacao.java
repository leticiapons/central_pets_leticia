package br.com.ongs.atividadeongs.models;

/**
 * Situação de uma doação:
 * DOADO = recebimento confirmado pela ONG ou protetor (só estas contam na meta);
 * CANCELADA = a ONG ou protetor marcou que o PIX não foi recebido;
 * PENDENTE = o doador avisou que pagou, e a ONG ou protetor ainda não confirmou.
 */
public enum StatusDoacao {
    DOADO,
    CANCELADA,
    PENDENTE
}
