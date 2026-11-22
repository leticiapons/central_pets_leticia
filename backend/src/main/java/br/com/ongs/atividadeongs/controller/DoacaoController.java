package br.com.ongs.atividadeongs.controller;

import br.com.ongs.atividadeongs.dto.DoacaoRequest;
import br.com.ongs.atividadeongs.dto.DoacaoResponse;
import br.com.ongs.atividadeongs.exception.AcessoNegadoException;
import br.com.ongs.atividadeongs.models.PerfilUsuario;
import br.com.ongs.atividadeongs.models.Usuario;
import br.com.ongs.atividadeongs.service.AuthService;
import br.com.ongs.atividadeongs.service.DoacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/doacoes")
public class DoacaoController {

    private final DoacaoService doacaoService;
    private final AuthService authService;

    public DoacaoController(DoacaoService doacaoService, AuthService authService) {
        this.doacaoService = doacaoService;
        this.authService = authService;
    }

    /** Histórico de doações do padrinho que está logado. */
    @GetMapping("/minhas")
    public ResponseEntity<List<DoacaoResponse>> minhas(
            @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        Usuario padrinho = authService.exigirUsuario(autorizacao, PerfilUsuario.PADRINHO);
        return ResponseEntity.ok(doacaoService.listarPorPadrinho(padrinho.getId()));
    }

    /** Doadores e padrinhos logados registram doações. ONGs e protetores não doam. */
    @PostMapping
    public ResponseEntity<DoacaoResponse> criar(
            @RequestHeader(value = "Authorization", required = false) String autorizacao,
            @RequestBody DoacaoRequest dados) {
        Usuario doador = authService.exigirUsuario(autorizacao, null);
        if (doador.getPerfil() == PerfilUsuario.ADM) {
            throw new AcessoNegadoException("Contas de ONG ou protetor não fazem doações.");
        }

        DoacaoResponse criada = doacaoService.criar(doador, dados);

        return ResponseEntity
                .created(URI.create("/api/doacoes/" + criada.id()))
                .body(criada);
    }
}
