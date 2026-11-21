package br.com.ongs.atividadeongs.service;

import br.com.ongs.atividadeongs.dto.AuthResponse;
import br.com.ongs.atividadeongs.dto.CadastroRequest;
import br.com.ongs.atividadeongs.dto.LoginRequest;
import br.com.ongs.atividadeongs.dto.UsuarioLogadoResponse;
import br.com.ongs.atividadeongs.exception.AcessoNegadoException;
import br.com.ongs.atividadeongs.exception.NaoAutorizadoException;
import br.com.ongs.atividadeongs.exception.RegraNegocioException;
import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.Usuario;
import br.com.ongs.atividadeongs.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int TAMANHO_MINIMO_SENHA = 6;
    private static final int TAMANHO_MAXIMO_SENHA = 72;
    private static final String MENSAGEM_LOGIN_INVALIDO = "E-mail ou senha incorretos.";

    private final UsuarioRepository usuarioRepository;
    private final SenhaService senhaService;
    private final SessaoService sessaoService;
    private final OngService ongService;

    public AuthService(UsuarioRepository usuarioRepository,
                       SenhaService senhaService,
                       SessaoService sessaoService,
                       OngService ongService) {
        this.usuarioRepository = usuarioRepository;
        this.senhaService = senhaService;
        this.sessaoService = sessaoService;
        this.ongService = ongService;
    }

    @Transactional
    public AuthResponse cadastrar(CadastroRequest req) {
        String nome = req.nome() == null ? "" : req.nome().trim();
        String email = normalizarEmail(req.email());
        String senha = req.senha() == null ? "" : req.senha();

        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Informe o seu nome.");
        }
        if (nome.length() > 120) {
            throw new IllegalArgumentException("O nome deve ter até 120 caracteres.");
        }
        if (email.isEmpty() || email.length() > 160 || !EMAIL_VALIDO.matcher(email).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA || senha.length() > TAMANHO_MAXIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A senha deve ter de " + TAMANHO_MINIMO_SENHA + " a " + TAMANHO_MAXIMO_SENHA + " caracteres.");
        }

        PerfilUsuario perfil = req.perfil() == null ? PerfilUsuario.USUARIO : req.perfil();

        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);

        // ONG, protetor independente ou pessoa pedindo ajuda: cadastram também a própria campanha.
        if (perfil == PerfilUsuario.ADM) {
            if (req.ong() == null) {
                throw new IllegalArgumentException("Informe os dados da ONG ou do protetor.");
            }
            usuario.setOng(ongService.criar(req.ong()));
        }

        usuario.setSenhaHash(senhaService.gerarHash(senha));
        usuario = usuarioRepository.save(usuario);

        return respostaDeAutenticacao(usuario);
    }

    @Transactional(readOnly = true)
    public AuthResponse entrar(LoginRequest req) {
        String email = normalizarEmail(req.email());
        String senha = req.senha() == null ? "" : req.senha();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NaoAutorizadoException(MENSAGEM_LOGIN_INVALIDO));

        if (!senhaService.conferir(senha, usuario.getSenhaHash())) {
            throw new NaoAutorizadoException(MENSAGEM_LOGIN_INVALIDO);
        }

        return respostaDeAutenticacao(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioLogadoResponse usuarioDoToken(String autorizacao) {
        return UsuarioLogadoResponse.de(exigirUsuario(autorizacao, null));
    }

    public void sair(String autorizacao) {
        sessaoService.encerrar(SessaoService.extrairToken(autorizacao));
    }

    /**
     * Descobre quem está fazendo o pedido a partir do cabeçalho Authorization.
     * Se perfilExigido não for nulo, só aceita usuários desse perfil.
     */
    @Transactional(readOnly = true)
    public Usuario exigirUsuario(String autorizacao, PerfilUsuario perfilExigido) {
        String token = SessaoService.extrairToken(autorizacao);
        SessaoService.Sessao sessao = sessaoService.buscar(token)
                .orElseThrow(() -> new NaoAutorizadoException("Sessão expirada. Entre novamente."));

        Usuario usuario = usuarioRepository.findById(sessao.usuarioId())
                .orElseThrow(() -> new NaoAutorizadoException("Sessão inválida. Entre novamente."));

        if (perfilExigido != null && usuario.getPerfil() != perfilExigido) {
            throw new AcessoNegadoException("Você não tem permissão para esta ação.");
        }
        return usuario;
    }

    /** Exige uma conta de ONG/protetor (perfil ADM) e devolve a ONG que ela gerencia. */
    @Transactional(readOnly = true)
    public Ong exigirOngDoUsuario(String autorizacao) {
        Usuario usuario = exigirUsuario(autorizacao, PerfilUsuario.ADM);
        if (usuario.getOng() == null) {
            throw new RegraNegocioException("Sua conta não está ligada a uma ONG ou protetor.");
        }
        return usuario.getOng();
    }

    private AuthResponse respostaDeAutenticacao(Usuario usuario) {
        String token = sessaoService.criar(usuario);
        return new AuthResponse(token, UsuarioLogadoResponse.de(usuario));
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
