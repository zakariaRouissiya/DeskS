package com.symolia.DeskS.services;

import com.symolia.DeskS.dtos.DelegationRequestDTO;
import com.symolia.DeskS.dtos.DelegationResponseDTO;
import com.symolia.DeskS.entities.Delegation;
import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.enums.StatutDelegation;
import com.symolia.DeskS.enums.Statut;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.repositories.DelegationRepository;
import com.symolia.DeskS.repositories.TicketRepository;
import com.symolia.DeskS.repositories.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DelegationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DelegationService.class);
    
    private final DelegationRepository delegationRepository;
    private final TicketRepository ticketRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService; 
    
    @Transactional
    public DelegationResponseDTO createDelegationRequest(DelegationRequestDTO requestDTO, Long fromTechnicianId) {
        logger.info("Création d'une demande de délégation pour le ticket {} par le technicien {}", 
                   requestDTO.getTicketId(), fromTechnicianId);
        
        Ticket ticket = ticketRepository.findById(requestDTO.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket non trouvé"));
        
        if (!ticket.getAssignedTo().getId().equals(fromTechnicianId)) {
            throw new RuntimeException("Vous ne pouvez déléguer que les tickets qui vous sont assignés");
        }
        
        Optional<Delegation> existingPendingDelegation = delegationRepository
                .findPendingDelegationByTicketId(requestDTO.getTicketId());
        
        if (existingPendingDelegation.isPresent()) {
            throw new RuntimeException("Une demande de délégation est déjà en attente pour ce ticket");
        }
        
        Utilisateur fromTechnician = utilisateurRepository.findById(fromTechnicianId)
                .orElseThrow(() -> new RuntimeException("Technicien demandeur non trouvé"));
        
        Utilisateur toTechnician = utilisateurRepository.findById(requestDTO.getToTechnicianId())
                .orElseThrow(() -> new RuntimeException("Technicien destinataire non trouvé"));
        
        if (!fromTechnician.getDepartment().getId().equals(toTechnician.getDepartment().getId())) {
            throw new RuntimeException("La délégation n'est possible qu'entre techniciens du même département");
        }
        
        Delegation delegation = Delegation.builder()
                .ticket(ticket)
                .fromTechnician(fromTechnician)
                .toTechnician(toTechnician)
                .justification(requestDTO.getJustification())
                .statut(StatutDelegation.EN_ATTENTE)
                .build();
        
        delegation = delegationRepository.save(delegation);
        
        logger.info("Demande de délégation créée avec succès, ID: {}", delegation.getId());
        
        try {
            emailService.sendDelegationRequestNotificationToManager(delegation);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de notification de demande de délégation: ", e);
        }
        
        return mapToResponseDTO(delegation);
    }
    
    @Transactional
    public DelegationResponseDTO approveDelegation(Long delegationId, Long managerId, String commentaire) {
        logger.info("Approbation de la délégation {} par le manager {}", delegationId, managerId);
        
        Delegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new RuntimeException("Délégation non trouvée"));
        
        if (delegation.getStatut() != StatutDelegation.EN_ATTENTE) {
            throw new RuntimeException("Cette délégation a déjà été traitée");
        }
        
        Utilisateur manager = utilisateurRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager non trouvé"));
        
        validateManagerPermission(manager, delegation);
        
        delegation.setStatut(StatutDelegation.APPROUVE);
        delegation.setDateReponse(LocalDateTime.now());
        delegation.setCommentaireReponse(commentaire);
        delegation.setApprovedBy(manager);
        
        Ticket ticket = delegation.getTicket();
        ticket.setAssignedTo(delegation.getToTechnician());
        ticket.setDelegatedTo(delegation.getToTechnician());
        ticket.setStatut(Statut.EN_COURS);
        
        ticketRepository.save(ticket);
        delegation = delegationRepository.save(delegation);
        
        logger.info("Délégation approuvée avec succès");
        
        try {
            emailService.sendDelegationResponseNotificationToRequester(delegation);
            
            emailService.sendTicketDelegatedNotificationToTechnician(delegation);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi des notifications de délégation approuvée: ", e);
        }
        
        return mapToResponseDTO(delegation);
    }
    
    @Transactional
    public DelegationResponseDTO rejectDelegation(Long delegationId, Long managerId, String commentaire) {
        logger.info("Refus de la délégation {} par le manager {}", delegationId, managerId);
        
        Delegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new RuntimeException("Délégation non trouvée"));
        
        if (delegation.getStatut() != StatutDelegation.EN_ATTENTE) {
            throw new RuntimeException("Cette délégation a déjà été traitée");
        }
        
        Utilisateur manager = utilisateurRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager non trouvé"));
        
        validateManagerPermission(manager, delegation);
        
        delegation.setStatut(StatutDelegation.REFUSE);
        delegation.setDateReponse(LocalDateTime.now());
        delegation.setCommentaireReponse(commentaire);
        delegation.setApprovedBy(manager);
        
        delegation = delegationRepository.save(delegation);
        
        logger.info("Délégation refusée avec succès");
        
        try {
            emailService.sendDelegationResponseNotificationToRequester(delegation);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de notification de délégation refusée: ", e);
        }
        
        return mapToResponseDTO(delegation);
    }
    
    private void validateManagerPermission(Utilisateur manager, Delegation delegation) {
        if (!manager.getRole().equals(Role.MANAGER_IT)) {
            throw new RuntimeException("Seuls les managers peuvent traiter les délégations");
        }
        
        if (manager.getDepartment() == null) {
            throw new RuntimeException("Le manager n'a pas de département assigné");
        }
        
        Ticket ticket = delegation.getTicket();
        if (ticket.getDepartment() == null) {
            throw new RuntimeException("Le ticket n'a pas de département assigné");
        }
        
        if (!ticket.getDepartment().getId().equals(manager.getDepartment().getId())) {
            throw new RuntimeException("Vous ne pouvez traiter que les délégations de votre département");
        }
        
        logger.info("Validation des permissions réussie pour le manager {} sur le département {}", 
                   manager.getId(), manager.getDepartment().getNom());
    }
    
    public List<DelegationResponseDTO> getDelegationsByFromTechnician(Long technicianId) {
        List<Delegation> delegations = delegationRepository.findByFromTechnicianId(technicianId);
        return delegations.stream().map(this::mapToResponseDTO).toList();
    }
    
    public List<DelegationResponseDTO> getDelegationsByToTechnician(Long technicianId) {
        List<Delegation> delegations = delegationRepository.findByToTechnicianId(technicianId);
        return delegations.stream().map(this::mapToResponseDTO).toList();
    }
    
    public List<DelegationResponseDTO> getPendingDelegationsForManager(Long managerId) {
        logger.info("Récupération des délégations en attente pour le manager {}", managerId);
        
        Utilisateur manager = utilisateurRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager non trouvé"));
        
        if (!manager.getRole().equals(Role.MANAGER_IT)) {
            throw new RuntimeException("Seuls les managers peuvent accéder aux délégations");
        }
        
        if (manager.getDepartment() == null) {
            throw new RuntimeException("Le manager n'a pas de département assigné");
        }
        
        Long departmentId = manager.getDepartment().getId();
        logger.info("Récupération des délégations en attente pour le département {}", departmentId);
        
        List<Delegation> delegations = delegationRepository.findPendingDelegationsByDepartment(departmentId);
        
        logger.info("Nombre de délégations en attente trouvées: {}", delegations.size());
        
        return delegations.stream().map(this::mapToResponseDTO).toList();
    }
    
    public List<DelegationResponseDTO> getAllDelegationsForManager(Long managerId) {
        logger.info("Récupération de toutes les délégations pour le manager {}", managerId);
        
        Utilisateur manager = utilisateurRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager non trouvé"));
        
        if (!manager.getRole().equals(Role.MANAGER_IT)) {
            throw new RuntimeException("Seuls les managers peuvent accéder aux délégations");
        }
        
        if (manager.getDepartment() == null) {
            throw new RuntimeException("Le manager n'a pas de département assigné");
        }
        
        Long departmentId = manager.getDepartment().getId();
        logger.info("Récupération de toutes les délégations pour le département {}", departmentId);
        
        List<Delegation> delegations = delegationRepository.findAllDelegationsByDepartment(departmentId);
        
        logger.info("Nombre de délégations trouvées: {}", delegations.size());
        
        return delegations.stream().map(this::mapToResponseDTO).toList();
    }
    
    
    public List<DelegationResponseDTO> getDelegationsByTicket(Long ticketId) {
        List<Delegation> delegations = delegationRepository.findByTicketId(ticketId);
        return delegations.stream().map(this::mapToResponseDTO).toList();
    }
    
    private DelegationResponseDTO mapToResponseDTO(Delegation delegation) {
        DelegationResponseDTO dto = new DelegationResponseDTO();
        dto.setId(delegation.getId());
        dto.setTicketId(delegation.getTicket().getId());
        dto.setTicketTitre(delegation.getTicket().getTitre());
        dto.setJustification(delegation.getJustification());
        dto.setStatut(delegation.getStatut());
        dto.setDateDemande(delegation.getDateDemande());
        dto.setDateReponse(delegation.getDateReponse());
        dto.setCommentaireReponse(delegation.getCommentaireReponse());
        
        DelegationResponseDTO.UserDTO fromTechnicianDTO = new DelegationResponseDTO.UserDTO();
        fromTechnicianDTO.setId(delegation.getFromTechnician().getId());
        fromTechnicianDTO.setNom(delegation.getFromTechnician().getNom());
        fromTechnicianDTO.setPrenom(delegation.getFromTechnician().getPrenom());
        fromTechnicianDTO.setEmail(delegation.getFromTechnician().getEmail());
        fromTechnicianDTO.setDepartmentNom(delegation.getFromTechnician().getDepartment().getNom());
        dto.setFromTechnician(fromTechnicianDTO);
        
        DelegationResponseDTO.UserDTO toTechnicianDTO = new DelegationResponseDTO.UserDTO();
        toTechnicianDTO.setId(delegation.getToTechnician().getId());
        toTechnicianDTO.setNom(delegation.getToTechnician().getNom());
        toTechnicianDTO.setPrenom(delegation.getToTechnician().getPrenom());
        toTechnicianDTO.setEmail(delegation.getToTechnician().getEmail());
        toTechnicianDTO.setDepartmentNom(delegation.getToTechnician().getDepartment().getNom());
        dto.setToTechnician(toTechnicianDTO);
        
        if (delegation.getApprovedBy() != null) {
            DelegationResponseDTO.UserDTO approvedByDTO = new DelegationResponseDTO.UserDTO();
            approvedByDTO.setId(delegation.getApprovedBy().getId());
            approvedByDTO.setNom(delegation.getApprovedBy().getNom());
            approvedByDTO.setPrenom(delegation.getApprovedBy().getPrenom());
            approvedByDTO.setEmail(delegation.getApprovedBy().getEmail());
            approvedByDTO.setDepartmentNom(delegation.getApprovedBy().getDepartment().getNom());
            dto.setApprovedBy(approvedByDTO);
        }
        
        return dto;
    }
}