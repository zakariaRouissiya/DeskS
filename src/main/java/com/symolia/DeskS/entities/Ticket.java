package com.symolia.DeskS.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.symolia.DeskS.enums.Priorite;
import com.symolia.DeskS.enums.Statut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ticket")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AllArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priorite priorite;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateResolution;

    private Duration tempsResolution;

    private String type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private Utilisateur user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Departement department;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore 
    private List<Commentaire> commentaires;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<PieceJointe> piecesJointes;

    public PieceJointe getPieceJointe() {
        return (piecesJointes != null && !piecesJointes.isEmpty()) ? piecesJointes.get(0) : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id", foreignKey = @ForeignKey(name = "fk_ticket_assigned_to"))
    private Utilisateur assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_to_id")
    private Utilisateur delegatedTo;

    private LocalDateTime dateFermeture;
    private LocalDateTime dateReouverture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_id")
    private Utilisateur closedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reopened_by_id")
    private Utilisateur reopenedBy;

    public Utilisateur getDelegatedTo() {
        return delegatedTo;
    }

    public void setDelegatedTo(Utilisateur delegatedTo) {
        this.delegatedTo = delegatedTo;
    }
}