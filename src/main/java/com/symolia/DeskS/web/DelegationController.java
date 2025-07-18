package com.symolia.DeskS.web;

import com.symolia.DeskS.dtos.DelegationRequestDTO;
import com.symolia.DeskS.dtos.DelegationResponseDTO;
import com.symolia.DeskS.services.DelegationService;
import com.symolia.DeskS.services.CustomUserDetailsService;
import com.symolia.DeskS.entities.Utilisateur;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delegations")
@RequiredArgsConstructor
public class DelegationController {
    
    private static final Logger logger = LoggerFactory.getLogger(DelegationController.class);
    
    private final DelegationService delegationService;
    private final CustomUserDetailsService customUserDetailsService;
    
    @PostMapping("/request")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIEN_SUPPORT')")
    public ResponseEntity<Map<String, Object>> createDelegationRequest(
            @Valid @RequestBody DelegationRequestDTO requestDTO,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            logger.info("Création d'une demande de délégation par {}", username);
            
            Long technicianId = getCurrentUserId(authentication);
            
            DelegationResponseDTO delegation = delegationService.createDelegationRequest(requestDTO, technicianId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Demande de délégation créée avec succès");
            response.put("delegation", delegation);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la création de la demande de délégation: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/{delegationId}/approve")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> approveDelegation(
            @PathVariable Long delegationId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        try {
            String commentaire = request != null ? request.get("commentaire") : "";
            Long managerId = getCurrentUserId(authentication);
            
            logger.info("Tentative d'approbation de la délégation {} par le manager {}", delegationId, managerId);
            
            DelegationResponseDTO delegation = delegationService.approveDelegation(delegationId, managerId, commentaire);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Délégation approuvée avec succès");
            response.put("delegation", delegation);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de l'approbation de la délégation: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/{delegationId}/reject")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<Map<String, Object>> rejectDelegation(
            @PathVariable Long delegationId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String commentaire = request.get("commentaire");
            if (commentaire == null || commentaire.trim().isEmpty()) {
                throw new RuntimeException("Un commentaire est requis pour le refus");
            }
            
            Long managerId = getCurrentUserId(authentication);
            
            logger.info("Tentative de refus de la délégation {} par le manager {}", delegationId, managerId);
            
            DelegationResponseDTO delegation = delegationService.rejectDelegation(delegationId, managerId, commentaire);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Délégation refusée avec succès");
            response.put("delegation", delegation);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors du refus de la délégation: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/my-requests")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIEN_SUPPORT')")
    public ResponseEntity<List<DelegationResponseDTO>> getMyDelegationRequests(Authentication authentication) {
        try {
            Long technicianId = getCurrentUserId(authentication);
            List<DelegationResponseDTO> delegations = delegationService.getDelegationsByFromTechnician(technicianId);
            return ResponseEntity.ok(delegations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des demandes de délégation: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/received")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIEN_SUPPORT')")
    public ResponseEntity<List<DelegationResponseDTO>> getReceivedDelegations(Authentication authentication) {
        try {
            Long technicianId = getCurrentUserId(authentication);
            List<DelegationResponseDTO> delegations = delegationService.getDelegationsByToTechnician(technicianId);
            return ResponseEntity.ok(delegations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des délégations reçues: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<List<DelegationResponseDTO>> getPendingDelegations(Authentication authentication) {
        try {
            Long managerId = getCurrentUserId(authentication);
            logger.info("Récupération des délégations en attente pour le manager {}", managerId);
            
            List<DelegationResponseDTO> delegations = delegationService.getPendingDelegationsForManager(managerId);
            
            logger.info("Délégations en attente trouvées: {}", delegations.size());
            return ResponseEntity.ok(delegations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des délégations en attente: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT')")
    public ResponseEntity<List<DelegationResponseDTO>> getAllDelegations(Authentication authentication) {
        try {
            Long managerId = getCurrentUserId(authentication);
            logger.info("Récupération de toutes les délégations pour le manager {}", managerId);
            
            List<DelegationResponseDTO> delegations = delegationService.getAllDelegationsForManager(managerId);
            
            logger.info("Délégations trouvées: {}", delegations.size());
            return ResponseEntity.ok(delegations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des délégations: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAuthority('ROLE_MANAGER_IT') or hasAuthority('ROLE_TECHNICIEN_SUPPORT')")
    public ResponseEntity<List<DelegationResponseDTO>> getDelegationsByTicket(@PathVariable Long ticketId) {
        try {
            List<DelegationResponseDTO> delegations = delegationService.getDelegationsByTicket(ticketId);
            return ResponseEntity.ok(delegations);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des délégations du ticket: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    

    private Long getCurrentUserId(Authentication authentication) {
        try {
            String email = authentication.getName();
            logger.debug("Récupération de l'ID utilisateur pour l'email: {}", email);
            
            Utilisateur utilisateur = customUserDetailsService.findByEmail(email);
            
            if (utilisateur == null) {
                logger.error("Utilisateur non trouvé pour l'email: {}", email);
                throw new RuntimeException("Utilisateur non trouvé");
            }
            
            logger.debug("ID utilisateur trouvé: {} pour l'email: {}", utilisateur.getId(), email);
            return utilisateur.getId();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'ID utilisateur: ", e);
            throw new RuntimeException("Erreur lors de la récupération de l'utilisateur connecté");
        }
    }
}