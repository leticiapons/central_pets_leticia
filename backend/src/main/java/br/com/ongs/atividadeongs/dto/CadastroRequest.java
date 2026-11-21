package br.com.ongs.atividadeongs.dto;

import br.com.ongs.atividadeongs.models.PerfilUsuario;

/**
 * Corpo do cadastro. O campo "ong" só é usado quando o perfil é ADM
 * (ONG, protetor ou pessoa pedindo ajuda por um animal).
 */
public record CadastroRequest(
        String nome,
        String email,
        String senha,
        PerfilUsuario perfil,
        OngRequest ong) {
}
