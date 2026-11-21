package br.com.ongs.atividadeongs.service;

import br.com.ongs.atividadeongs.dto.OngRequest;
import br.com.ongs.atividadeongs.dto.OngResponse;
import br.com.ongs.atividadeongs.exception.RecursoNaoEncontradoException;
import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.models.TipoChavePix;
import br.com.ongs.atividadeongs.repository.OngRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Cadastro de ONGs, protetores independentes e pessoas pedindo ajuda por um animal.
 * Todos são tratados do mesmo jeito: cada um tem a sua "ONG" (nome, chave PIX e contato).
 */
@Service
public class OngService {

    private static final int TAMANHO_MAXIMO_NOME = 120;
    private static final int TAMANHO_MAXIMO_CHAVE_PIX = 160;
    private static final int TAMANHO_MAXIMO_TELEFONE = 30;
    private static final int TAMANHO_MAXIMO_ENDERECO = 255;

    private final OngRepository ongRepository;

    public OngService(OngRepository ongRepository) {
        this.ongRepository = ongRepository;
    }

    @Transactional(readOnly = true)
    public List<OngResponse> listar() {
        return ongRepository.findAll().stream()
                .sorted(Comparator.comparing(Ong::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(OngResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OngResponse buscar(Long id) {
        return OngResponse.from(buscarEntidade(id));
    }

    @Transactional
    public Ong criar(OngRequest dados) {
        Ong ong = new Ong();
        ong.setNome(obrigatorio(dados.nome(), "nome da ONG ou do protetor", TAMANHO_MAXIMO_NOME));
        ong.setChavePix(obrigatorio(dados.chavePix(), "chave PIX", TAMANHO_MAXIMO_CHAVE_PIX));

        TipoChavePix tipo = TipoChavePix.fromValor(dados.tipoChavePix());
        if (tipo == null) {
            throw new IllegalArgumentException("Informe o tipo da chave PIX.");
        }
        ong.setTipoChavePix(tipo);

        ong.setDescricao(dados.descricao() == null ? null : dados.descricao().trim());
        ong.setTelefone(opcional(dados.telefone(), "telefone", TAMANHO_MAXIMO_TELEFONE));
        ong.setEndereco(opcional(dados.endereco(), "endereço", TAMANHO_MAXIMO_ENDERECO));
        return ongRepository.save(ong);
    }

    /** Atualização parcial: campos nulos não são alterados. */
    @Transactional
    public OngResponse atualizar(Long ongId, OngRequest dados) {
        Ong ong = buscarEntidade(ongId);

        if (dados.nome() != null) {
            ong.setNome(obrigatorio(dados.nome(), "nome da ONG ou do protetor", TAMANHO_MAXIMO_NOME));
        }
        if (dados.chavePix() != null) {
            ong.setChavePix(obrigatorio(dados.chavePix(), "chave PIX", TAMANHO_MAXIMO_CHAVE_PIX));
        }
        if (dados.tipoChavePix() != null) {
            TipoChavePix tipo = TipoChavePix.fromValor(dados.tipoChavePix());
            if (tipo == null) {
                throw new IllegalArgumentException("Informe o tipo da chave PIX.");
            }
            ong.setTipoChavePix(tipo);
        }
        if (dados.descricao() != null) {
            ong.setDescricao(dados.descricao().trim());
        }
        if (dados.telefone() != null) {
            ong.setTelefone(opcional(dados.telefone(), "telefone", TAMANHO_MAXIMO_TELEFONE));
        }
        if (dados.endereco() != null) {
            ong.setEndereco(opcional(dados.endereco(), "endereço", TAMANHO_MAXIMO_ENDERECO));
        }

        return OngResponse.from(ongRepository.save(ong));
    }

    private Ong buscarEntidade(Long id) {
        return ongRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ONG ou protetor não encontrado."));
    }

    private String obrigatorio(String valor, String campo, int tamanhoMaximo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório.");
        }
        if (texto.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(
                    "O campo '" + campo + "' deve ter até " + tamanhoMaximo + " caracteres.");
        }
        return texto;
    }

    private String opcional(String valor, String campo, int tamanhoMaximo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.length() > tamanhoMaximo) {
            throw new IllegalArgumentException(
                    "O campo '" + campo + "' deve ter até " + tamanhoMaximo + " caracteres.");
        }
        return texto;
    }
}
