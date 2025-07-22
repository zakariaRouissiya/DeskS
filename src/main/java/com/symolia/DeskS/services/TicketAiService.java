package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAiService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private static final String SYSTEM_PROMPT = """
        Vous êtes un expert consultant en support technique IT avec plus de 15 ans d'expérience.
        Votre mission est d'analyser des tickets de support et de fournir une assistance technique de niveau expert.

        OBJECTIFS PRINCIPAUX :
        1. Diagnostiquer précisément la nature du problème technique
        2. Évaluer la criticité et l'impact business
        3. Proposer une méthodologie de résolution structurée
        4. Identifier les risques et dépendances
        5. Optimiser le temps de résolution

        STRUCTURE DE RÉPONSE OBLIGATOIRE :
        📋 **DIAGNOSTIC**
        - Classification du problème
        - Cause racine probable
        - Impact estimé sur l'activité

        🚨 **NIVEAU DE CRITICITÉ**
        - Évaluation justifiée (Critique/Élevé/Moyen/Faible)
        - Délai de résolution recommandé

        🔧 **PLAN D'ACTION TECHNIQUE**
        1. Actions immédiates (< 15 min)
        2. Investigations approfondies
        3. Solutions de contournement si applicable
        4. Résolution définitive

        ❓ **INFORMATIONS COMPLÉMENTAIRES**
        - Questions techniques à poser à l'utilisateur
        - Éléments de contexte à vérifier
        - Logs ou diagnostics à collecter

        📚 **RÉFÉRENCES & DOCUMENTATION**
        - Procédures documentées applicables
        - Solutions similaires dans la base de connaissances
        - Ressources techniques recommandées

        CONTRAINTES :
        - Réponse en français professionnel
        - Vocabulaire technique précis
        - Approche méthodique et structurée
        - Maximum 500 mots pour rester actionnable
        """;

    public String analyseTicket(Ticket ticket) {
        try {
            log.info("Analyse du ticket ID: {} - Titre: {}", ticket.getId(), ticket.getTitre());

            if (!isTicketValid(ticket)) {
                log.warn("Ticket invalide détecté: {}", ticket.getId());
                return generateErrorResponse("Données de ticket incomplètes ou invalides");
            }

            List<Document> relevantDocs = performEnhancedRAGSearch(ticket);
            String contextualizedPrompt = buildContextualizedPrompt(ticket, relevantDocs);

            String aiResponse = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(contextualizedPrompt)
                    .call()
                    .content();

            log.info("Analyse générée avec succès pour le ticket: {}", ticket.getId());
            return enrichResponse(aiResponse, ticket, relevantDocs.size());

        } catch (Exception e) {
            log.error("Erreur lors de l'analyse du ticket {}: {}", ticket.getId(), e.getMessage(), e);
            return generateErrorResponse("Erreur technique lors de l'analyse");
        }
    }

    private List<Document> performEnhancedRAGSearch(Ticket ticket) {
        try {
            String searchQuery = buildOptimizedSearchQuery(ticket);

            SearchRequest.Builder rb = SearchRequest.builder()
                    .query(searchQuery)
                    .topK(5);
            SearchRequest searchRequest = rb.build();
            return vectorStore.similaritySearch(searchRequest);

        } catch (Exception e) {
            log.warn("Erreur lors de la recherche RAG: {}", e.getMessage());
            return vectorStore.similaritySearch(ticket.getDescription());
        }
    }

    private String buildOptimizedSearchQuery(Ticket ticket) {
        StringBuilder queryBuilder = new StringBuilder();

        if (StringUtils.hasText(ticket.getTitre())) {
            queryBuilder.append(ticket.getTitre()).append(" ");
        }

        if (StringUtils.hasText(ticket.getDescription())) {
            queryBuilder.append(ticket.getDescription()).append(" ");
        }

        if (ticket.getPriorite() != null) {
            queryBuilder.append("priorité:").append(ticket.getPriorite().name()).append(" ");
        }

        if (ticket.getStatut() != null) {
            queryBuilder.append("statut:").append(ticket.getStatut().name()).append(" ");
        }

        return queryBuilder.toString().trim();
    }

    private String buildContextualizedPrompt(Ticket ticket, List<Document> procedures) {
        return """
            CONTEXTE DU TICKET :
            ═══════════════════
            🎫 ID: %s
            📝 Titre: %s
            📄 Description: %s
            ⚡ Priorité: %s
            📊 Statut: %s
            🏷️ Type: %s
            👤 Utilisateur: %s
            📅 Date création: %s
            👨‍💻 Assigné à: %s
            🏢 Département: %s

            BASE DE CONNAISSANCES (%d procédure(s) pertinente(s)) :
            ═══════════════════════════════════════════════
            %s

            MISSION :
            Analysez ce ticket de support selon votre expertise et fournissez une réponse structurée 
            suivant le format défini dans vos instructions système.
            """.formatted(
                ticket.getId() != null ? ticket.getId().toString() : "N/A",
                Optional.ofNullable(ticket.getTitre()).orElse("Non spécifié"),
                Optional.ofNullable(ticket.getDescription()).orElse("Aucune description"),
                ticket.getPriorite() != null ? ticket.getPriorite().name() : "Non définie",
                ticket.getStatut() != null ? ticket.getStatut().name() : "Non défini",
                Optional.ofNullable(ticket.getType()).orElse("Non spécifié"),
                ticket.getUser() != null ? ticket.getUser().getNom() + " " + ticket.getUser().getPrenom() : "Non spécifié",
                ticket.getDateCreation() != null ? ticket.getDateCreation().toString() : "Non spécifiée",
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getNom() + " " + ticket.getAssignedTo().getPrenom() : "Non assigné",
                ticket.getDepartment() != null ? ticket.getDepartment().getNom() : "Non spécifié",
                procedures.size(),
                formatKnowledgeBaseProcedures(procedures)
        );
    }

    private String formatKnowledgeBaseProcedures(List<Document> procedures) {
        if (procedures == null || procedures.isEmpty()) {
            return "❌ Aucune procédure pertinente trouvée dans la base de connaissances.";
        }

        return procedures.stream()
                .limit(5)
                .map(proc -> {
                    String title = Optional.ofNullable(proc.getMetadata())
                            .map(meta -> meta.get("title"))
                            .map(Object::toString)
                            .orElse("Procédure sans titre");

                    String content = Optional.ofNullable(proc.getText())
                            .filter(t -> !t.isEmpty())
                            .orElseGet(proc::getFormattedContent);

                    return String.format("📄 **%s**\n   %s\n",
                            title,
                            truncateContent(content, 300));
                })
                .collect(Collectors.joining("\n"));
    }

    private String enrichResponse(String aiResponse, Ticket ticket, int proceduresFound) {
        int totalProcedures = vectorStore.similaritySearch("procédure").size();

        String footer = String.format("""
            \n📑 %d procédure(s) consultée(s) sur %d dans la base de connaissance
            🎫 Ticket: %s • ⏱️ Analysé le: %s
            """,
            proceduresFound,
            totalProcedures,
            ticket.getId() != null ? ticket.getId().toString() : "N/A",
            java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")
            )
        );

        return aiResponse + footer;
    }

    private boolean isTicketValid(Ticket ticket) {
        return ticket != null
                && StringUtils.hasText(ticket.getTitre())
                && StringUtils.hasText(ticket.getDescription());
    }

    private String generateErrorResponse(String errorMessage) {
        return String.format("""
            ❌ **ERREUR D'ANALYSE**

            %s

            **Actions recommandées :**
            1. Vérifiez les informations du ticket
            2. Assurez-vous que la description est complète
            3. Contactez l'équipe technique si le problème persiste

            Pour une analyse manuelle, consultez la documentation technique.
            """, errorMessage);
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        return content.length() > maxLength
                ? content.substring(0, maxLength) + "..."
                : content;
    }
}
