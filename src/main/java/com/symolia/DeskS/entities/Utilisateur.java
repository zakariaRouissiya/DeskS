package com.symolia.DeskS.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.symolia.DeskS.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
@Data
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "text")
    private String email;

    @Column(name = "mot_de_passe", nullable = false, columnDefinition = "text")
    private String motDePasse;

    @Column(name = "nom", nullable = false, columnDefinition = "text")
    private String nom;

    @Column(name = "prenom", nullable = false, columnDefinition = "text")
    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "text")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonIgnore
    private Departement department;

    @Column(name = "date_inscription", nullable = false)
    private LocalDateTime dateInscription;
}