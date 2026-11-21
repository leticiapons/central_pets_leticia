package br.com.ongs.atividadeongs.service;

import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.Usuario;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda as sessões de quem entrou no site. Para o protótipo, as sessões ficam
 * na memória do servidor: se o back-end for reiniciado, todos precisam entrar de novo.
 */
@Service
public class SessaoService {

    private static final Duration VALIDADE = Duration.ofHours(8);
    private static final String PREFIXO = "Bearer ";

    public record Sessao(Long usuarioId, String nome, String email, PerfilUsuario perfil, Instant expiraEm) {
    }

    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();

    public String criar(Usuario usuario) {
        Instant agora = Instant.now();
        sessoes.values().removeIf(sessao -> sessao.expiraEm().isBefore(agora));

        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        sessoes.put(token, new Sessao(
                usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), agora.plus(VALIDADE)));
        return token;
    }

    public Optional<Sessao> buscar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Sessao sessao = sessoes.get(token);
        if (sessao == null) {
            return Optional.empty();
        }
        if (sessao.expiraEm().isBefore(Instant.now())) {
            sessoes.remove(token);
            return Optional.empty();
        }
        return Optional.of(sessao);
    }

    public void encerrar(String token) {
        if (token != null) {
            sessoes.remove(token);
        }
    }

    /** Extrai o token do cabeçalho "Authorization: Bearer ...". */
    public static String extrairToken(String cabecalho) {
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            return null;
        }
        String token = cabecalho.substring(PREFIXO.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
