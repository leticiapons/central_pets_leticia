package br.com.ongs.atividadeongs.dto;

import br.com.ongs.atividadeongs.models.MotivoInativacao;
import br.com.ongs.atividadeongs.models.StatusAnimal;

public record AnimalStatusRequest(StatusAnimal status, MotivoInativacao motivo) {
}
