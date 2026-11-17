package br.com.ongs.atividadeongs.service;

import br.com.ongs.atividadeongs.dto.AnimalRequest;
import br.com.ongs.atividadeongs.dto.AnimalResponse;
import br.com.ongs.atividadeongs.exception.RecursoNaoEncontradoException;
import br.com.ongs.atividadeongs.exception.RegraNegocioException;
import br.com.ongs.atividadeongs.models.Animal;
import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.StatusAnimal;
import br.com.ongs.atividadeongs.models.StatusDoacao;
import br.com.ongs.atividadeongs.models.Usuario;
import br.com.ongs.atividadeongs.repository.AnimalRepository;
import br.com.ongs.atividadeongs.repository.DoacaoRepository;
import br.com.ongs.atividadeongs.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AnimalService {

    private static final int TAMANHO_MAXIMO_NOME = 60;
    private static final int TAMANHO_MAXIMO_ESPECIE = 40;
    private static final int TAMANHO_MAXIMO_IDADE = 30;
    private static final int TAMANHO_MAXIMO_NECESSIDADE = 120;

    private final AnimalRepository animalRepository;
    private final DoacaoRepository doacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public AnimalService(AnimalRepository animalRepository,
                         DoacaoRepository doacaoRepository,
                         UsuarioRepository usuarioRepository) {
        this.animalRepository = animalRepository;
        this.doacaoRepository = doacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> listar() {
        return animalRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AnimalResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public AnimalResponse criar(AnimalRequest req) {
        String nome = textoObrigatorio(req.nome(), "nome", TAMANHO_MAXIMO_NOME);
        String especie = textoObrigatorio(req.especie(), "espécie", TAMANHO_MAXIMO_ESPECIE);
        String descricao = textoObrigatorio(req.descricao(), "descrição", null);

        if (req.meta() == null || req.meta().signum() <= 0) {
            throw new IllegalArgumentException("A meta deve ser maior que zero.");
        }
        if (req.mensalidadeSugerida() != null && req.mensalidadeSugerida().signum() < 0) {
            throw new IllegalArgumentException("A mensalidade sugerida não pode ser negativa.");
        }

        String idade = req.idade() == null ? null : req.idade().trim();
        if (idade != null && idade.length() > TAMANHO_MAXIMO_IDADE) {
            throw new IllegalArgumentException("A idade deve ter até " + TAMANHO_MAXIMO_IDADE + " caracteres.");
        }

        Animal animal = new Animal();
        animal.setNome(nome);
        animal.setEspecie(especie);
        animal.setIdade(idade);
        animal.setDescricao(descricao);
        animal.setMeta(req.meta());
        animal.setMensalidadeSugerida(req.mensalidadeSugerida());
        animal.setNecessidades(validarNecessidades(req.necessidades()));
        if (req.status() != null) {
            animal.setStatus(req.status());
        }
        return toResponse(animalRepository.save(animal));
    }

    @Transactional
    public AnimalResponse atualizarStatus(Long id, StatusAnimal novoStatus) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("O status é obrigatório.");
        }
        Animal animal = buscarEntidade(id);
        if (novoStatus == StatusAnimal.INATIVO && !calcularMetaAtingida(animal)) {
            throw new RegraNegocioException("Só é possível inativar o animal quando a meta for atingida.");
        }
        animal.setStatus(novoStatus);
        return toResponse(animal);
    }

    /**
     * Cria o vínculo de apadrinhamento entre um animal e um usuário com perfil PADRINHO.
     * A operação é idempotente: apadrinhar um animal já vinculado não duplica o registro.
     */
    @Transactional
    public AnimalResponse adicionarPadrinho(Long animalId, Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("Informe o padrinho.");
        }

        Animal animal = buscarEntidade(animalId);
        if (animal.getStatus() != StatusAnimal.ATIVO) {
            throw new RegraNegocioException("Não é possível apadrinhar um animal inativo.");
        }

        Usuario padrinho = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Padrinho não encontrado."));

        if (padrinho.getPerfil() != PerfilUsuario.PADRINHO) {
            throw new IllegalArgumentException("O usuário informado não possui perfil de padrinho.");
        }

        boolean jaVinculado = animal.getPadrinhos().stream()
                .anyMatch(p -> Objects.equals(p.getId(), padrinho.getId()));

        if (!jaVinculado) {
            animal.getPadrinhos().add(padrinho);
            animalRepository.save(animal);
        }

        return toResponse(animal);
    }

    /**
     * Remove o vínculo de apadrinhamento. A operação é idempotente.
     */
    @Transactional
    public AnimalResponse removerPadrinho(Long animalId, Long usuarioId) {
        Animal animal = buscarEntidade(animalId);

        boolean removido = animal.getPadrinhos()
                .removeIf(p -> Objects.equals(p.getId(), usuarioId));

        if (removido) {
            animalRepository.save(animal);
        }

        return toResponse(animal);
    }

    private Animal buscarEntidade(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Animal não encontrado."));
    }

    private BigDecimal arrecadado(Animal a) {
        return doacaoRepository.somarValorPorAnimal(a.getId(), StatusDoacao.CANCELADA);
    }

    private boolean calcularMetaAtingida(Animal a) {
        BigDecimal meta = a.getMeta();
        return meta != null && meta.signum() > 0 && arrecadado(a).compareTo(meta) >= 0;
    }

    private String textoObrigatorio(String valor, String campo, Integer tamanhoMaximo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório.");
        }
        if (tamanhoMaximo != null && texto.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(
                    "O campo '" + campo + "' deve ter até " + tamanhoMaximo + " caracteres.");
        }
        return texto;
    }

    private List<String> validarNecessidades(List<String> necessidades) {
        List<String> normalizadas = new ArrayList<>();

        if (necessidades == null) {
            return normalizadas;
        }

        for (String necessidade : necessidades) {
            String texto = necessidade == null ? "" : necessidade.trim();
            if (texto.isEmpty()) {
                continue;
            }
            if (texto.length() > TAMANHO_MAXIMO_NECESSIDADE) {
                throw new IllegalArgumentException(
                        "Cada necessidade deve ter até " + TAMANHO_MAXIMO_NECESSIDADE + " caracteres.");
            }
            normalizadas.add(texto);
        }

        return normalizadas;
    }

    private AnimalResponse toResponse(Animal a) {
        BigDecimal arrecadado = arrecadado(a);
        BigDecimal meta = a.getMeta();
        boolean temMeta = meta != null && meta.signum() > 0;
        boolean atingida = temMeta && arrecadado.compareTo(meta) >= 0;
        int percentual = temMeta
                ? arrecadado.multiply(BigDecimal.valueOf(100))
                .divide(meta, 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100)).intValue()
                : 0;

        List<Long> padrinhoIds = a.getPadrinhos().stream().map(Usuario::getId).toList();

        return new AnimalResponse(
                a.getId(), a.getNome(), a.getEspecie(), a.getIdade(), a.getDescricao(),
                a.getStatus(), meta, new ArrayList<>(a.getNecessidades()),
                padrinhoIds, a.getMensalidadeSugerida(),
                arrecadado, percentual, atingida);
    }
}
