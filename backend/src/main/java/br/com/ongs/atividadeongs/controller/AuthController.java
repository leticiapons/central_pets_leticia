package br.com.ongs.atividadeongs.controller;

import br.com.ongs.atividadeongs.dto.AuthResponse;
import br.com.ongs.atividadeongs.dto.CadastroRequest;
import br.com.ongs.atividadeongs.dto.LoginRequest;
import br.com.ongs.atividadeongs.dto.UsuarioLogadoResponse;
import br.com.ongs.atividadeongs.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<AuthResponse> cadastrar(@RequestBody CadastroRequest dados) {
        AuthResponse resposta = service.cadastrar(dados);
        return ResponseEntity.created(URI.create("/api/usuarios/" + resposta.usuario().id())).body(resposta);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> entrar(@RequestBody LoginRequest dados) {
        return ResponseEntity.ok(service.entrar(dados));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioLogadoResponse> eu(
            @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        return ResponseEntity.ok(service.usuarioDoToken(autorizacao));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> sair(
            @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        service.sair(autorizacao);
        return ResponseEntity.noContent().build();
    }
}
