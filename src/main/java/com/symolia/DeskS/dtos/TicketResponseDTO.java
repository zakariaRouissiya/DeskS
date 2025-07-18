package com.symolia.DeskS.dtos;

import com.symolia.DeskS.enums.Priorite;
import com.symolia.DeskS.enums.Statut;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class TicketResponseDTO {
    private Long id;
    private String titre;
    private String description;
    private Statut statut;
    private Priorite priorite;
    private LocalDateTime dateCreation;
    private LocalDateTime dateResolution;
    private String type;
    private UserDTO user;
    private UserDTO assignedTo;
    private PieceJointeDTO pieceJointe;
    
    @Data
    public static class UserDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private DepartmentDTO department;
        
        @Data
        public static class DepartmentDTO {
            private Long id;
            private String nom;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieceJointeDTO {
        private Long id;
        private String nomDuFichier;
        private String typeDuFichier;
        private String url;
        private Long taille;
        private String downloadUrl; 
        private String previewUrl; 
    }
}