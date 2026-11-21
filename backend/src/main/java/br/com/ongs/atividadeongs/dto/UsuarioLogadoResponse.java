package br.com.ongs.atividadeongs.dto;

import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.Usuario;

public record UsuarioLogadoResponse(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        Long ongId,
        String ongNome) {

    public static UsuarioLogadoResponse de(Usuario usuario) {
        Ong ong = usuario.getOng();
        return new UsuarioLogadoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                ong == null ? null : ong.getId(),
                ong == null ? null : ong.getNome());
    }
}
