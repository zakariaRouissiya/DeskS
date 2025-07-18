package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.dtos.TicketResponseDTO;
import com.symolia.DeskS.entities.Departement;
import com.symolia.DeskS.entities.PieceJointe;
import com.symolia.DeskS.enums.Priorite;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.enums.Statut;
import com.symolia.DeskS.repositories.TicketRepository;
import com.symolia.DeskS.repositories.UtilisateurRepository;
import com.symolia.DeskS.repositories.DepartementRepository;
import com.symolia.DeskS.repositories.PieceJointeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    
    private final TicketRepository ticketRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DepartementRepository departementRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final TicketMappingService ticketMappingService;
    private final EmailService emailService;
    
    private static final String UPLOAD_DIR = "uploads/attachments/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSIONS = {".pdf", ".jpg", ".jpeg", ".png", ".doc", ".docx", ".txt"};

    public Ticket createTicketWithAttachment(Ticket ticket, Long userId, MultipartFile file) {
        logger.info("Création du ticket avec pièce jointe pour l'utilisateur: {}", userId);
        
        Ticket savedTicket = createTicket(ticket, userId);
        
        if (file != null && !file.isEmpty()) {
            try {
                PieceJointe pieceJointe = saveAttachment(file, savedTicket);
                logger.info("Pièce jointe sauvegardée avec succès: {} pour le ticket: {}", 
                    pieceJointe.getNomDuFichier(), savedTicket.getId());
            } catch (Exception e) {
                logger.error("Erreur lors de la sauvegarde de la pièce jointe: ", e);
                throw new RuntimeException("Erreur lors de la sauvegarde de la pièce jointe: " + e.getMessage());
            }
        }
        
        return getTicketByIdWithPieceJointe(savedTicket.getId());
    }

    public Ticket createTicket(Ticket ticket, Long userId) {
        logger.info("Création du ticket pour l'utilisateur: {}", userId);
        
        Utilisateur user = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID: " + userId));
        
        logger.info("Utilisateur trouvé: {} {}, Département: {}", 
            user.getPrenom(), user.getNom(), 
            user.getDepartment() != null ? user.getDepartment().getNom() : "AUCUN");
        
        ticket.setUser(user);
        
        if (user.getDepartment() != null) {
            ticket.setDepartment(user.getDepartment());
            logger.info("Département assigné au ticket: {}", user.getDepartment().getNom());
        } else {
            logger.warn("L'utilisateur {} n'a pas de département assigné", userId);
        }
        
        ticket.setStatut(Statut.OUVERT);
        ticket.setDateCreation(LocalDateTime.now());
        
        Ticket savedTicket = ticketRepository.save(ticket);
        logger.info("Ticket créé avec succès - ID: {}, Département: {}", 
            savedTicket.getId(), 
            savedTicket.getDepartment() != null ? savedTicket.getDepartment().getNom() : "AUCUN");
        
        if (savedTicket.getDepartment() != null) {
            try {
                emailService.sendTicketCreationNotificationToManagers(savedTicket);
            } catch (Exception e) {
                logger.error("Erreur lors de l'envoi de notification de création de ticket: ", e);
            }
        }
        
        return savedTicket;
    }

    private PieceJointe saveAttachment(MultipartFile file, Ticket ticket) throws IOException {
        logger.info("Début de sauvegarde de la pièce jointe: {}", file.getOriginalFilename());
        
        validateFile(file);
        
        createUploadDirectoryIfNotExists();
        
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = generateUniqueFilename(originalFilename);
        String relativePath = UPLOAD_DIR + uniqueFilename;
        
        Path fullPath = Paths.get(relativePath);
        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, file.getBytes());
        
        logger.info("Fichier sauvegardé à: {}", fullPath.toAbsolutePath());
        
        PieceJointe pieceJointe = new PieceJointe();
        pieceJointe.setNomDuFichier(originalFilename);
        pieceJointe.setTypeDuFichier(file.getContentType());
        pieceJointe.setTaille(file.getSize());
        pieceJointe.setUrl("/" + relativePath);
        pieceJointe.setTicket(ticket);
        
        PieceJointe savedPieceJointe = pieceJointeRepository.save(pieceJointe);
        
        logger.info("PieceJointe sauvegardée en DB - ID: {}, Nom: {}, Taille: {} bytes, URL: {}", 
            savedPieceJointe.getId(), 
            savedPieceJointe.getNomDuFichier(), 
            savedPieceJointe.getTaille(), 
            savedPieceJointe.getUrl());
        
        return savedPieceJointe;
    }

    public PieceJointe addAttachmentToTicket(Long ticketId, MultipartFile file) throws IOException {
        logger.info("Ajout d'une pièce jointe au ticket: {}", ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket introuvable avec l'ID: " + ticketId));
        
        return saveAttachment(file, ticket);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Le fichier est vide");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Le fichier est trop volumineux (max 10MB)");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("Nom de fichier invalide");
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        boolean isAllowed = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            throw new RuntimeException("Type de fichier non autorisé. Extensions autorisées: " + 
                String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private void createUploadDirectoryIfNotExists() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("Répertoire de téléchargement créé: {}", dir.getAbsolutePath());
            } else {
                logger.warn("Impossible de créer le répertoire: {}", dir.getAbsolutePath());
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        return baseName + "_" + timestamp + "_" + uuid + extension;
    }

    @Transactional
    public void createTestTicketWithAttachment(Long userId) {
        logger.info("Création d'un ticket test avec pièce jointe pour l'utilisateur: {}", userId);
        
        Ticket testTicket = new Ticket();
        testTicket.setTitre("Ticket test avec pièce jointe");
        testTicket.setDescription("Ceci est un ticket de test créé pour vérifier l'enregistrement des pièces jointes");
        testTicket.setPriorite(Priorite.MOYENNE);
        testTicket.setType("Test");
        
        Ticket savedTicket = createTicket(testTicket, userId);
        
        PieceJointe testPieceJointe = new PieceJointe();
        testPieceJointe.setNomDuFichier("document-test.pdf");
        testPieceJointe.setTypeDuFichier("application/pdf");
        testPieceJointe.setTaille(1024000L);
        testPieceJointe.setUrl("/uploads/attachments/document-test.pdf");
        testPieceJointe.setTicket(savedTicket);
        
        PieceJointe savedPieceJointe = pieceJointeRepository.save(testPieceJointe);
        
        logger.info("Ticket test créé avec pièce jointe - Ticket ID: {}, PieceJointe ID: {}", 
            savedTicket.getId(), savedPieceJointe.getId());
    }

    public List<Ticket> getTicketsByUserIdWithPieceJointe(Long userId) {
        logger.info("Récupération des tickets avec pièces jointes pour l'utilisateur: {}", userId);
        
        List<Ticket> tickets = ticketRepository.findByUserIdWithPiecesJointes(userId);
        
        tickets.forEach(ticket -> {
            logger.info("Ticket {}: {} pièce(s) jointe(s)", 
                ticket.getId(), 
                ticket.getPiecesJointes() != null ? ticket.getPiecesJointes().size() : 0);
                
            if (ticket.getPieceJointe() != null) {
                logger.info("  - Première pièce jointe: {}", ticket.getPieceJointe().getNomDuFichier());
            }
        });
        
        return tickets;
    }

    public Ticket getTicketByIdWithPieceJointe(Long ticketId) {
        logger.info("Récupération du ticket avec pièces jointes: {}", ticketId);
        
        Ticket ticket = ticketRepository.findByIdWithPiecesJointes(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket introuvable avec l'ID: " + ticketId));
        
        logger.info("Ticket trouvé: {}", ticket.getId());
        logger.info("Nombre de pièces jointes: {}", 
            ticket.getPiecesJointes() != null ? ticket.getPiecesJointes().size() : 0);
            
        if (ticket.getPieceJointe() != null) {
            logger.info("Première pièce jointe: {}, URL: {}", 
                ticket.getPieceJointe().getNomDuFichier(), 
                ticket.getPieceJointe().getUrl());
        } else {
            logger.warn("Aucune pièce jointe trouvée pour le ticket: {}", ticketId);
        }
        
        return ticket;
    }

    public Ticket updateTicket(Long ticketId, Ticket ticketData) {
        Ticket existing = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket introuvable"));
        if (ticketData.getTitre() != null) existing.setTitre(ticketData.getTitre());
        if (ticketData.getDescription() != null) existing.setDescription(ticketData.getDescription());
        if (ticketData.getType() != null) existing.setType(ticketData.getType());
        if (ticketData.getPriorite() != null) existing.setPriorite(ticketData.getPriorite());
        return ticketRepository.save(existing);
    }

    public void deleteTicketByEmployee(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket introuvable"));
        if (!ticket.getUser().getId().equals(userId))
            throw new RuntimeException("Droit insuffisant pour supprimer ce ticket");
        if (ticket.getStatut() != Statut.OUVERT)
            throw new RuntimeException("Seuls les tickets ouverts peuvent être supprimés");
        ticketRepository.delete(ticket);
    }

    public long countActiveTickets() {
        return ticketRepository.findByStatut(Statut.EN_COURS).size();
    }

    public long countOpenIncidents() {
        return ticketRepository.findByStatut(Statut.OUVERT).size();
    }

    public long countClosedTickets() {
        return ticketRepository.findByStatut(Statut.FERME).size();
    }

    public Ticket getTicketWithDetails(Long ticketId) {
        return ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket introuvable"));
    }

    public List<Ticket> searchTickets(String search) {
        return ticketRepository.findByTitreContainingIgnoreCaseOrderByDateCreationDesc(search);
    }

    public boolean ticketHasAttachment(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        return ticket != null && ticket.getPieceJointe() != null;
    }

    public void debugTicketAttachments(Long ticketId) {
        Ticket ticket = getTicketByIdWithPieceJointe(ticketId);
        logger.info("=== DEBUG TICKET {} ===", ticketId);
        logger.info("Titre: {}", ticket.getTitre());
        
        logger.info("piecesJointes collection: {}", ticket.getPiecesJointes());
        logger.info("piecesJointes null?: {}", ticket.getPiecesJointes() == null);
        
        if (ticket.getPiecesJointes() != null) {
            logger.info("piecesJointes size: {}", ticket.getPiecesJointes().size());
            logger.info("piecesJointes isEmpty?: {}", ticket.getPiecesJointes().isEmpty());
            
            for (int i = 0; i < ticket.getPiecesJointes().size(); i++) {
                PieceJointe pj = ticket.getPiecesJointes().get(i);
                logger.info("PieceJointe [{}] - ID: {}, Nom: {}, URL: {}, TicketId: {}", 
                    i, pj.getId(), pj.getNomDuFichier(), pj.getUrl(), 
                    pj.getTicket() != null ? pj.getTicket().getId() : "null");
            }
        }
        
        PieceJointe pieceJointe = ticket.getPieceJointe();
        logger.info("getPieceJointe() result: {}", pieceJointe);
        
        if (pieceJointe != null) {
            logger.info("PieceJointe ID: {}", pieceJointe.getId());
            logger.info("Nom du fichier: {}", pieceJointe.getNomDuFichier());
            logger.info("Type: {}", pieceJointe.getTypeDuFichier());
            logger.info("URL: {}", pieceJointe.getUrl());
            logger.info("Taille: {}", pieceJointe.getTaille());
        }
        logger.info("=== FIN DEBUG ===");
    }

    public List<Object[]> getTicketsWithNativeQuery(Long userId) {
        logger.info("Récupération des tickets avec requête native pour l'utilisateur: {}", userId);
        return ticketRepository.findTicketsWithDetailsNative(userId);
    }

    public List<Ticket> searchTicketsFullText(String search) {
        logger.info("Recherche full-text dans les tickets: {}", search);
        if (search == null || search.trim().isEmpty()) {
            return ticketRepository.findAllByOrderByDateCreationDesc();
        }
        return ticketRepository.searchTicketsFullText(search.trim());
    }

    public List<Object[]> getTicketStatisticsByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Récupération des statistiques pour la période: {} - {}", startDate, endDate);
        return ticketRepository.getTicketStatisticsByPeriod(startDate, endDate);
    }

    public List<Object[]> getTicketStatisticsByDepartment(LocalDateTime startDate) {
        logger.info("Récupération des statistiques par département depuis: {}", startDate);
        return ticketRepository.getTicketStatisticsByDepartment(startDate);
    }

    public List<Ticket> findTicketsWithFilters(Statut statut, Priorite priorite, Long departmentId, 
                                              Long assignedToId, LocalDateTime dateDebut, 
                                              LocalDateTime dateFin, String search) {
        logger.info("Recherche de tickets avec filtres: statut={}, priorite={}, dept={}, assignedTo={}, search={}", 
                   statut, priorite, departmentId, assignedToId, search);
        
        return ticketRepository.findTicketsWithFilters(statut, priorite, departmentId, 
                                                      assignedToId, dateDebut, dateFin, search);
    }

    public List<Ticket> getHighPriorityTickets() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return ticketRepository.findHighPriorityTicketsSince(since);
    }

    public List<Ticket> getUnassignedTickets() {
        return ticketRepository.findUnassignedOpenTickets();
    }

    public boolean hasAttachments(Long ticketId) {
        return ticketRepository.countPiecesJointesByTicketId(ticketId) > 0;
    }

    public List<Object[]> getTicketsWithAttachmentStatus(Long userId) {
        return ticketRepository.findTicketsWithAttachmentStatus(userId);
    }

    public List<Ticket> getTicketsByDepartmentWithPieceJointe(Long departmentId) {
        logger.info("Récupération des tickets avec pièces jointes pour le département: {}", departmentId);
        
        List<Ticket> tickets = ticketRepository.findByDepartmentIdWithPiecesJointes(departmentId);
        
        logger.info("Tickets trouvés pour le département {}: {}", departmentId, tickets.size());
        return tickets;
    }

    @Transactional
    public TicketResponseDTO assignTicketToTechnician(Long ticketId, Long technicianId) {
        logger.info("Assignation du ticket {} au technicien {}", ticketId, technicianId);
        
        Ticket ticket = ticketRepository.findByIdWithPiecesJointes(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket introuvable avec l'ID: " + ticketId));
        
        Utilisateur technician = utilisateurRepository.findById(technicianId)
            .orElseThrow(() -> new RuntimeException("Technicien introuvable avec l'ID: " + technicianId));
        
        if (!technician.getRole().name().equals("TECHNICIEN_SUPPORT")) {
            throw new RuntimeException("L'utilisateur n'est pas un technicien");
        }
        
        ticket.setAssignedTo(technician);
        ticket.setStatut(Statut.EN_COURS);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        logger.info("Ticket {} assigné avec succès au technicien {} {}", 
            ticketId, technician.getPrenom(), technician.getNom());
        
        try {
            emailService.sendTicketAssignmentNotificationToTechnician(savedTicket, technician);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de notification d'assignation: ", e);
        }
        
        return ticketMappingService.toResponseDTO(savedTicket);
    }


    public int getTechnicianWorkload(Long technicianId) {
        logger.info("Calcul de la charge de travail pour le technicien: {}", technicianId);
        
        List<Statut> activeStatuts = Arrays.asList(Statut.EN_COURS);
        long workload = ticketRepository.countByAssignedToIdAndStatutIn(technicianId, activeStatuts);
        
        logger.info("Charge de travail du technicien {}: {} tickets EN_COURS", technicianId, workload);
        return (int) workload;
    }

    public List<Ticket> getAssignedTicketsToTechnician(Long technicianId) {
        logger.info("Récupération des tickets assignés au technicien: {}", technicianId);
        
        List<Ticket> tickets = ticketRepository.findByAssignedToIdWithPiecesJointes(technicianId);
        
        logger.info("Tickets assignés trouvés pour le technicien {}: {}", technicianId, tickets.size());
        return tickets;
    }

    public Map<String, Object> getTechnicianWorkloadDetails(Long technicianId) {
        logger.info("Récupération des détails de charge pour le technicien: {}", technicianId);
        
        Map<String, Object> workloadDetails = new HashMap<>();
        
        long enCoursCount = ticketRepository.countByAssignedToIdAndStatutIn(
            technicianId, List.of(Statut.EN_COURS));
        long ouvertCount = ticketRepository.countByAssignedToIdAndStatutIn(
            technicianId, List.of(Statut.OUVERT));
        long resoluCount = ticketRepository.countByAssignedToIdAndStatutIn(
            technicianId, List.of(Statut.RESOLU));
        
        workloadDetails.put("enCours", enCoursCount);
        workloadDetails.put("ouvert", ouvertCount);
        workloadDetails.put("resolu", resoluCount);
        workloadDetails.put("total", enCoursCount + ouvertCount);
        workloadDetails.put("activeWorkload", enCoursCount);
        
        logger.info("Détails charge technicien {}: EN_COURS={}, OUVERT={}, RESOLU={}", 
            technicianId, enCoursCount, ouvertCount, resoluCount);
        
        return workloadDetails;
    }

    @Transactional
    public int autoDispatchTickets(Long departmentId) {
        logger.info("Début de la répartition automatique pour le département: {}", departmentId);
        
        List<Ticket> unassignedTickets = ticketRepository.findUnassignedTicketsByDepartment(departmentId);
        logger.info("Tickets non assignés trouvés: {}", unassignedTickets.size());
        
        if (unassignedTickets.isEmpty()) {
            return 0;
        }
        
        List<Utilisateur> technicians = utilisateurRepository.findByRoleAndDepartment_Id(
            Role.TECHNICIEN_SUPPORT, departmentId);
        logger.info("Techniciens disponibles: {}", technicians.size());
        
        if (technicians.isEmpty()) {
            throw new RuntimeException("Aucun technicien disponible dans ce département");
        }
        
        Map<Long, Long> technicianWorkloads = new HashMap<>();
        for (Utilisateur tech : technicians) {
            long workload = ticketRepository.countByAssignedToIdAndStatutIn(
                tech.getId(), List.of(Statut.EN_COURS));
            technicianWorkloads.put(tech.getId(), workload);
            logger.info("Technicien {} {} - Charge actuelle: {} tickets EN_COURS", 
                tech.getPrenom(), tech.getNom(), workload);
        }
        
        int assignedCount = 0;
        for (Ticket ticket : unassignedTickets) {
            Utilisateur leastBusyTech = technicians.stream()
                .min((t1, t2) -> technicianWorkloads.get(t1.getId())
                    .compareTo(technicianWorkloads.get(t2.getId())))
                .orElse(technicians.get(0));
            
            ticket.setAssignedTo(leastBusyTech);
            ticket.setStatut(Statut.EN_COURS);
            ticketRepository.save(ticket);
            
            technicianWorkloads.put(leastBusyTech.getId(), 
                technicianWorkloads.get(leastBusyTech.getId()) + 1);
            
            assignedCount++;
            logger.info("Ticket {} assigné automatiquement à {} {} (nouvelle charge: {})", 
                ticket.getId(), leastBusyTech.getPrenom(), leastBusyTech.getNom(),
                technicianWorkloads.get(leastBusyTech.getId()));
        }
        
        logger.info("Répartition automatique terminée. {} tickets assignés", assignedCount);
        return assignedCount;
    }

    public Map<String, Object> getDepartmentStatistics(Long departmentId) {
        logger.info("Calcul des statistiques pour le département: {}", departmentId);
        
        Map<String, Object> stats = new HashMap<>();
        
        long totalTickets = ticketRepository.countByDepartmentId(departmentId);
        long openTickets = ticketRepository.countByDepartmentIdAndStatut(departmentId, Statut.OUVERT);
        long inProgressTickets = ticketRepository.countByDepartmentIdAndStatut(departmentId, Statut.EN_COURS);
        long resolvedTickets = ticketRepository.countByDepartmentIdAndStatut(departmentId, Statut.RESOLU);
        long closedTickets = ticketRepository.countByDepartmentIdAndStatut(departmentId, Statut.FERME);
        
        stats.put("totalTickets", totalTickets);
        stats.put("openTickets", openTickets);
        stats.put("inProgressTickets", inProgressTickets);
        stats.put("resolvedTickets", resolvedTickets);
        stats.put("closedTickets", closedTickets);
        
        List<Utilisateur> technicians = utilisateurRepository.findByRoleAndDepartment_Id(
            Role.TECHNICIEN_SUPPORT, departmentId);
        
        List<Map<String, Object>> technicianStats = new ArrayList<>();
        for (Utilisateur tech : technicians) {
            Map<String, Object> techStat = new HashMap<>();
            techStat.put("id", tech.getId());
            techStat.put("nom", tech.getNom());
            techStat.put("prenom", tech.getPrenom());
            
            Map<String, Object> workloadDetails = getTechnicianWorkloadDetails(tech.getId());
            techStat.put("workloadDetails", workloadDetails);
            techStat.put("activeWorkload", workloadDetails.get("activeWorkload"));
            
            technicianStats.add(techStat);
        }
        
        stats.put("technicians", technicianStats);
        
        return stats;
    }
    public Ticket updateStatut(Long ticketId, String statut) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket introuvable"));
        ticket.setStatut(Statut.valueOf(statut));
        if (Statut.valueOf(statut) == Statut.RESOLU) {
            ticket.setDateResolution(LocalDateTime.now());
        }
        return ticketRepository.save(ticket);
    }
}