package br.com.ongs.atividadeongs.controller;

import br.com.ongs.atividadeongs.dto.AtualizacaoRequest;
import br.com.ongs.atividadeongs.dto.AtualizacaoResponse;
import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.service.AtualizacaoService;
import br.com.ongs.atividadeongs.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/animais/{animalId}/atualizacoes")
public class AtualizacaoController {

    private final AtualizacaoService service;
    private final AuthService authService;

    public AtualizacaoController(AtualizacaoService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    /** Qualquer pessoa pode ver as atualizações de um animal. */
    @GetMapping
    public ResponseEntity<List<AtualizacaoResponse>> listar(@PathVariable Long animalId) {
        return ResponseEntity.ok(service.listar(animalId));
    }

    /** Só a ONG ou protetor dono do animal publica atualizações. */
    @PostMapping
    public ResponseEntity<AtualizacaoResponse> publicar(
            @RequestHeader(value = "Authorization", required = false) String autorizacao,
            @PathVariable Long animalId,
            @RequestBody AtualizacaoRequest dados) {
        Ong ong = authService.exigirOngDoUsuario(autorizacao);
        AtualizacaoResponse criada = service.publicar(animalId, dados, ong);
        return ResponseEntity
                .created(URI.create("/api/animais/" + animalId + "/atualizacoes/" + criada.id()))
                .body(criada);
    }

    @DeleteMapping("/{atualizacaoId}")
    public ResponseEntity<Void> excluir(
            @RequestHeader(value = "Authorization", required = false) String autorizacao,
            @PathVariable Long animalId,
            @PathVariable Long atualizacaoId) {
        Ong ong = authService.exigirOngDoUsuario(autorizacao);
        service.excluir(animalId, atualizacaoId, ong);
        return ResponseEntity.noContent().build();
    }
}
