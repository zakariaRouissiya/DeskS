package com.symolia.DeskS.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Commentaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String commentaire;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = true)
    private LocalDateTime dateModification;

    @Column(nullable = false)
    private Boolean modifie = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "auteur_id", foreignKey = @ForeignKey(name = "fk_commentaire_auteur"))
    @JsonIgnoreProperties({"tickets", "commentaires", "motDePasse", "department"})
    private Utilisateur auteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", foreignKey = @ForeignKey(name = "fk_commentaire_ticket"))
    @JsonIgnoreProperties({"commentaires", "delegations", "user", "assignedTo", "delegatedTo", "department", "pieceJointe"})
    private Ticket ticket;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.modifie = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
        this.modifie = true;
    }
}