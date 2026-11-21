package br.com.ongs.atividadeongs.config;

import br.com.ongs.atividadeongs.models.Animal;
import br.com.ongs.atividadeongs.models.Doacao;
import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.StatusAnimal;
import br.com.ongs.atividadeongs.models.StatusDoacao;
import br.com.ongs.atividadeongs.models.TipoChavePix;
import br.com.ongs.atividadeongs.models.Usuario;
import br.com.ongs.atividadeongs.repository.AnimalRepository;
import br.com.ongs.atividadeongs.repository.DoacaoRepository;
import br.com.ongs.atividadeongs.repository.OngRepository;
import br.com.ongs.atividadeongs.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Garante o mínimo necessário para o protótipo funcionar em um banco vazio:
 * uma ONG, um padrinho e alguns animais e doações FICTÍCIOS para demonstração.
 * Todos os nomes, e-mails, chaves PIX e valores abaixo são inventados.
 * Os animais de exemplo só são criados se ainda não existir nenhum animal no banco.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String PADRINHO_EMAIL = "carlos.andrade@example.com";

    private final OngRepository ongRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnimalRepository animalRepository;
    private final DoacaoRepository doacaoRepository;

    public DataInitializer(OngRepository ongRepository,
                           UsuarioRepository usuarioRepository,
                           AnimalRepository animalRepository,
                           DoacaoRepository doacaoRepository) {
        this.ongRepository = ongRepository;
        this.usuarioRepository = usuarioRepository;
        this.animalRepository = animalRepository;
        this.doacaoRepository = doacaoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        criarOngPadraoSeNecessario();
        criarPadrinhoPadraoSeNecessario();
        criarAnimaisDeExemploSeNecessario();
    }

    private void criarOngPadraoSeNecessario() {
        if (ongRepository.findFirstByOrderByIdAsc().isPresent()) {
            return;
        }

        Ong ong = new Ong();
        ong.setNome("Patas Felizes");
        ong.setDescricao("ONG fictícia dedicada ao resgate, tratamento e adoção de animais abandonados.");
        ong.setChavePix("ong.exemplo@example.com");
        ong.setTipoChavePix(TipoChavePix.EMAIL);
        ong.setTelefone("(00) 00000-0000");
        ong.setEndereco("Rua dos Bichinhos, 120 - Cidade Exemplo");
        ongRepository.save(ong);

        log.info("ONG padrão criada para o protótipo.");
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

        log.info("Padrinho padrão criado para o protótipo.");
    }

    private void criarAnimaisDeExemploSeNecessario() {
        if (animalRepository.count() > 0) {
            return;
        }

        Usuario padrinho = usuarioRepository.findByPerfil(PerfilUsuario.PADRINHO).stream()
                .findFirst()
                .orElse(null);

        Animal mel = criarAnimal("Mel", "Cachorro", "3 anos",
                "Mel foi resgatada de uma casa abandonada. É carinhosa, adora brincar de bolinha e se dá bem com crianças. Precisa de tratamento contra carrapatos e de castração.",
                "1500.00", "60.00", StatusAnimal.ATIVO, "img/animais/mel.svg",
                List.of("Ração", "Tratamento contra carrapatos", "Castração"));

        Animal thor = criarAnimal("Thor", "Cachorro", "5 anos",
                "Thor é um cachorro grande e muito dócil, encontrado vagando perto de uma estrada. Está se recuperando de uma cirurgia na pata e precisa de fisioterapia.",
                "2200.00", "80.00", StatusAnimal.ATIVO, "img/animais/thor.svg",
                List.of("Fisioterapia", "Medicamentos", "Ração"));

        Animal luna = criarAnimal("Luna", "Gato", "2 anos",
                "Luna é uma gata tímida e muito esperta. Foi encontrada em uma caixa de papelão e já está vacinada. Procura um lar tranquilo e padrinhos para ajudar nos cuidados.",
                "900.00", "40.00", StatusAnimal.ATIVO, "img/animais/luna.svg",
                List.of("Ração", "Areia higiênica", "Vermifugação"));

        Animal mimi = criarAnimal("Mimi", "Gato", "1 ano",
                "Mimi é uma gatinha brincalhona e cheia de energia. Precisa fazer exames e castração antes de ser encaminhada para adoção.",
                "700.00", "35.00", StatusAnimal.ATIVO, "img/animais/mimi.svg",
                List.of("Exames", "Castração"));

        Animal bob = criarAnimal("Bob", "Cachorro", "8 anos",
                "Bob é um senhorzinho calmo e companheiro. Teve a meta de tratamento dentário atingida e está inativo por enquanto, aguardando um novo lar.",
                "600.00", "50.00", StatusAnimal.INATIVO, "img/animais/bob.svg",
                List.of("Tratamento dentário"));

        Animal nina = criarAnimal("Nina", "Cachorro", "6 meses",
                "Nina é uma filhote curiosa, resgatada junto com os irmãos. Precisa completar o ciclo de vacinas e de ração para filhotes.",
                "1100.00", "45.00", StatusAnimal.ATIVO, "img/animais/nina.svg",
                List.of("Vacinas", "Ração para filhotes", "Vermífugo"));

        Animal simba = criarAnimal("Simba", "Gato", "4 anos",
                "Simba é um gato laranja, muito sociável, que vivia em uma praça. Precisa de tratamento para uma alergia de pele.",
                "1300.00", "55.00", StatusAnimal.ATIVO, "img/animais/simba.svg",
                List.of("Consultas veterinárias", "Medicamentos", "Ração"));

        Animal pipoca = criarAnimal("Pipoca", "Coelho", "1 ano",
                "Pipoca é uma coelhinha mansa, entregue à ONG após o dono se mudar. Precisa de feno, verduras frescas e um recinto maior.",
                "500.00", "30.00", StatusAnimal.ATIVO, "img/animais/pipoca.svg",
                List.of("Feno", "Recinto maior", "Verduras"));

        // Doações fictícias espalhadas pelos últimos meses (úteis para testar filtros por período).
        doar(mel, PerfilUsuario.USUARIO, null, "Ana Souza", "100.00", 2);
        doar(mel, PerfilUsuario.USUARIO, null, "Bruno Lima", "50.00", 15);
        doar(mel, PerfilUsuario.USUARIO, null, "Doador anônimo", "200.00", 40);

        doar(thor, PerfilUsuario.USUARIO, null, "Carla Mendes", "300.00", 5);
        doar(thor, PerfilUsuario.USUARIO, null, "Diego Rocha", "150.00", 28);

        doar(luna, PerfilUsuario.USUARIO, null, "Elisa Prado", "80.00", 1);
        doar(luna, PerfilUsuario.USUARIO, null, "Fábio Nunes", "120.00", 33);

        doar(mimi, PerfilUsuario.USUARIO, null, "Gabriela Dias", "70.00", 9);

        doar(bob, PerfilUsuario.USUARIO, null, "Henrique Alves", "350.00", 60);
        doar(bob, PerfilUsuario.USUARIO, null, "Isabela Costa", "250.00", 75);

        doar(nina, PerfilUsuario.USUARIO, null, "João Pereira", "90.00", 12);
        doar(simba, PerfilUsuario.USUARIO, null, "Karen Martins", "200.00", 20);

        // Apadrinhamento de exemplo: o padrinho padrão apadrinha a Luna e o Thor.
        if (padrinho != null) {
            vincularPadrinho(luna, padrinho);
            vincularPadrinho(thor, padrinho);
            doar(luna, PerfilUsuario.PADRINHO, padrinho, padrinho.getNome(), "40.00", 3);
            doar(thor, PerfilUsuario.PADRINHO, padrinho, padrinho.getNome(), "80.00", 6);
        }

        log.info("Animais e doações fictícios criados para o protótipo.");
    }

    private Animal criarAnimal(String nome, String especie, String idade, String descricao,
                               String meta, String mensalidade, StatusAnimal status,
                               String fotoUrl, List<String> necessidades) {
        Animal animal = new Animal();
        animal.setNome(nome);
        animal.setEspecie(especie);
        animal.setIdade(idade);
        animal.setDescricao(descricao);
        animal.setMeta(new BigDecimal(meta));
        animal.setMensalidadeSugerida(new BigDecimal(mensalidade));
        animal.setStatus(status);
        animal.setFotoUrl(fotoUrl);
        animal.setNecessidades(new java.util.ArrayList<>(necessidades));
        return animalRepository.save(animal);
    }

    private void vincularPadrinho(Animal animal, Usuario padrinho) {
        animal.getPadrinhos().add(padrinho);
        animalRepository.save(animal);
    }

    private void doar(Animal animal, PerfilUsuario perfil, Usuario padrinho,
                      String doadorNome, String valor, int diasAtras) {
        Doacao doacao = new Doacao();
        doacao.setAnimal(animal);
        doacao.setPerfil(perfil);
        doacao.setPadrinho(padrinho);
        doacao.setDoadorNome(doadorNome);
        doacao.setValor(new BigDecimal(valor));
        doacao.setData(Instant.now().minus(diasAtras, ChronoUnit.DAYS));
        doacao.setStatus(StatusDoacao.DOADO);
        doacaoRepository.save(doacao);
    }
}
