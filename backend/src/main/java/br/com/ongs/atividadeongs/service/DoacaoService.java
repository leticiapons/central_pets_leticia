package br.com.ongs.atividadeongs.service;

import br.com.ongs.atividadeongs.dto.DoacaoRequest;
import br.com.ongs.atividadeongs.dto.DoacaoResponse;
import br.com.ongs.atividadeongs.exception.AcessoNegadoException;
import br.com.ongs.atividadeongs.exception.RecursoNaoEncontradoException;
import br.com.ongs.atividadeongs.exception.RegraNegocioException;
import br.com.ongs.atividadeongs.models.*;
import br.com.ongs.atividadeongs.repository.AnimalRepository;
import br.com.ongs.atividadeongs.repository.DoacaoRepository;
import br.com.ongs.atividadeongs.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class DoacaoService {

    private final DoacaoRepository doacaoRepository;
    private final AnimalRepository animalRepository;
    private final UsuarioRepository usuarioRepository;

    public DoacaoService(
            DoacaoRepository doacaoRepository,
            AnimalRepository animalRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.doacaoRepository = doacaoRepository;
        this.animalRepository = animalRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<DoacaoResponse> listarPorOng(Long ongId) {
        return doacaoRepository.findAll()
                .stream()
                .filter(d -> d.getAnimal().getOng() != null
                        && Objects.equals(d.getAnimal().getOng().getId(), ongId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoacaoResponse> listarDoUsuario(Long usuarioId) {
        return doacaoRepository.findAll()
                .stream()
                .filter(d -> (d.getDoador() != null && Objects.equals(d.getDoador().getId(), usuarioId))
                        || (d.getPadrinho() != null && Objects.equals(d.getPadrinho().getId(), usuarioId)))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Registra uma doação feita por quem está logado. O perfil e o padrinho vêm da
     * conta de quem doa, e não do que o navegador enviou.
     */
    @Transactional
    public DoacaoResponse criar(Usuario doador, DoacaoRequest dados) {
        if (dados.animalId() == null) {
            throw new IllegalArgumentException("Informe o animal da doação.");
        }

        BigDecimal valor = dados.valor();

        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }

        if (valor.stripTrailingZeros().scale() > 2
                || valor.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Informe um valor válido com até duas casas decimais.");
        }

        Animal animal = animalRepository.findById(dados.animalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Animal não encontrado."));

        if (animal.getStatus() != StatusAnimal.ATIVO) {
            throw new RegraNegocioException("O animal está inativo.");
        }

        PerfilUsuario perfil = doador.getPerfil();
        Usuario quemDoou = usuarioRepository.findById(doador.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doador não encontrado."));
        Usuario padrinho = perfil == PerfilUsuario.PADRINHO ? quemDoou : null;

        String nome = dados.doadorNome();

        if (nome == null || nome.isBlank()) {
            nome = doador.getNome();
        } else {
            nome = nome.trim();
        }

        if (nome.length() > 120) {
            throw new IllegalArgumentException("O nome do doador deve ter até 120 caracteres.");
        }

        Doacao doacao = new Doacao();
        doacao.setAnimal(animal);
        doacao.setPerfil(perfil);
        doacao.setPadrinho(padrinho);
        doacao.setDoador(quemDoou);
        doacao.setDoadorNome(nome);
        doacao.setValor(valor);
        doacao.setData(Instant.now());
        doacao.setStatus(StatusDoacao.PENDENTE);

        // Uma doação feita como padrinho consolida o apadrinhamento do animal.
        if (padrinho != null) {
            Usuario padrinhoDaDoacao = padrinho;
            boolean jaVinculado = animal.getPadrinhos().stream()
                    .anyMatch(p -> Objects.equals(p.getId(), padrinhoDaDoacao.getId()));
            if (!jaVinculado) {
                animal.getPadrinhos().add(padrinho);
                animalRepository.save(animal);
            }
        }

        return toResponse(doacaoRepository.save(doacao));
    }

    /**
     * A ONG ou protetor dona do animal confirma (DOADO), recusa (CANCELADA) ou
     * desfaz (PENDENTE) uma doação recebida pelos seus animais.
     */
    @Transactional
    public DoacaoResponse atualizarStatus(Long id, StatusDoacao novoStatus, Ong ong) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("O status é obrigatório.");
        }

        Doacao doacao = doacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada."));

        Ong dona = doacao.getAnimal().getOng();
        if (ong == null || dona == null || !Objects.equals(dona.getId(), ong.getId())) {
            throw new AcessoNegadoException("Esta doação pertence a outra ONG ou protetor.");
        }

        doacao.setStatus(novoStatus);
        return toResponse(doacaoRepository.save(doacao));
    }

    private DoacaoResponse toResponse(Doacao doacao) {
        Long padrinhoId = doacao.getPadrinho() == null
                ? null
                : doacao.getPadrinho().getId();

        return new DoacaoResponse(
                doacao.getId(),
                doacao.getAnimal().getId(),
                doacao.getPerfil(),
                padrinhoId,
                doacao.getDoadorNome(),
                doacao.getValor(),
                doacao.getData(),
                doacao.getStatus()
        );
    }
}
