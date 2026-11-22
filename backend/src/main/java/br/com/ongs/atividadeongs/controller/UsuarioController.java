package br.com.ongs.atividadeongs.controller;

/**
 * Os antigos endpoints de /api/usuarios (listar todos os usuários, buscar por id e
 * cadastrar sem senha) foram removidos porque expunham dados pessoais.
 * Cadastro e login ficam em /api/auth. Esta classe não é mais um controlador e pode
 * ser apagada em uma limpeza futura do projeto.
 */
final class UsuarioController {

    private UsuarioController() {
    }
}
