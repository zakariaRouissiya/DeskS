package com.symolia.DeskS.repositories;

import com.symolia.DeskS.entities.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {
    
    @Query("SELECT c FROM Commentaire c " +
           "LEFT JOIN FETCH c.auteur " +
           "LEFT JOIN FETCH c.auteur.department " +
           "WHERE c.ticket.id = :ticketId " +
           "ORDER BY c.dateCreation ASC")
    List<Commentaire> findByTicketIdWithAuthor(@Param("ticketId") Long ticketId);
    
    List<Commentaire> findByTicket_Id(Long ticketId);
    
    List<Commentaire> findByAuteur_Id(Long auteurId);
    
    @Query("SELECT COUNT(c) FROM Commentaire c WHERE c.ticket.id = :ticketId")
    long countByTicketId(@Param("ticketId") Long ticketId);
    
    @Query("SELECT c FROM Commentaire c " +
           "WHERE c.ticket.id = :ticketId " +
           "ORDER BY c.dateCreation DESC")
    List<Commentaire> findByTicketIdOrderByDateDesc(@Param("ticketId") Long ticketId);
    
    @Query("SELECT c FROM Commentaire c " +
           "WHERE c.auteur.id = :auteurId " +
           "ORDER BY c.dateCreation DESC")
    List<Commentaire> findByAuteurIdOrderByDateDesc(@Param("auteurId") Long auteurId);
}