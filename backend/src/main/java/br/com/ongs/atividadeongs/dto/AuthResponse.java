package br.com.ongs.atividadeongs.dto;

public record AuthResponse(
        String token,
        UsuarioLogadoResponse usuario) {
}
