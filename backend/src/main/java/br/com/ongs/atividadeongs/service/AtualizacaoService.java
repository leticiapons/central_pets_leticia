package br.com.ongs.atividadeongs.service;

import br.com.ongs.atividadeongs.dto.AtualizacaoRequest;
import br.com.ongs.atividadeongs.dto.AtualizacaoResponse;
import br.com.ongs.atividadeongs.exception.AcessoNegadoException;
import br.com.ongs.atividadeongs.exception.RecursoNaoEncontradoException;
import br.com.ongs.atividadeongs.models.Animal;
import br.com.ongs.atividadeongs.models.AnimalAtualizacao;
import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.repository.AnimalAtualizacaoRepository;
import br.com.ongs.atividadeongs.repository.AnimalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class AtualizacaoService {

    private static final int TAMANHO_MAXIMO_TEXTO = 1000;
    private static final int TAMANHO_MAXIMO_FOTO = 1_500_000;

    private final AnimalAtualizacaoRepository atualizacaoRepository;
    private final AnimalRepository animalRepository;

    public AtualizacaoService(AnimalAtualizacaoRepository atualizacaoRepository,
                              AnimalRepository animalRepository) {
        this.atualizacaoRepository = atualizacaoRepository;
        this.animalRepository = animalRepository;
    }

    /** Lista pública: qualquer pessoa pode acompanhar as novidades de um animal. */
    @Transactional(readOnly = true)
    public List<AtualizacaoResponse> listar(Long animalId) {
        buscarAnimal(animalId);
        return atualizacaoRepository.findByAnimalIdOrderByDataPublicacaoDescIdDesc(animalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AtualizacaoResponse publicar(Long animalId, AtualizacaoRequest dados, Ong ong) {
        Animal animal = buscarAnimal(animalId);
        exigirDono(animal, ong);

        String texto = dados.texto() == null ? "" : dados.texto().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Escreva o texto da atualização.");
        }
        if (texto.length() > TAMANHO_MAXIMO_TEXTO) {
            throw new IllegalArgumentException(
                    "O texto da atualização deve ter até " + TAMANHO_MAXIMO_TEXTO + " caracteres.");
        }

        AnimalAtualizacao atualizacao = new AnimalAtualizacao();
        atualizacao.setAnimal(animal);
        atualizacao.setTexto(texto);
        atualizacao.setFotoUrl(validarFoto(dados.fotoUrl()));

        return toResponse(atualizacaoRepository.save(atualizacao));
    }

    @Transactional
    public void excluir(Long animalId, Long atualizacaoId, Ong ong) {
        Animal animal = buscarAnimal(animalId);
        exigirDono(animal, ong);

        AnimalAtualizacao atualizacao = atualizacaoRepository.findById(atualizacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atualização não encontrada."));

        if (!Objects.equals(atualizacao.getAnimal().getId(), animal.getId())) {
            throw new RecursoNaoEncontradoException("Atualização não encontrada.");
        }

        atualizacaoRepository.delete(atualizacao);
    }

    private Animal buscarAnimal(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Animal não encontrado."));
    }

    private void exigirDono(Animal animal, Ong ong) {
        boolean dono = ong != null
                && animal.getOng() != null
                && Objects.equals(animal.getOng().getId(), ong.getId());
        if (!dono) {
            throw new AcessoNegadoException("Este animal pertence a outra ONG ou protetor.");
        }
    }

    private String validarFoto(String foto) {
        String texto = foto == null ? "" : foto.trim();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.length() > TAMANHO_MAXIMO_FOTO) {
            throw new IllegalArgumentException("A foto é grande demais. Escolha uma imagem menor.");
        }
        boolean formatoValido = texto.startsWith("data:image/")
                || texto.startsWith("http://")
                || texto.startsWith("https://")
                || texto.startsWith("img/");
        if (!formatoValido) {
            throw new IllegalArgumentException("A foto deve ser uma imagem válida.");
        }
        return texto;
    }

    private AtualizacaoResponse toResponse(AnimalAtualizacao a) {
        return new AtualizacaoResponse(
                a.getId(),
                a.getAnimal().getId(),
                a.getTexto(),
                a.getFotoUrl(),
                a.getDataPublicacao());
    }
}
