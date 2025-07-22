package com.symolia.DeskS.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.symolia.DeskS.enums.StatutDelegation;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "delegations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delegation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @JsonIgnore 
    private Ticket ticket;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_technician_id", nullable = false)
    private Utilisateur fromTechnician;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_technician_id", nullable = false)
    private Utilisateur toTechnician;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDelegation statut;
    
    @CreationTimestamp
    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;
    
    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;
    
    @Column(name = "commentaire_reponse")
    private String commentaireReponse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Utilisateur approvedBy;
}