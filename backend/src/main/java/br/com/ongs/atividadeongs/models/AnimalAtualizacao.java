package br.com.ongs.atividadeongs.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** Novidade publicada por quem cuida do animal (por exemplo: "fez a cirurgia"). */
@Entity
@Table(name = "animal_atualizacoes")
public class AnimalAtualizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(nullable = false, length = 1000)
    private String texto;

    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(name = "data_publicacao", nullable = false)
    private Instant dataPublicacao;

    @PrePersist
    void definirDataDePublicacao() {
        if (dataPublicacao == null) {
            dataPublicacao = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Instant getDataPublicacao() {
        return dataPublicacao;
    }

    public void setDataPublicacao(Instant dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }
}
