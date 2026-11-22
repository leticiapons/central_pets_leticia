package br.com.ongs.atividadeongs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Ajuste do banco para bancos criados antes de a doação ganhar o status PENDENTE.
 *
 * Ao criar a tabela "doacoes", o Hibernate gravou uma regra no banco que só aceitava
 * os status que existiam na época (DOADO e CANCELADA). O "ddl-auto=update" não atualiza
 * essa regra, e por isso uma doação PENDENTE era recusada. Aqui a regra antiga é removida.
 * Se ela não existir (banco novo), nada acontece.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AjusteBancoInicializador implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AjusteBancoInicializador.class);

    private static final String REMOVER_REGRA_ANTIGA =
            "DO $$ "
                    + "DECLARE regra record; "
                    + "BEGIN "
                    + "  FOR regra IN "
                    + "    SELECT conname FROM pg_constraint "
                    + "    WHERE conrelid = 'doacoes'::regclass AND contype = 'c' "
                    + "      AND pg_get_constraintdef(oid) ILIKE '%status%' "
                    + "  LOOP "
                    + "    EXECUTE format('ALTER TABLE doacoes DROP CONSTRAINT %I', regra.conname); "
                    + "  END LOOP; "
                    + "END $$;";

    private final DataSource dataSource;

    public AjusteBancoInicializador(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection conexao = dataSource.getConnection();
             Statement comando = conexao.createStatement()) {
            comando.execute(REMOVER_REGRA_ANTIGA);
            log.info("Regra antiga do status das doações conferida.");
        } catch (Exception e) {
            log.warn("Não foi possível conferir a regra do status das doações: {}", e.getMessage());
        }
    }
}
