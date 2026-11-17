package br.com.ongs.atividadeongs.config;

import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.TipoChavePix;
import br.com.ongs.atividadeongs.models.Usuario;
import br.com.ongs.atividadeongs.repository.OngRepository;
import br.com.ongs.atividadeongs.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garante o mínimo necessário para o MVP funcionar em um banco vazio:
 * uma ONG e um padrinho. Não cria animais nem vínculos de apadrinhamento,
 * que são gerenciados pela aplicação.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String PADRINHO_EMAIL = "carlos.andrade@email.com";

    private final OngRepository ongRepository;
    private final UsuarioRepository usuarioRepository;

    public DataInitializer(OngRepository ongRepository, UsuarioRepository usuarioRepository) {
        this.ongRepository = ongRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        criarOngPadraoSeNecessario();
        criarPadrinhoPadraoSeNecessario();
    }

    private void criarOngPadraoSeNecessario() {
        if (ongRepository.findFirstByOrderByIdAsc().isPresent()) {
            return;
        }

        Ong ong = new Ong();
        ong.setNome("Patas Felizes");
        ong.setDescricao("ONG dedicada ao resgate, tratamento e adoção de animais abandonados.");
        ong.setChavePix("contato@patasfelizes.org.br");
        ong.setTipoChavePix(TipoChavePix.EMAIL);
        ong.setTelefone("(11) 99999-0000");
        ong.setEndereco("Rua dos Bichinhos, 120 - São Paulo/SP");
        ongRepository.save(ong);

        log.info("ONG padrão criada para o MVP.");
    }

    private void criarPadrinhoPadraoSeNecessario() {
        if (!usuarioRepository.findByPerfil(PerfilUsuario.PADRINHO).isEmpty()) {
            return;
        }

        Usuario padrinho = usuarioRepository.findByEmailIgnoreCase(PADRINHO_EMAIL)
                .orElseGet(Usuario::new);

        if (padrinho.getId() == null) {
            padrinho.setNome("Carlos Andrade");
            padrinho.setEmail(PADRINHO_EMAIL);
        }
        padrinho.setPerfil(PerfilUsuario.PADRINHO);
        usuarioRepository.save(padrinho);

        log.info("Padrinho padrão criado para o MVP.");
    }
}
