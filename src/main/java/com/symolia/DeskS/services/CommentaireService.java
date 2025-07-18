package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.Commentaire;
import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.repositories.CommentaireRepository;
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
public class CommentaireService {

    private static final Logger logger = LoggerFactory.getLogger(CommentaireService.class);
    
    private final CommentaireRepository commentaireRepository;
    private final TicketRepository ticketRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    public Commentaire createCommentaire(Long ticketId, Long auteurId, String contenu) {
        logger.info("Création d'un commentaire pour le ticket {} par l'utilisateur {}", ticketId, auteurId);
        
        // Vérifier que le ticket existe
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket introuvable avec l'ID: " + ticketId));
        
        // Vérifier que l'auteur existe
        Utilisateur auteur = utilisateurRepository.findById(auteurId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID: " + auteurId));
        
        // Créer le commentaire
        Commentaire commentaire = new Commentaire();
        commentaire.setCommentaire(contenu);
        commentaire.setTicket(ticket);
        commentaire.setAuteur(auteur);
        commentaire.setDateCreation(LocalDateTime.now());
        commentaire.setModifie(false);
        
        Commentaire savedCommentaire = commentaireRepository.save(commentaire);
        
        // Envoyer des notifications
        try {
            sendCommentNotifications(savedCommentaire);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi des notifications pour le commentaire {}: ", savedCommentaire.getId(), e);
        }
        
        logger.info("Commentaire créé avec succès - ID: {}", savedCommentaire.getId());
        return savedCommentaire;
    }

    public List<Commentaire> getCommentairesByTicketId(Long ticketId) {
        logger.info("Récupération des commentaires pour le ticket: {}", ticketId);
        List<Commentaire> commentaires = commentaireRepository.findByTicketIdWithAuthor(ticketId);
        logger.info("Commentaires trouvés: {}", commentaires.size());
        return commentaires;
    }

    public Optional<Commentaire> getCommentaireById(Long id) {
        logger.info("Récupération du commentaire: {}", id);
        return commentaireRepository.findById(id);
    }

    public Commentaire updateCommentaire(Long commentaireId, Long auteurId, String nouveauContenu) {
        logger.info("Mise à jour du commentaire {} par l'utilisateur {}", commentaireId, auteurId);
        
        Commentaire commentaire = commentaireRepository.findById(commentaireId)
            .orElseThrow(() -> new RuntimeException("Commentaire introuvable avec l'ID: " + commentaireId));
        
        // Vérifier que l'auteur est le même
        if (!commentaire.getAuteur().getId().equals(auteurId)) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres commentaires");
        }
        
        // Vérifier que le commentaire n'est pas trop ancien (par exemple, 24h)
        if (commentaire.getDateCreation().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new RuntimeException("Vous ne pouvez plus modifier ce commentaire (trop ancien)");
        }
        
        commentaire.setCommentaire(nouveauContenu);
        commentaire.setDateModification(LocalDateTime.now());
        commentaire.setModifie(true);
        
        Commentaire updatedCommentaire = commentaireRepository.save(commentaire);
        logger.info("Commentaire mis à jour avec succès - ID: {}", updatedCommentaire.getId());
        
        return updatedCommentaire;
    }

    public void deleteCommentaire(Long commentaireId, Long auteurId) {
        logger.info("Suppression du commentaire {} par l'utilisateur {}", commentaireId, auteurId);
        
        Commentaire commentaire = commentaireRepository.findById(commentaireId)
            .orElseThrow(() -> new RuntimeException("Commentaire introuvable avec l'ID: " + commentaireId));
        
        // Vérifier que l'auteur est le même
        if (!commentaire.getAuteur().getId().equals(auteurId)) {
            throw new RuntimeException("Vous ne pouvez supprimer que vos propres commentaires");
        }
        
        // Vérifier que le commentaire n'est pas trop ancien (par exemple, 1h)
        if (commentaire.getDateCreation().isBefore(LocalDateTime.now().minusHours(1))) {
            throw new RuntimeException("Vous ne pouvez plus supprimer ce commentaire (trop ancien)");
        }
        
        commentaireRepository.delete(commentaire);
        logger.info("Commentaire supprimé avec succès - ID: {}", commentaireId);
    }

    public long countCommentairesByTicket(Long ticketId) {
        return commentaireRepository.countByTicketId(ticketId);
    }

    public List<Commentaire> getCommentairesByAuteur(Long auteurId) {
        return commentaireRepository.findByAuteurIdOrderByDateDesc(auteurId);
    }

    public boolean canUserModifyComment(Long commentaireId, Long userId) {
        Optional<Commentaire> commentaire = commentaireRepository.findById(commentaireId);
        if (commentaire.isEmpty()) {
            return false;
        }
        
        return commentaire.get().getAuteur().getId().equals(userId) &&
               commentaire.get().getDateCreation().isAfter(LocalDateTime.now().minusHours(24));
    }

    public boolean canUserDeleteComment(Long commentaireId, Long userId) {
        Optional<Commentaire> commentaire = commentaireRepository.findById(commentaireId);
        if (commentaire.isEmpty()) {
            return false;
        }
        
        return commentaire.get().getAuteur().getId().equals(userId) &&
               commentaire.get().getDateCreation().isAfter(LocalDateTime.now().minusHours(1));
    }

    private void sendCommentNotifications(Commentaire commentaire) {
        try {
            Ticket ticket = commentaire.getTicket();
            
            // Notifier le créateur du ticket (s'il n'est pas l'auteur du commentaire)
            if (!ticket.getUser().getId().equals(commentaire.getAuteur().getId())) {
                emailService.sendCommentNotificationToCreator(commentaire);
            }
            
            // Notifier le technicien assigné (s'il existe et n'est pas l'auteur)
            if (ticket.getAssignedTo() != null && 
                !ticket.getAssignedTo().getId().equals(commentaire.getAuteur().getId())) {
                emailService.sendCommentNotificationToTechnician(commentaire);
            }
            
            // Notifier le technicien délégué (s'il existe et n'est pas l'auteur)
            if (ticket.getDelegatedTo() != null && 
                !ticket.getDelegatedTo().getId().equals(commentaire.getAuteur().getId())) {
                emailService.sendCommentNotificationToTechnician(commentaire);
            }
            
            // Notifier les managers du département (sauf l'auteur)
            emailService.sendCommentNotificationToManagers(commentaire);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi des notifications pour le commentaire: ", e);
        }
    }
}