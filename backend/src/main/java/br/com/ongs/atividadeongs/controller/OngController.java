package br.com.ongs.atividadeongs.controller;

import br.com.ongs.atividadeongs.dto.DoacaoResponse;
import br.com.ongs.atividadeongs.dto.OngRequest;
import br.com.ongs.atividadeongs.dto.OngResponse;
import br.com.ongs.atividadeongs.models.Ong;
import br.com.ongs.atividadeongs.service.AuthService;
import br.com.ongs.atividadeongs.service.DoacaoService;
import br.com.ongs.atividadeongs.service.OngService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ongs")
public class OngController {

    private final OngService ongService;
    private final AuthService authService;
    private final DoacaoService doacaoService;

    public OngController(OngService ongService, AuthService authService, DoacaoService doacaoService) {
        this.ongService = ongService;
        this.authService = authService;
        this.doacaoService = doacaoService;
    }

    /** Lista pública: os doadores precisam da chave PIX de cada ONG. */
    @GetMapping
    public ResponseEntity<List<OngResponse>> listar() {
        return ResponseEntity.ok(ongService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OngResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ongService.buscar(id));
    }

    /** A conta de ONG/protetor edita somente o próprio perfil. */
    @PutMapping("/minha")
    public ResponseEntity<OngResponse> atualizarMinha(
            @RequestHeader(value = "Authorization", required = false) String autorizacao,
            @RequestBody OngRequest dados) {
        Ong minha = authService.exigirOngDoUsuario(autorizacao);
        return ResponseEntity.ok(ongService.atualizar(minha.getId(), dados));
    }

    /** Doações recebidas pelos animais da ONG/protetor que está logado. */
    @GetMapping("/minha/doacoes")
    public ResponseEntity<List<DoacaoResponse>> doacoesDaMinha(
            @RequestHeader(value = "Authorization", required = false) String autorizacao) {
        Ong minha = authService.exigirOngDoUsuario(autorizacao);
        return ResponseEntity.ok(doacaoService.listarPorOng(minha.getId()));
    }
}
