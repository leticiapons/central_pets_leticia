package br.com.ongs.atividadeongs.controller;

import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.Usuario;
import br.com.ongs.atividadeongs.repository.UsuarioRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Listar todos os usuarios cadastrados
    @GetMapping("/todos")
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    // Buscar um usuario pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Cadastrar um novo usuario
    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody Usuario usuario) {

        String nome = texto(usuario.getNome());
        String email = texto(usuario.getEmail());

        if (nome.isEmpty() || email.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensagem", "Nome e email são obrigatórios."));
        }
        if (nome.length() > 120) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensagem", "O nome deve ter até 120 caracteres."));
        }
        if (email.length() > 160) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensagem", "O email deve ter até 160 caracteres."));
        }

        // Verificar se o email ja esta cadastrado
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensagem", "Email já cadastrado."));
        }

        PerfilUsuario perfil = usuario.getPerfil();
        if (perfil == null) {
            perfil = PerfilUsuario.USUARIO;
        }
        if (perfil == PerfilUsuario.ADM) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensagem", "O perfil ADM não pode ser criado por este endpoint."));
        }

        // O banco gera o ID automaticamente
        usuario.setId(null);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);

        Usuario novoUsuario = usuarioRepository.save(usuario);

        return ResponseEntity
                .created(URI.create("/api/usuarios/" + novoUsuario.getId()))
                .body(novoUsuario);
    }

    // Contrato consumido pelo front-end: um único padrinho no MVP.
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar() {
        Map<String, Object> resposta = new LinkedHashMap<>();

        Optional<Usuario> padrinho = usuarioRepository.findByPerfil(PerfilUsuario.PADRINHO)
                .stream()
                .findFirst();

        padrinho.ifPresentOrElse(
                usuario -> resposta.put("padrinhoAtual", Map.of(
                        "id", usuario.getId(),
                        "nome", usuario.getNome(),
                        "email", usuario.getEmail()
                )),
                () -> resposta.put("padrinhoAtual", Map.of())
        );

        return ResponseEntity.ok(resposta);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
