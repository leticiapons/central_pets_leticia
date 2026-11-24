package br.com.ongs.atividadeongs.controller;

import br.com.ongs.atividadeongs.dto.DoacaoRequest;
import br.com.ongs.atividadeongs.dto.DoacaoResponse;
import br.com.ongs.atividadeongs.dto.DoacaoStatusRequest;
import br.com.ongs.atividadeongs.exception.AcessoNegadoException;
import br.com.ongs.atividadeongs.models.Ong;
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

    /** Histórico de doações de quem está logado (doador ou padrinho). */
    @GetMapping("/minhas")
    public ResponseEntity<List<DoacaoResponse>> minhas(
            @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        Usuario quem = authService.exigirUsuario(autorizacao, null);
        if (quem.getPerfil() == PerfilUsuario.ADM) {
            throw new AcessoNegadoException("Contas de ONG ou protetor veem as doações recebidas no painel.");
        }
        return ResponseEntity.ok(doacaoService.listarDoUsuario(quem.getId()));
    }

    /** A ONG ou protetor confirma, recusa ou desfaz o recebimento de uma doação. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<DoacaoResponse> alterarStatus(
            @RequestHeader(value = "Authorization", required = false) String autorizacao,
            @PathVariable Long id,
            @RequestBody DoacaoStatusRequest dados) {
        Ong ong = authService.exigirOngDoUsuario(autorizacao);
        return ResponseEntity.ok(doacaoService.atualizarStatus(id, dados.status(), ong));
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
