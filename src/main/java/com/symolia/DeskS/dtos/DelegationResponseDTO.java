package com.symolia.DeskS.dtos;

import com.symolia.DeskS.enums.StatutDelegation;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DelegationResponseDTO {
    private Long id;
    private Long ticketId;
    private String ticketTitre;
    private UserDTO fromTechnician;
    private UserDTO toTechnician;
    private String justification;
    private StatutDelegation statut;
    private LocalDateTime dateDemande;
    private LocalDateTime dateReponse;
    private String commentaireReponse;
    private UserDTO approvedBy;
    
    @Data
    public static class UserDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String departmentNom;
    }
}