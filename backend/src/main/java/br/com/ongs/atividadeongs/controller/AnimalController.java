package br.com.ongs.atividadeongs.controller;

import br.com.ongs.atividadeongs.dto.AnimalRequest;
import br.com.ongs.atividadeongs.dto.AnimalResponse;
import br.com.ongs.atividadeongs.dto.AnimalStatusRequest;
import br.com.ongs.atividadeongs.dto.PadrinhoRequest;
import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.service.AnimalService;
import br.com.ongs.atividadeongs.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/animais")
public class AnimalController {

    private final AnimalService service;
    private final AuthService authService;

    public AnimalController(AnimalService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<AnimalResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnimalResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PostMapping
    public ResponseEntity<AnimalResponse> criar(
            @RequestHeader(value = "Authorization", required = false) String autorizacao,
            @RequestBody AnimalRequest dados) {
        Ong ong = authService.exigirOngDoUsuario(autorizacao);
        AnimalResponse criado = service.criar(ong, dados);
        return ResponseEntity.created(URI.create("/api/animais/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnimalResponse> atualizar(
            @RequestHeader(value = "Authorization", required = false) String autorizacao,
            @PathVariable Long id,
            @RequestBody AnimalRequest dados) {
        Ong ong = authService.exigirOngDoUsuario(autorizacao);
        return ResponseEntity.ok(service.atualizar(id, dados, ong));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AnimalResponse> alterarStatus(
            @RequestHeader(value = "Authorization", required = false) String autorizacao,
            @PathVariable Long id,
            @RequestBody AnimalStatusRequest dados) {
        Ong ong = authService.exigirOngDoUsuario(autorizacao);
        return ResponseEntity.ok(service.atualizarStatus(id, dados.status(), ong));
    }

    @PostMapping("/{id}/padrinhos")
    public ResponseEntity<AnimalResponse> adicionarPadrinho(@PathVariable Long id,
                                                            @RequestBody PadrinhoRequest dados) {
        return ResponseEntity.ok(service.adicionarPadrinho(id, dados.usuarioId()));
    }

    @DeleteMapping("/{id}/padrinhos/{usuarioId}")
    public ResponseEntity<AnimalResponse> removerPadrinho(@PathVariable Long id,
                                                          @PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.removerPadrinho(id, usuarioId));
    }
}
