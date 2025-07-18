package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.entities.Utilisateur;
import com.symolia.DeskS.entities.Delegation;
import com.symolia.DeskS.entities.Commentaire;
import com.symolia.DeskS.enums.Role;
import com.symolia.DeskS.repositories.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final UtilisateurRepository utilisateurRepository;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // ========== MÉTHODES DE TICKET ==========

    @Async
    public CompletableFuture<Void> sendTicketCreationNotificationToManagers(Ticket ticket) {
        logger.info("Envoi de notification de création de ticket {} aux managers du département {}", 
                   ticket.getId(), ticket.getDepartment().getId());
        
        try {
            List<Utilisateur> managers = utilisateurRepository.findByRoleAndDepartment_Id(
                Role.MANAGER_IT, ticket.getDepartment().getId());
            
            if (managers.isEmpty()) {
                logger.warn("Aucun manager trouvé pour le département {}", ticket.getDepartment().getId());
                return CompletableFuture.completedFuture(null);
            }
            
            for (Utilisateur manager : managers) {
                try {
                    sendTicketCreationEmail(ticket, manager);
                    logger.info("Email de création de ticket envoyé à: {}", manager.getEmail());
                } catch (Exception e) {
                    logger.error("Erreur lors de l'envoi de l'email de création de ticket à {}: ", 
                               manager.getEmail(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi des notifications de création de ticket: ", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> sendTicketAssignmentNotificationToTechnician(Ticket ticket, Utilisateur technician) {
        logger.info("Envoi de notification d'assignation de ticket {} au technicien {}", 
                   ticket.getId(), technician.getEmail());
        
        try {
            sendTicketAssignmentEmail(ticket, technician);
            logger.info("Email d'assignation de ticket envoyé à: {}", technician.getEmail());
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email d'assignation à {}: ", 
                       technician.getEmail(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    // ========== MÉTHODES DE DÉLÉGATION ==========

    @Async
    public CompletableFuture<Void> sendDelegationRequestNotificationToManager(Delegation delegation) {
        logger.info("Envoi de notification de demande de délégation {} au manager", delegation.getId());
        
        try {
            List<Utilisateur> managers = utilisateurRepository.findByRoleAndDepartment_Id(
                Role.MANAGER_IT, delegation.getTicket().getDepartment().getId());
            
            if (managers.isEmpty()) {
                logger.warn("Aucun manager trouvé pour le département {}", 
                           delegation.getTicket().getDepartment().getId());
                return CompletableFuture.completedFuture(null);
            }
            
            for (Utilisateur manager : managers) {
                try {
                    sendDelegationRequestEmail(delegation, manager);
                    logger.info("Email de demande de délégation envoyé à: {}", manager.getEmail());
                } catch (Exception e) {
                    logger.error("Erreur lors de l'envoi de l'email de délégation à {}: ", 
                               manager.getEmail(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi des notifications de délégation: ", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> sendDelegationResponseNotificationToRequester(Delegation delegation) {
        logger.info("Envoi de notification de réponse de délégation {} au technicien demandeur", 
                   delegation.getId());
        
        try {
            sendDelegationResponseEmail(delegation);
            logger.info("Email de réponse de délégation envoyé à: {}", 
                       delegation.getFromTechnician().getEmail());
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de réponse de délégation à {}: ", 
                       delegation.getFromTechnician().getEmail(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> sendTicketDelegatedNotificationToTechnician(Delegation delegation) {
        logger.info("Envoi de notification de délégation acceptée au technicien destinataire {}", 
                   delegation.getToTechnician().getEmail());
        
        try {
            sendTicketDelegatedEmail(delegation);
            logger.info("Email de délégation acceptée envoyé à: {}", 
                       delegation.getToTechnician().getEmail());
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de délégation acceptée à {}: ", 
                       delegation.getToTechnician().getEmail(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    // ========== MÉTHODES DE COMMENTAIRE ==========

    @Async
    public CompletableFuture<Void> sendCommentNotificationToCreator(Commentaire commentaire) {
        logger.info("Envoi de notification de commentaire au créateur du ticket {}", 
                   commentaire.getTicket().getId());
        
        try {
            sendCommentCreatorEmail(commentaire);
            logger.info("Email de commentaire envoyé au créateur: {}", 
                       commentaire.getTicket().getUser().getEmail());
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de commentaire au créateur {}: ", 
                       commentaire.getTicket().getUser().getEmail(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> sendCommentNotificationToTechnician(Commentaire commentaire) {
        logger.info("Envoi de notification de commentaire au technicien responsable du ticket {}", 
                   commentaire.getTicket().getId());
        
        try {
            // Déterminer le technicien responsable (délégué ou assigné)
            Utilisateur responsable = commentaire.getTicket().getDelegatedTo() != null ? 
                                    commentaire.getTicket().getDelegatedTo() : 
                                    commentaire.getTicket().getAssignedTo();
            
            if (responsable != null) {
                sendCommentTechnicianEmail(commentaire, responsable);
                logger.info("Email de commentaire envoyé au technicien: {}", responsable.getEmail());
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de commentaire au technicien: ", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> sendCommentNotificationToManagers(Commentaire commentaire) {
        logger.info("Envoi de notification de commentaire aux managers du département {}", 
                   commentaire.getTicket().getDepartment().getId());
        
        try {
            List<Utilisateur> managers = utilisateurRepository.findByRoleAndDepartment_Id(
                Role.MANAGER_IT, commentaire.getTicket().getDepartment().getId());
            
            if (managers.isEmpty()) {
                logger.warn("Aucun manager trouvé pour le département {}", 
                           commentaire.getTicket().getDepartment().getId());
                return CompletableFuture.completedFuture(null);
            }
            
            for (Utilisateur manager : managers) {
                try {
                    // Ne pas envoyer à l'auteur du commentaire
                    if (!manager.getId().equals(commentaire.getAuteur().getId())) {
                        sendCommentManagerEmail(commentaire, manager);
                        logger.info("Email de commentaire envoyé au manager: {}", manager.getEmail());
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors de l'envoi de l'email de commentaire au manager {}: ", 
                               manager.getEmail(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi des notifications de commentaire aux managers: ", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    // ========== MÉTHODES PRIVÉES D'ENVOI D'EMAIL ==========

    private void sendTicketCreationEmail(Ticket ticket, Utilisateur manager) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(manager.getEmail());
        helper.setSubject("Nouveau ticket créé - #" + ticket.getId());
        helper.setText(createTicketCreationEmailTemplate(ticket, manager), true);
        
        mailSender.send(message);
    }

    private void sendTicketAssignmentEmail(Ticket ticket, Utilisateur technician) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(technician.getEmail());
        helper.setSubject("Ticket assigné - #" + ticket.getId() + " - " + ticket.getTitre());
        helper.setText(createTicketAssignmentEmailTemplate(ticket, technician), true);
        
        mailSender.send(message);
    }

    private void sendDelegationRequestEmail(Delegation delegation, Utilisateur manager) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(manager.getEmail());
        helper.setSubject("Demande de délégation - Ticket #" + delegation.getTicket().getId());
        helper.setText(createDelegationRequestEmailTemplate(delegation, manager), true);
        
        mailSender.send(message);
    }

    private void sendDelegationResponseEmail(Delegation delegation) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(delegation.getFromTechnician().getEmail());
        helper.setSubject("Réponse à votre demande de délégation - Ticket #" + 
                        delegation.getTicket().getId());
        helper.setText(createDelegationResponseEmailTemplate(delegation), true);
        
        mailSender.send(message);
    }

    private void sendTicketDelegatedEmail(Delegation delegation) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(delegation.getToTechnician().getEmail());
        helper.setSubject("Ticket délégué - #" + delegation.getTicket().getId() + 
                        " - " + delegation.getTicket().getTitre());
        helper.setText(createTicketDelegatedEmailTemplate(delegation), true);
        
        mailSender.send(message);
    }

    private void sendCommentCreatorEmail(Commentaire commentaire) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(commentaire.getTicket().getUser().getEmail());
        helper.setSubject("Nouveau commentaire sur votre ticket #" + commentaire.getTicket().getId());
        helper.setText(createCommentCreatorEmailTemplate(commentaire), true);
        
        mailSender.send(message);
    }

    private void sendCommentTechnicianEmail(Commentaire commentaire, Utilisateur technician) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(technician.getEmail());
        helper.setSubject("Nouveau commentaire sur le ticket #" + commentaire.getTicket().getId());
        helper.setText(createCommentTechnicianEmailTemplate(commentaire, technician), true);
        
        mailSender.send(message);
    }

    private void sendCommentManagerEmail(Commentaire commentaire, Utilisateur manager) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(manager.getEmail());
        helper.setSubject("Nouveau commentaire sur le ticket #" + commentaire.getTicket().getId());
        helper.setText(createCommentManagerEmailTemplate(commentaire, manager), true);
        
        mailSender.send(message);
    }

    // ========== TEMPLATES EMAIL ==========

    private String createTicketCreationEmailTemplate(Ticket ticket, Utilisateur manager) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Nouveau ticket créé</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                    <h2 style="color: #4461e6; margin-bottom: 20px;">Nouveau ticket créé</h2>
                    
                    <p>Bonjour %s,</p>
                    
                    <p>Un nouveau ticket a été créé dans votre département :</p>
                    
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="color: #4461e6; margin-top: 0;">Ticket #%s</h3>
                        <p><strong>Titre :</strong> %s</p>
                        <p><strong>Créé par :</strong> %s %s</p>
                        <p><strong>Date :</strong> %s</p>
                        <p><strong>Priorité :</strong> %s</p>
                        <p><strong>Type :</strong> %s</p>
                        <p><strong>Description :</strong> %s</p>
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="%s/tickets" style="background-color: #4461e6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Voir le ticket
                        </a>
                    </p>
                </div>
            </body>
            </html>
            """, 
            manager.getPrenom(),
            ticket.getId(),
            ticket.getTitre(),
            ticket.getUser().getPrenom(),
            ticket.getUser().getNom(),
            ticket.getDateCreation().format(formatter),
            ticket.getPriorite().name(),
            ticket.getType(),
            ticket.getDescription(),
            frontendUrl
        );
    }

    private String createTicketAssignmentEmailTemplate(Ticket ticket, Utilisateur technician) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Ticket assigné</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                    <h2 style="color: #38a169; margin-bottom: 20px;">Ticket assigné</h2>
                    
                    <p>Bonjour %s,</p>
                    
                    <p>Un ticket vous a été assigné et est maintenant en cours :</p>
                    
                    <div style="background-color: #f0fff4; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #38a169;">
                        <h3 style="color: #38a169; margin-top: 0;">Ticket #%s</h3>
                        <p><strong>Titre :</strong> %s</p>
                        <p><strong>Créé par :</strong> %s %s</p>
                        <p><strong>Date de création :</strong> %s</p>
                        <p><strong>Priorité :</strong> %s</p>
                        <p><strong>Type :</strong> %s</p>
                        <p><strong>Description :</strong> %s</p>
                    </div>
                    
                    <div style="background-color: #e6fffa; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="margin-top: 0;">Prochaines étapes :</h4>
                        <ul>
                            <li>Analyser le problème décrit</li>
                            <li>Contacter l'utilisateur si nécessaire</li>
                            <li>Mettre à jour le statut du ticket</li>
                        </ul>
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="%s/tickets/%s" style="background-color: #38a169; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Traiter le ticket
                        </a>
                    </p>
                </div>
            </body>
            </html>
            """, 
            technician.getPrenom(),
            ticket.getId(),
            ticket.getTitre(),
            ticket.getUser().getPrenom(),
            ticket.getUser().getNom(),
            ticket.getDateCreation().format(formatter),
            ticket.getPriorite().name(),
            ticket.getType(),
            ticket.getDescription(),
            frontendUrl,
            ticket.getId()
        );
    }

    private String createDelegationRequestEmailTemplate(Delegation delegation, Utilisateur manager) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Demande de délégation</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                    <h2 style="color: #ed8936; margin-bottom: 20px;">Demande de délégation</h2>
                    
                    <p>Bonjour %s,</p>
                    
                    <p>Une demande de délégation nécessite votre approbation :</p>
                    
                    <div style="background-color: #fffaf0; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #ed8936;">
                        <h3 style="color: #ed8936; margin-top: 0;">Ticket #%s - %s</h3>
                        <p><strong>Demandé par :</strong> %s %s</p>
                        <p><strong>Délégué vers :</strong> %s %s</p>
                        <p><strong>Date de demande :</strong> %s</p>
                        <p><strong>Justification :</strong> %s</p>
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="%s/delegations" style="background-color: #ed8936; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Gérer la délégation
                        </a>
                    </p>
                </div>
            </body>
            </html>
            """, 
            manager.getPrenom(),
            delegation.getTicket().getId(),
            delegation.getTicket().getTitre(),
            delegation.getFromTechnician().getPrenom(),
            delegation.getFromTechnician().getNom(),
            delegation.getToTechnician().getPrenom(),
            delegation.getToTechnician().getNom(),
            delegation.getDateDemande().format(formatter),
            delegation.getJustification(),
            frontendUrl
        );
    }

    private String createDelegationResponseEmailTemplate(Delegation delegation) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        boolean isApproved = delegation.getStatut().name().equals("APPROUVE");
        String statusColor = isApproved ? "#38a169" : "#e53e3e";
        String statusText = isApproved ? "approuvée" : "refusée";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Réponse délégation</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                    <h2 style="color: %s; margin-bottom: 20px;">Demande de délégation %s</h2>
                    
                    <p>Bonjour %s,</p>
                    
                    <p>Votre demande de délégation a été <strong>%s</strong> :</p>
                    
                    <div style="background-color: %s; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid %s;">
                        <h3 style="color: %s; margin-top: 0;">Ticket #%s - %s</h3>
                        <p><strong>Délégué vers :</strong> %s %s</p>
                        <p><strong>Traité par :</strong> %s %s</p>
                        <p><strong>Date de réponse :</strong> %s</p>
                        %s
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="%s/tickets/%s" style="background-color: %s; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Voir le ticket
                        </a>
                    </p>
                </div>
            </body>
            </html>
            """, 
            statusColor,
            statusText,
            delegation.getFromTechnician().getPrenom(),
            statusText,
            isApproved ? "#f0fff4" : "#fed7d7",
            statusColor,
            statusColor,
            delegation.getTicket().getId(),
            delegation.getTicket().getTitre(),
            delegation.getToTechnician().getPrenom(),
            delegation.getToTechnician().getNom(),
            delegation.getApprovedBy().getPrenom(),
            delegation.getApprovedBy().getNom(),
            delegation.getDateReponse().format(formatter),
            delegation.getCommentaireReponse() != null ? 
                String.format("<p><strong>Commentaire :</strong> %s</p>", delegation.getCommentaireReponse()) : "",
            frontendUrl,
            delegation.getTicket().getId(),
            statusColor
        );
    }

    private String createTicketDelegatedEmailTemplate(Delegation delegation) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Ticket délégué</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                    <h2 style="color: #667eea; margin-bottom: 20px;">Ticket délégué</h2>
                    
                    <p>Bonjour %s,</p>
                    
                    <p>Un ticket vous a été délégué par votre collègue :</p>
                    
                    <div style="background-color: #f7faff; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #667eea;">
                        <h3 style="color: #667eea; margin-top: 0;">Ticket #%s - %s</h3>
                        <p><strong>Délégué par :</strong> %s %s</p>
                        <p><strong>Créé par :</strong> %s %s</p>
                        <p><strong>Priorité :</strong> %s</p>
                        <p><strong>Type :</strong> %s</p>
                        <p><strong>Description :</strong> %s</p>
                        <p><strong>Justification :</strong> %s</p>
                    </div>
                    
                    <div style="background-color: #e6fffa; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="margin-top: 0;">Informations importantes :</h4>
                        <ul>
                            <li>Ce ticket vous a été délégué par %s %s</li>
                            <li>La délégation a été approuvée par votre manager</li>
                            <li>Vous êtes maintenant responsable de ce ticket</li>
                        </ul>
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="%s/tickets/%s" style="background-color: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Traiter le ticket
                        </a>
                    </p>
                </div>
            </body>
            </html>
            """, 
            delegation.getToTechnician().getPrenom(),
            delegation.getTicket().getId(),
            delegation.getTicket().getTitre(),
            delegation.getFromTechnician().getPrenom(),
            delegation.getFromTechnician().getNom(),
            delegation.getTicket().getUser().getPrenom(),
            delegation.getTicket().getUser().getNom(),
            delegation.getTicket().getPriorite().name(),
            delegation.getTicket().getType(),
            delegation.getTicket().getDescription(),
            delegation.getJustification(),
            delegation.getFromTechnician().getPrenom(),
            delegation.getFromTechnician().getNom(),
            frontendUrl,
            delegation.getTicket().getId()
        );
    }

    // ========== TEMPLATES EMAIL POUR LES COMMENTAIRES ==========

    private String createCommentCreatorEmailTemplate(Commentaire commentaire) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Nouveau commentaire sur votre ticket</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                    <h2 style="color: #7c3aed; margin-bottom: 20px;">💬 Nouveau commentaire sur votre ticket</h2>
                    
                    <p>Bonjour %s,</p>
                    
                    <p>Un nouveau commentaire a été ajouté à votre ticket :</p>
                    
                    <div style="background-color: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #7c3aed;">
                        <h3 style="color: #7c3aed; margin-top: 0;">Ticket #%s - %s</h3>
                        <p><strong>Statut :</strong> %s</p>
                        <p><strong>Priorité :</strong> %s</p>
                    </div>
                    
                    <div style="background-color: #faf5ff; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="color: #7c3aed; margin-top: 0;">💭 Commentaire de %s %s</h4>
                        <p><strong>Date :</strong> %s</p>
                        <div style="background-color: white; padding: 15px; border-radius: 6px; margin-top: 10px;">
                            <p style="margin: 0; color: #374151; line-height: 1.6;">%s</p>
                        </div>
                    </div>
                    
                    <div style="background-color: #e0e7ff; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="margin-top: 0;">📋 Informations utiles :</h4>
                        <ul>
                            <li>Vous pouvez répondre directement via l'interface</li>
                            <li>Vous recevrez une notification pour chaque nouveau commentaire</li>
                            <li>Le technicien assigné est également notifié</li>
                        </ul>
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="%s/tickets/%s" style="background-color: #7c3aed; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Voir le ticket et répondre
                        </a>
                    </p>
                </div>
            </body>
            </html>
            """, 
            commentaire.getTicket().getUser().getPrenom(),
            commentaire.getTicket().getId(),
            commentaire.getTicket().getTitre(),
            commentaire.getTicket().getStatut().name(),
            commentaire.getTicket().getPriorite().name(),
            commentaire.getAuteur().getPrenom(),
            commentaire.getAuteur().getNom(),
            getRoleDisplayName(commentaire.getAuteur().getRole().name()),
            commentaire.getDateCreation().format(formatter),
            commentaire.getCommentaire(),
            frontendUrl,
            commentaire.getTicket().getId()
        );
    }

    private String createCommentTechnicianEmailTemplate(Commentaire commentaire, Utilisateur technician) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Nouveau commentaire sur le ticket assigné</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                    <h2 style="color: #059669; margin-bottom: 20px;">🔧 Nouveau commentaire sur votre ticket</h2>
                    
                    <p>Bonjour %s,</p>
                    
                    <p>Un nouveau commentaire a été ajouté au ticket dont vous êtes responsable :</p>
                    
                    <div style="background-color: #f0fdf4; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #059669;">
                        <h3 style="color: #059669; margin-top: 0;">Ticket #%s</h3>
                        <p><strong>Créé par :</strong> %s %s</p>
                        <p><strong>Statut :</strong> %s</p>
                        <p><strong>Priorité :</strong> %s</p>
                    </div>
                    
                    <div style="background-color: #f0fdfa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="color: #059669; margin-top: 0;">💬 Commentaire de %s %s</h4>
                        <p><strong>Date :</strong> %s</p>
                        <div style="background-color: white; padding: 15px; border-radius: 6px; margin-top: 10px;">
                            <p style="margin: 0; color: #374151; line-height: 1.6;">%s</p>
                        </div>
                    </div>
                    
                    <div style="background-color: #ecfdf5; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="margin-top: 0;">⚡ Actions recommandées :</h4>
                        <ul>
                            <li>Lire le commentaire et évaluer s'il nécessite une action</li>
                            <li>Répondre si des clarifications sont nécessaires</li>
                            <li>Mettre à jour le statut du ticket si approprié</li>
                        </ul>
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="%s/tickets/%s" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Voir le ticket et répondre
                        </a>
                    </p>
                </div>
            </body>
            </html>
            """, 
            technician.getPrenom(),
            commentaire.getTicket().getId(),
            commentaire.getTicket().getUser().getPrenom(),
            commentaire.getTicket().getUser().getNom(),
            commentaire.getTicket().getStatut().name(),
            commentaire.getTicket().getPriorite().name(),
            commentaire.getAuteur().getPrenom(),
            commentaire.getAuteur().getNom(),
            getRoleDisplayName(commentaire.getAuteur().getRole().name()),
            commentaire.getDateCreation().format(formatter),
            commentaire.getCommentaire(),
            frontendUrl,
            commentaire.getTicket().getId()
        );
    }

    private String createCommentManagerEmailTemplate(Commentaire commentaire, Utilisateur manager) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Nouveau commentaire sur le ticket</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                    <h2 style="color: #d53f8c; margin-bottom: 20px;">📝 Nouveau commentaire sur le ticket</h2>
                    
                    <p>Bonjour %s,</p>
                    
                    <p>Un nouveau commentaire a été ajouté au ticket :</p>
                    
                    <div style="background-color: #fff5f5; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #d53f8c;">
                        <h3 style="color: #d53f8c; margin-top: 0;">Ticket #%s - %s</h3>
                        <p><strong>Statut :</strong> %s</p>
                        <p><strong>Priorité :</strong> %s</p>
                    </div>
                    
                    <div style="background-color: #fef2f2; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="color: #d53f8c; margin-top: 0;">💬 Commentaire de %s %s</h4>
                        <p><strong>Date :</strong> %s</p>
                        <div style="background-color: white; padding: 15px; border-radius: 6px; margin-top: 10px;">
                            <p style="margin: 0; color: #374151; line-height: 1.6;">%s</p>
                        </div>
                    </div>
                    
                    <div style="background-color: #fee2e2; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="margin-top: 0;">📌 Informations pour le manager :</h4>
                        <ul>
                            <li>Vérifiez les commentaires et intervenez si nécessaire</li>
                            <li>Assurez-vous que le technicien a bien reçu la notification</li>
                            <li>Suivez l'évolution du ticket</li>
                        </ul>
                    </div>
                    
                    <p style="text-align: center; margin-top: 30px;">
                        <a href="%s/tickets/%s" style="background-color: #d53f8c; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Voir le ticket
                        </a>
                    </p>
                </div>
            </body>
            </html>
            """, 
            manager.getPrenom(),
            commentaire.getTicket().getId(),
            commentaire.getTicket().getTitre(),
            commentaire.getTicket().getStatut().name(),
            commentaire.getTicket().getPriorite().name(),
            commentaire.getAuteur().getPrenom(),
            commentaire.getAuteur().getNom(),
            getRoleDisplayName(commentaire.getAuteur().getRole().name()),
            commentaire.getDateCreation().format(formatter),
            commentaire.getCommentaire(),
            frontendUrl,
            commentaire.getTicket().getId()
        );
    }

    private String getRoleDisplayName(String roleName) {
        return switch (roleName) {
            case "ROLE_ADMIN" -> "Administrateur";
            case "ROLE_MANAGER" -> "Manager";
            case "ROLE_TECHNICIEN" -> "Technicien";
            case "ROLE_USER" -> "Utilisateur";
            default -> "Inconnu";
        };
    }
}