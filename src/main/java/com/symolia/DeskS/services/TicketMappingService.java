package com.symolia.DeskS.services;

import com.symolia.DeskS.dtos.TicketResponseDTO;
import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.entities.PieceJointe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TicketMappingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketMappingService.class);
    private static final String BASE_URL = "http://localhost:8080";

    public TicketResponseDTO toResponseDTO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        logger.info("=== MAPPING TICKET {} ===", ticket.getId());

        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setTitre(ticket.getTitre());
        dto.setDescription(ticket.getDescription());
        dto.setStatut(ticket.getStatut());
        dto.setPriorite(ticket.getPriorite());
        dto.setDateCreation(ticket.getDateCreation());
        dto.setDateResolution(ticket.getDateResolution());
        dto.setType(ticket.getType());

        // Ajout dateFermeture et dateReouverture
        dto.setDateFermeture(ticket.getDateFermeture());
        dto.setDateReouverture(ticket.getDateReouverture());

        // Mapping user
        if (ticket.getUser() != null) {
            TicketResponseDTO.UserDTO userDTO = new TicketResponseDTO.UserDTO();
            userDTO.setId(ticket.getUser().getId());
            userDTO.setNom(ticket.getUser().getNom());
            userDTO.setPrenom(ticket.getUser().getPrenom());
            userDTO.setEmail(ticket.getUser().getEmail());

            if (ticket.getUser().getDepartment() != null) {
                TicketResponseDTO.UserDTO.DepartmentDTO deptDTO = new TicketResponseDTO.UserDTO.DepartmentDTO();
                deptDTO.setId(ticket.getUser().getDepartment().getId());
                deptDTO.setNom(ticket.getUser().getDepartment().getNom());
                userDTO.setDepartment(deptDTO);
            }

            dto.setUser(userDTO);
        }

        // Mapping assignedTo
        if (ticket.getAssignedTo() != null) {
            logger.info("Mapping assignedTo: {} {}", ticket.getAssignedTo().getPrenom(), ticket.getAssignedTo().getNom());

            TicketResponseDTO.UserDTO assignedToDTO = new TicketResponseDTO.UserDTO();
            assignedToDTO.setId(ticket.getAssignedTo().getId());
            assignedToDTO.setNom(ticket.getAssignedTo().getNom());
            assignedToDTO.setPrenom(ticket.getAssignedTo().getPrenom());
            assignedToDTO.setEmail(ticket.getAssignedTo().getEmail());

            if (ticket.getAssignedTo().getDepartment() != null) {
                TicketResponseDTO.UserDTO.DepartmentDTO deptDTO = new TicketResponseDTO.UserDTO.DepartmentDTO();
                deptDTO.setId(ticket.getAssignedTo().getDepartment().getId());
                deptDTO.setNom(ticket.getAssignedTo().getDepartment().getNom());
                assignedToDTO.setDepartment(deptDTO);
            }

            dto.setAssignedTo(assignedToDTO);
            logger.info("AssignedTo mappé avec succès: {} {}", assignedToDTO.getPrenom(), assignedToDTO.getNom());
        } else {
            logger.info("Aucun technicien assigné au ticket {}", ticket.getId());
        }

        // Mapping closedBy
        if (ticket.getClosedBy() != null) {
            TicketResponseDTO.UserDTO closedByDTO = new TicketResponseDTO.UserDTO();
            closedByDTO.setId(ticket.getClosedBy().getId());
            closedByDTO.setNom(ticket.getClosedBy().getNom());
            closedByDTO.setPrenom(ticket.getClosedBy().getPrenom());
            closedByDTO.setEmail(ticket.getClosedBy().getEmail());

            if (ticket.getClosedBy().getDepartment() != null) {
                TicketResponseDTO.UserDTO.DepartmentDTO deptDTO = new TicketResponseDTO.UserDTO.DepartmentDTO();
                deptDTO.setId(ticket.getClosedBy().getDepartment().getId());
                deptDTO.setNom(ticket.getClosedBy().getDepartment().getNom());
                closedByDTO.setDepartment(deptDTO);
            }

            dto.setClosedBy(closedByDTO);
        }

        // Mapping reopenedBy
        if (ticket.getReopenedBy() != null) {
            TicketResponseDTO.UserDTO reopenedByDTO = new TicketResponseDTO.UserDTO();
            reopenedByDTO.setId(ticket.getReopenedBy().getId());
            reopenedByDTO.setNom(ticket.getReopenedBy().getNom());
            reopenedByDTO.setPrenom(ticket.getReopenedBy().getPrenom());
            reopenedByDTO.setEmail(ticket.getReopenedBy().getEmail());

            if (ticket.getReopenedBy().getDepartment() != null) {
                TicketResponseDTO.UserDTO.DepartmentDTO deptDTO = new TicketResponseDTO.UserDTO.DepartmentDTO();
                deptDTO.setId(ticket.getReopenedBy().getDepartment().getId());
                deptDTO.setNom(ticket.getReopenedBy().getDepartment().getNom());
                reopenedByDTO.setDepartment(deptDTO);
            }

            dto.setReopenedBy(reopenedByDTO);
        }

        // Mapping pièce jointe
        PieceJointe pieceJointe = ticket.getPieceJointe();
        if (pieceJointe != null) {
            logger.info("Mapping pièce jointe: {}", pieceJointe.getNomDuFichier());

            TicketResponseDTO.PieceJointeDTO pieceJointeDTO = new TicketResponseDTO.PieceJointeDTO();
            pieceJointeDTO.setId(pieceJointe.getId());
            pieceJointeDTO.setNomDuFichier(pieceJointe.getNomDuFichier());
            pieceJointeDTO.setTypeDuFichier(pieceJointe.getTypeDuFichier());
            pieceJointeDTO.setUrl(pieceJointe.getUrl());
            pieceJointeDTO.setTaille(pieceJointe.getTaille());
            pieceJointeDTO.setDownloadUrl(BASE_URL + "/api/files/download/" + pieceJointe.getId());
            pieceJointeDTO.setPreviewUrl(BASE_URL + "/api/files/preview/" + pieceJointe.getId());

            dto.setPieceJointe(pieceJointeDTO);
        }

        logger.info("=== FIN MAPPING ===");
        return dto;
    }
}