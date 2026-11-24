package br.com.ongs.atividadeongs.dto;

import java.time.Instant;

public record AtualizacaoResponse(
        Long id,
        Long animalId,
        String texto,
        String fotoUrl,
        Instant dataPublicacao) {
}
