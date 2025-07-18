package com.symolia.DeskS.web;

import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.entities.PieceJointe;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.Priorite;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.enums.Statut;
import com.symolia.DeskS.repositories.TicketRepository;
import com.symolia.DeskS.repositories.UtilisateurRepository;
import com.symolia.DeskS.services.TicketService;
import com.symolia.DeskS.services.TicketMappingService;
import com.symolia.DeskS.dtos.TicketResponseDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TicketController {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
    
    private final TicketService ticketService;
    private final TicketMappingService ticketMappingService;
    private final TicketRepository ticketRepository;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createTicketWithAttachment(
            @RequestParam("userId") Long userId,
            @RequestParam("titre") String titre,
            @RequestParam("description") String description,
            @RequestParam("priorite") String priorite,
            @RequestParam("type") String type,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            logger.info("Création d'un ticket avec pièce jointe - Utilisateur: {}, Fichier: {}", 
                userId, file != null ? file.getOriginalFilename() : "aucun");
            
            Ticket ticket = new Ticket();
            ticket.setTitre(titre);
            ticket.setDescription(description);
            ticket.setPriorite(Priorite.valueOf(priorite.toUpperCase()));
            ticket.setType(type);
            
            Ticket savedTicket;
            if (file != null && !file.isEmpty()) {
                savedTicket = ticketService.createTicketWithAttachment(ticket, userId, file);
            } else {
                savedTicket = ticketService.createTicket(ticket, userId);
            }
            
            TicketResponseDTO ticketDTO = ticketMappingService.toResponseDTO(savedTicket);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ticket créé avec succès");
            response.put("ticket", ticketDTO);
            
            logger.info("Ticket créé avec succès - ID: {}", savedTicket.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la création du ticket: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping(value = "/{ticketId}/attachment", consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> addAttachmentToTicket(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file) {
        try {
            logger.info("Ajout d'une pièce jointe au ticket: {}, Fichier: {}", 
                ticketId, file.getOriginalFilename());
            
            PieceJointe pieceJointe = ticketService.addAttachmentToTicket(ticketId, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pièce jointe ajoutée avec succès");
            response.put("pieceJointe", Map.of(
                "id", pieceJointe.getId(),
                "nomDuFichier", pieceJointe.getNomDuFichier(),
                "typeDuFichier", pieceJointe.getTypeDuFichier(),
                "taille", pieceJointe.getTaille(),
                "url", pieceJointe.getUrl()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout de la pièce jointe: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/test-with-attachment/{userId}")
    public ResponseEntity<Map<String, Object>> createTestTicketWithAttachment(@PathVariable Long userId) {
        try {
            ticketService.createTestTicketWithAttachment(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Ticket test avec pièce jointe créé");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Erreur lors de la création du ticket test: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/my/{userId}")
    public ResponseEntity<List<TicketResponseDTO>> getMyTickets(@PathVariable Long userId) {
        try {
            logger.info("Récupération des tickets pour l'utilisateur: {}", userId);
            
            List<Ticket> tickets = ticketService.getTicketsByUserIdWithPieceJointe(userId);
            logger.info("Tickets trouvés: {}", tickets.size());
            
            List<TicketResponseDTO> ticketDTOs = tickets.stream()
                    .map(ticket -> {
                        TicketResponseDTO dto = ticketMappingService.toResponseDTO(ticket);
                        if (dto.getPieceJointe() != null) {
                            logger.info("Ticket {} avec pièce jointe: {}", 
                                dto.getId(), 
                                dto.getPieceJointe().getNomDuFichier());
                        } else {
                            logger.info("Ticket {} sans pièce jointe", dto.getId());
                        }
                        return dto;
                    })
                    .toList();
                    
            logger.info("DTOs créés: {}", ticketDTOs.size());
            return ResponseEntity.ok(ticketDTOs);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des tickets: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{ticketId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
    public ResponseEntity<?> updateTicket(@PathVariable Long ticketId,
                                          @RequestBody Ticket ticketData) {
        try {
            Ticket updated = ticketService.updateTicket(ticketId, ticketData);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("ticket", updated);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/employee/{ticketId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
    public ResponseEntity<?> deleteTicketByEmployee(@PathVariable Long ticketId,
                                                    @RequestParam Long userId) {
        try {
            ticketService.deleteTicketByEmployee(ticketId, userId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> getTicketDetails(@PathVariable Long ticketId) {
        try {
            logger.info("Récupération des détails du ticket: {}", ticketId);
            
            Ticket ticket = ticketService.getTicketByIdWithPieceJointe(ticketId);
            logger.info("Ticket trouvé: {}", ticket.getTitre());
            
            if (ticket.getPieceJointe() != null) {
                logger.info("Pièce jointe trouvée: {}", ticket.getPieceJointe().getNomDuFichier());
            } else {
                logger.warn("Aucune pièce jointe trouvée pour le ticket: {}", ticketId);
            }
            
            TicketResponseDTO ticketDTO = ticketMappingService.toResponseDTO(ticket);
            
            if (ticketDTO.getPieceJointe() != null) {
                logger.info("DTO avec pièce jointe: {}", ticketDTO.getPieceJointe().getNomDuFichier());
            } else {
                logger.warn("DTO sans pièce jointe");
            }
            
            return ResponseEntity.ok(ticketDTO);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du ticket {}: ", ticketId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/debug/{ticketId}")
    public ResponseEntity<Map<String, Object>> debugTicket(@PathVariable Long ticketId) {
        try {
            ticketService.debugTicketAttachments(ticketId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Vérifiez les logs pour les détails");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<TicketResponseDTO>> searchTickets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String priorite,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin) {
        try {
            logger.info("Recherche avancée de tickets avec paramètres: search={}, statut={}", search, statut);
            
            Statut statutEnum = statut != null ? Statut.valueOf(statut.toUpperCase()) : null;
            Priorite prioriteEnum = priorite != null ? Priorite.valueOf(priorite.toUpperCase()) : null;
            LocalDateTime dateDebutParsed = dateDebut != null ? LocalDateTime.parse(dateDebut) : null;
            LocalDateTime dateFinParsed = dateFin != null ? LocalDateTime.parse(dateFin) : null;
            
            List<Ticket> tickets = ticketService.findTicketsWithFilters(
                statutEnum, prioriteEnum, departmentId, assignedToId, 
                dateDebutParsed, dateFinParsed, search);
            
            List<TicketResponseDTO> ticketDTOs = tickets.stream()
                    .map(ticketMappingService::toResponseDTO)
                    .toList();
            
            return ResponseEntity.ok(ticketDTOs);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de tickets: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/statistics/period")
    public ResponseEntity<List<Map<String, Object>>> getTicketStatisticsByPeriod(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            
            List<Object[]> results = ticketService.getTicketStatisticsByPeriod(start, end);
            
            List<Map<String, Object>> statistics = results.stream()
                    .map(row -> {
                        Map<String, Object> stat = new HashMap<>();
                        stat.put("statut", row[0]);
                        stat.put("count", row[1]);
                        stat.put("avgResolutionHours", row[2]);
                        return stat;
                    })
                    .toList();
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/high-priority")
    public ResponseEntity<List<TicketResponseDTO>> getHighPriorityTickets() {
        try {
            List<Ticket> tickets = ticketService.getHighPriorityTickets();
            List<TicketResponseDTO> ticketDTOs = tickets.stream()
                    .map(ticketMappingService::toResponseDTO)
                    .toList();
            
            return ResponseEntity.ok(ticketDTOs);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des tickets prioritaires: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/debug/departments")
    public ResponseEntity<Map<String, Object>> debugDepartments() {
        try {
            List<Object[]> results = ticketRepository.findAll().stream()
                .map(ticket -> new Object[]{
                    ticket.getId(),
                    ticket.getTitre(),
                    ticket.getUser() != null ? ticket.getUser().getId() : null,
                    ticket.getUser() != null ? ticket.getUser().getNom() + " " + ticket.getUser().getPrenom() : null,
                    ticket.getDepartment() != null ? ticket.getDepartment().getId() : null,
                    ticket.getDepartment() != null ? ticket.getDepartment().getNom() : null,
                    ticket.getUser() != null && ticket.getUser().getDepartment() != null ? 
                        ticket.getUser().getDepartment().getId() : null,
                    ticket.getUser() != null && ticket.getUser().getDepartment() != null ? 
                        ticket.getUser().getDepartment().getNom() : null
                })
                .toList();

            Map<String, Object> debug = new HashMap<>();
            debug.put("tickets_count", results.size());
            debug.put("tickets_details", results);
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            logger.error("Erreur lors du debug des départements: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<List<TicketResponseDTO>> getDepartmentTickets(@PathVariable Long departmentId) {
        try {
            logger.info("Récupération des tickets du département: {}", departmentId);
            
            List<Ticket> tickets = ticketService.getTicketsByDepartmentWithPieceJointe(departmentId);
            logger.info("Tickets trouvés pour le département {}: {}", departmentId, tickets.size());
            
            List<TicketResponseDTO> ticketDTOs = tickets.stream()
                    .map(ticketMappingService::toResponseDTO)
                    .toList();
                    
            return ResponseEntity.ok(ticketDTOs);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des tickets du département {}: ", departmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{ticketId}/assign")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_TECHNICIEN_SUPPORT')")
    public ResponseEntity<Map<String, Object>> assignTicket(
            @PathVariable Long ticketId,
            @RequestBody Map<String, Long> request) {
        try {
            Long technicianId = request.get("technicianId");
            logger.info("Assignation du ticket {} au technicien {}", ticketId, technicianId);
            
            TicketResponseDTO ticketDTO = ticketService.assignTicketToTechnician(ticketId, technicianId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ticket assigné avec succès");
            response.put("ticket", ticketDTO);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de l'assignation du ticket {}: ", ticketId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/auto-dispatch")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> autoDispatchTickets(
            @RequestBody Map<String, Long> request) {
        try {
            Long departmentId = request.get("departmentId");
            logger.info("Répartition automatique des tickets pour le département: {}", departmentId);
            
            int assignedCount = ticketService.autoDispatchTickets(departmentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Répartition automatique effectuée");
            response.put("assignedCount", assignedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la répartition automatique: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/department/{departmentId}/statistics")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Object>> getDepartmentStatistics(@PathVariable Long departmentId) {
        try {
            Map<String, Object> statistics = ticketService.getDepartmentStatistics(departmentId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques du département {}: ", departmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/assigned/{technicianId}")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<List<TicketResponseDTO>> getAssignedTickets(@PathVariable Long technicianId) {
        try {
            logger.info("Récupération des tickets assignés au technicien: {}", technicianId);
            
            List<Ticket> tickets = ticketService.getAssignedTicketsToTechnician(technicianId);
            logger.info("Tickets assignés trouvés pour le technicien {}: {}", technicianId, tickets.size());
            
            List<TicketResponseDTO> ticketDTOs = tickets.stream()
                    .map(ticketMappingService::toResponseDTO)
                    .toList();
                    
            return ResponseEntity.ok(ticketDTOs);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des tickets assignés au technicien {}: ", technicianId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/technicians/{technicianId}/workload")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_TECHNICIEN_SUPPORT') or hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<Map<String, Object>> getTechnicianWorkload(@PathVariable Long technicianId) {
        try {
            logger.info("Récupération de la charge de travail pour le technicien: {}", technicianId);
            
            Map<String, Object> workloadDetails = ticketService.getTechnicianWorkloadDetails(technicianId);
            int workload = ticketService.getTechnicianWorkload(technicianId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("technicianId", technicianId);
            response.put("workload", workload);
            response.put("details", workloadDetails);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de la charge de travail: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/department/{departmentId}/technicians/workload")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_ADMINISTRATEUR')")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentTechniciansWorkload(@PathVariable Long departmentId) {
        try {
            logger.info("Récupération de la charge des techniciens pour le département: {}", departmentId);
            
            List<Utilisateur> technicians = utilisateurRepository.findByRoleAndDepartment_Id(
                Role.TECHNICIEN_SUPPORT, departmentId);
            
            List<Map<String, Object>> techniciansWorkload = new ArrayList<>();
            
            for (Utilisateur tech : technicians) {
                Map<String, Object> techData = new HashMap<>();
                techData.put("id", tech.getId());
                techData.put("nom", tech.getNom());
                techData.put("prenom", tech.getPrenom());
                techData.put("email", tech.getEmail());
                
                Map<String, Object> workloadDetails = ticketService.getTechnicianWorkloadDetails(tech.getId());
                techData.put("workload", ticketService.getTechnicianWorkload(tech.getId()));
                techData.put("details", workloadDetails);
                
                techniciansWorkload.add(techData);
            }
            
            return ResponseEntity.ok(techniciansWorkload);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des charges du département: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{ticketId}/statut")
    public ResponseEntity<?> updateTicketStatut(@PathVariable Long ticketId, @RequestBody Map<String, String> body) {
        try {
            String statut = body.get("statut");
            Ticket updated = ticketService.updateStatut(ticketId, statut);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("ticket", updated);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}