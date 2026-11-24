package br.com.ongs.atividadeongs.config;

import br.com.ongs.atividadeongs.models.Animal;
import br.com.ongs.atividadeongs.models.AnimalAtualizacao;
import br.com.ongs.atividadeongs.models.Doacao;
import br.com.ongs.atividadeongs.models.MotivoInativacao;
import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.StatusAnimal;
import br.com.ongs.atividadeongs.models.StatusDoacao;
import br.com.ongs.atividadeongs.models.TipoChavePix;
import br.com.ongs.atividadeongs.models.Usuario;
import br.com.ongs.atividadeongs.repository.AnimalAtualizacaoRepository;
import br.com.ongs.atividadeongs.repository.AnimalRepository;
import br.com.ongs.atividadeongs.repository.DoacaoRepository;
import br.com.ongs.atividadeongs.repository.OngRepository;
import br.com.ongs.atividadeongs.repository.UsuarioRepository;
import br.com.ongs.atividadeongs.service.SenhaService;
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
    private static final String PADRINHO_EMAIL_ANTIGO = "carlos.andrade@email.com";
    private static final String ADM_EMAIL = "ong@example.com";
    private static final String DOADOR_EMAIL = "doador@example.com";
    private static final String ABRIGO_EMAIL = "abrigo@example.com";
    private static final String PROTETORA_EMAIL = "maria@example.com";

    // Senha FICTÍCIA, igual para todas as contas de exemplo (só para demonstração).
    private static final String SENHA_EXEMPLO = "exemplo123";

    private final OngRepository ongRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnimalRepository animalRepository;
    private final DoacaoRepository doacaoRepository;
    private final SenhaService senhaService;
    private final AnimalAtualizacaoRepository atualizacaoRepository;

    public DataInitializer(OngRepository ongRepository,
                           UsuarioRepository usuarioRepository,
                           AnimalRepository animalRepository,
                           DoacaoRepository doacaoRepository,
                           SenhaService senhaService,
                           AnimalAtualizacaoRepository atualizacaoRepository) {
        this.ongRepository = ongRepository;
        this.usuarioRepository = usuarioRepository;
        this.animalRepository = animalRepository;
        this.doacaoRepository = doacaoRepository;
        this.senhaService = senhaService;
        this.atualizacaoRepository = atualizacaoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Ong patasFelizes = criarOngPadraoSeNecessario();
        criarPadrinhoPadraoSeNecessario();
        criarAnimaisDeExemploSeNecessario(patasFelizes);
        vincularAnimaisSemOng(patasFelizes);
        criarOngsExtrasDeExemplo();
        garantirContasDeExemplo(patasFelizes);
        preencherDatasDeCadastro();
        criarAtualizacoesDeExemploSeNecessario();
    }

    /** Novidades fictícias em alguns animais de exemplo. Só são criadas se ainda não houver nenhuma. */
    private void criarAtualizacoesDeExemploSeNecessario() {
        if (atualizacaoRepository.count() > 0) {
            return;
        }

        atualizar("Thor", 12, "Thor fez a cirurgia na pata e foi muito bem! Agora ele descansa e começa a fisioterapia na semana que vem.");
        atualizar("Thor", 4, "Primeira sessão de fisioterapia concluída. Ele já apoia a pata e está de bom humor.");
        atualizar("Mel", 9, "Mel tomou o tratamento contra carrapatos e está brincando bastante com a bolinha.");
        atualizar("Frajola", 6, "O ouvido do Frajola está melhorando com o antibiótico. Ele continua muito falante.");
        atualizar("Caramelo", 3, "Caramelo ganhou uma caminha nova e já está andando sem dificuldade.");
    }

    private void atualizar(String nomeDoAnimal, int diasAtras, String texto) {
        animalRepository.findAll().stream()
                .filter(animal -> nomeDoAnimal.equals(animal.getNome()))
                .findFirst()
                .ifPresent(animal -> {
                    AnimalAtualizacao atualizacao = new AnimalAtualizacao();
                    atualizacao.setAnimal(animal);
                    atualizacao.setTexto(texto);
                    atualizacao.setDataPublicacao(Instant.now().minus(diasAtras, ChronoUnit.DAYS));
                    atualizacaoRepository.save(atualizacao);
                });
    }

    /**
     * Animais cadastrados antes de existir a data de publicação ficam sem data.
     * Como a data real não é conhecida, eles recebem a data de hoje, uma única vez.
     */
    private void preencherDatasDeCadastro() {
        Instant agora = Instant.now();
        for (Animal animal : animalRepository.findAll()) {
            if (animal.getDataCadastro() == null) {
                animal.setDataCadastro(agora);
                animalRepository.save(animal);
            }
        }
    }

    private Ong criarOngPadraoSeNecessario() {
        Ong existente = ongRepository.findFirstByOrderByIdAsc().orElse(null);
        if (existente != null) {
            return existente;
        }

        Ong ong = new Ong();
        ong.setNome("Patas Felizes");
        ong.setDescricao("ONG fictícia dedicada ao resgate, tratamento e adoção de animais abandonados.");
        ong.setChavePix("ong.exemplo@example.com");
        ong.setTipoChavePix(TipoChavePix.EMAIL);
        ong.setTelefone("(00) 00000-0000");
        ong.setEndereco("Rua dos Bichinhos, 120 - Cidade Exemplo");
        ong = ongRepository.save(ong);

        log.info("ONG padrão criada para o protótipo.");
        return ong;
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

    /**
     * Garante as contas fictícias para entrar no site (ONG, doador e padrinho) e dá a
     * senha de exemplo às contas antigas que foram criadas antes de existir login.
     */
    private void garantirContasDeExemplo(Ong patasFelizes) {
        usuarioRepository.findByEmailIgnoreCase(PADRINHO_EMAIL_ANTIGO).ifPresent(antigo -> {
            if (usuarioRepository.findByEmailIgnoreCase(PADRINHO_EMAIL).isEmpty()) {
                antigo.setEmail(PADRINHO_EMAIL);
                usuarioRepository.save(antigo);
            }
        });

        contaDeExemplo("Responsável pela Patas Felizes", ADM_EMAIL, PerfilUsuario.ADM, patasFelizes);
        contaDeExemplo("Doador Exemplo", DOADOR_EMAIL, PerfilUsuario.USUARIO, null);

        for (Usuario usuario : usuarioRepository.findAll()) {
            if (usuario.getSenhaHash() == null) {
                usuario.setSenhaHash(senhaService.gerarHash(SENHA_EXEMPLO));
                usuarioRepository.save(usuario);
                log.info("Senha de exemplo definida para a conta {}.", usuario.getEmail());
            }
        }
    }

    /** Cria a conta de exemplo se não existir e, se faltar, liga a conta à sua ONG. */
    private Usuario contaDeExemplo(String nome, String email, PerfilUsuario perfil, Ong ong) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);

        if (usuario == null) {
            usuario = new Usuario();
            usuario.setNome(nome);
            usuario.setEmail(email);
            usuario.setPerfil(perfil);
            usuario.setSenhaHash(senhaService.gerarHash(SENHA_EXEMPLO));
            usuario.setOng(ong);
            usuario = usuarioRepository.save(usuario);
            log.info("Conta de exemplo criada: {}.", email);
        } else if (ong != null && usuario.getOng() == null) {
            usuario.setOng(ong);
            usuario = usuarioRepository.save(usuario);
        }
        return usuario;
    }

    /** Animais criados antes de existirem várias ONGs passam a pertencer à ONG padrão. */
    private void vincularAnimaisSemOng(Ong patasFelizes) {
        for (Animal animal : animalRepository.findAll()) {
            if (animal.getOng() == null) {
                animal.setOng(patasFelizes);
                animalRepository.save(animal);
            }
        }
    }

    /**
     * Cria duas campanhas fictícias extras (um abrigo e uma protetora independente)
     * para mostrar o site com várias ONGs. Só roda se as contas ainda não existirem.
     */
    private void criarOngsExtrasDeExemplo() {
        if (usuarioRepository.findByEmailIgnoreCase(ABRIGO_EMAIL).isEmpty()) {
            Ong abrigo = criarOng("Abrigo Cantinho do Gato",
                    "Abrigo fictício que acolhe gatos resgatados das ruas e cuida deles até a adoção.",
                    "pix.cantinho@example.com", TipoChavePix.EMAIL,
                    "(00) 00000-0001", "Rua das Mangueiras, 45 - Cidade Exemplo");
            contaDeExemplo("Responsável pelo Cantinho do Gato", ABRIGO_EMAIL, PerfilUsuario.ADM, abrigo);

            Animal frajola = criarAnimal(abrigo, "Frajola", "Gato", "3 anos",
                    "Frajola é um gato preto e branco, muito falante. Foi resgatado com uma infecção no ouvido e está em tratamento.",
                    "800.00", "35.00", StatusAnimal.ATIVO, "img/animais/frajola.svg",
                    List.of("Antibióticos", "Consultas veterinárias"));
            frajola.setUrgente(true);
            animalRepository.save(frajola);

            Animal mingau = criarAnimal(abrigo, "Mingau", "Gato", "8 meses",
                    "Mingau é um filhote branquinho e brincalhão, encontrado em uma caixa na chuva. Precisa de vacinas e castração.",
                    "650.00", "30.00", StatusAnimal.ATIVO, "img/animais/mingau.svg",
                    List.of("Vacinas", "Castração", "Ração para filhotes"));
            Animal tigrado = criarAnimal(abrigo, "Tigrado", "Gato", "6 anos",
                    "Tigrado é um senhor tranquilo, que gosta de dormir ao sol. Precisa de ração especial para os rins.",
                    "1200.00", "50.00", StatusAnimal.ATIVO, "img/animais/tigrado.svg",
                    List.of("Ração renal", "Exames de rotina"));

            doar(frajola, PerfilUsuario.USUARIO, null, "Letícia Fonseca", "120.00", 4);
            doar(frajola, PerfilUsuario.USUARIO, null, "Marcos Vieira", "60.00", 18);
            doar(mingau, PerfilUsuario.USUARIO, null, "Natália Cruz", "90.00", 7);
            doar(tigrado, PerfilUsuario.USUARIO, null, "Otávio Ramos", "200.00", 25);
        }

        if (usuarioRepository.findByEmailIgnoreCase(PROTETORA_EMAIL).isEmpty()) {
            Ong protetora = criarOng("Maria Souza, protetora independente",
                    "Protetora independente fictícia que cuida de cães resgatados em sua casa, com a ajuda de vizinhos.",
                    "(00) 90000-0002", TipoChavePix.TELEFONE,
                    "(00) 90000-0002", "Bairro Jardim Exemplo");
            contaDeExemplo("Maria Souza", PROTETORA_EMAIL, PerfilUsuario.ADM, protetora);

            Animal caramelo = criarAnimal(protetora, "Caramelo", "Cachorro", "2 anos",
                    "Caramelo foi atropelado e salvo por vizinhos. Já se recuperou da cirurgia e agora precisa de fisioterapia e ração.",
                    "950.00", "40.00", StatusAnimal.ATIVO, "img/animais/caramelo.svg",
                    List.of("Fisioterapia", "Ração"));
            Animal fred = criarAnimal(protetora, "Fred", "Cachorro", "4 anos",
                    "Fred é um cachorro de porte médio, muito leal. Precisa tratar uma sarna e fazer a castração.",
                    "700.00", "30.00", StatusAnimal.ATIVO, "img/animais/fred.svg",
                    List.of("Tratamento de pele", "Castração"));
            Animal pretinha = criarAnimal(protetora, "Pretinha", "Cachorro", "10 anos",
                    "Pretinha é uma senhorinha doce, que vive com a protetora desde filhote. Precisa de remédios para as articulações.",
                    "1000.00", "45.00", StatusAnimal.ATIVO, "img/animais/pretinha.svg",
                    List.of("Anti-inflamatórios", "Suplementos"));

            doar(caramelo, PerfilUsuario.USUARIO, null, "Paula Teixeira", "150.00", 3);
            doar(caramelo, PerfilUsuario.USUARIO, null, "Rafael Duarte", "80.00", 21);
            doar(fred, PerfilUsuario.USUARIO, null, "Sara Medeiros", "100.00", 11);
            doar(pretinha, PerfilUsuario.USUARIO, null, "Tiago Barros", "300.00", 30);
        }
    }

    private Ong criarOng(String nome, String descricao, String chavePix, TipoChavePix tipo,
                         String telefone, String endereco) {
        Ong ong = new Ong();
        ong.setNome(nome);
        ong.setDescricao(descricao);
        ong.setChavePix(chavePix);
        ong.setTipoChavePix(tipo);
        ong.setTelefone(telefone);
        ong.setEndereco(endereco);
        return ongRepository.save(ong);
    }

    private void criarAnimaisDeExemploSeNecessario(Ong patasFelizes) {
        if (animalRepository.count() > 0) {
            return;
        }

        Usuario padrinho = usuarioRepository.findByPerfil(PerfilUsuario.PADRINHO).stream()
                .findFirst()
                .orElse(null);

        Animal mel = criarAnimal(patasFelizes, "Mel", "Cachorro", "3 anos",
                "Mel foi resgatada de uma casa abandonada. É carinhosa, adora brincar de bolinha e se dá bem com crianças. Precisa de tratamento contra carrapatos e de castração.",
                "1500.00", "60.00", StatusAnimal.ATIVO, "img/animais/mel.svg",
                List.of("Ração", "Tratamento contra carrapatos", "Castração"));

        Animal thor = criarAnimal(patasFelizes, "Thor", "Cachorro", "5 anos",
                "Thor é um cachorro grande e muito dócil, encontrado vagando perto de uma estrada. Está se recuperando de uma cirurgia na pata e precisa de fisioterapia.",
                "2200.00", "80.00", StatusAnimal.ATIVO, "img/animais/thor.svg",
                List.of("Fisioterapia", "Medicamentos", "Ração"));

        thor.setUrgente(true);
        animalRepository.save(thor);

        Animal luna = criarAnimal(patasFelizes, "Luna", "Gato", "2 anos",
                "Luna é uma gata tímida e muito esperta. Foi encontrada em uma caixa de papelão e já está vacinada. Procura um lar tranquilo e padrinhos para ajudar nos cuidados.",
                "900.00", "40.00", StatusAnimal.ATIVO, "img/animais/luna.svg",
                List.of("Ração", "Areia higiênica", "Vermifugação"));

        Animal mimi = criarAnimal(patasFelizes, "Mimi", "Gato", "1 ano",
                "Mimi é uma gatinha brincalhona e cheia de energia. Precisa fazer exames e castração antes de ser encaminhada para adoção.",
                "700.00", "35.00", StatusAnimal.ATIVO, "img/animais/mimi.svg",
                List.of("Exames", "Castração"));

        Animal bob = criarAnimal(patasFelizes, "Bob", "Cachorro", "8 anos",
                "Bob é um senhorzinho calmo e companheiro. Já conseguiu o dinheiro do tratamento dentário e agora aguarda um novo lar.",
                "600.00", "50.00", StatusAnimal.INATIVO, "img/animais/bob.svg",
                List.of("Tratamento dentário"));

        bob.setMotivoInativacao(MotivoInativacao.META_ATINGIDA);
        animalRepository.save(bob);

        Animal nina = criarAnimal(patasFelizes, "Nina", "Cachorro", "6 meses",
                "Nina é uma filhote curiosa, resgatada junto com os irmãos. Precisa completar o ciclo de vacinas e de ração para filhotes.",
                "1100.00", "45.00", StatusAnimal.ATIVO, "img/animais/nina.svg",
                List.of("Vacinas", "Ração para filhotes", "Vermífugo"));

        Animal simba = criarAnimal(patasFelizes, "Simba", "Gato", "4 anos",
                "Simba é um gato laranja, muito sociável, que vivia em uma praça. Precisa de tratamento para uma alergia de pele.",
                "1300.00", "55.00", StatusAnimal.ATIVO, "img/animais/simba.svg",
                List.of("Consultas veterinárias", "Medicamentos", "Ração"));

        Animal pipoca = criarAnimal(patasFelizes, "Pipoca", "Coelho", "1 ano",
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

    private Animal criarAnimal(Ong ong, String nome, String especie, String idade, String descricao,
                               String meta, String mensalidade, StatusAnimal status,
                               String fotoUrl, List<String> necessidades) {
        Animal animal = new Animal();
        animal.setOng(ong);
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
