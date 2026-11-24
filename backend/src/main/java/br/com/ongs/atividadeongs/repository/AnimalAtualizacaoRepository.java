package br.com.ongs.atividadeongs.repository;

import br.com.ongs.atividadeongs.models.AnimalAtualizacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnimalAtualizacaoRepository extends JpaRepository<AnimalAtualizacao, Long> {

    List<AnimalAtualizacao> findByAnimalIdOrderByDataPublicacaoDescIdDesc(Long animalId);
}
