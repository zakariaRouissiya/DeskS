package com.symolia.DeskS.repositories;

import com.symolia.DeskS.entities.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {
    List<Commentaire> findByTicket_Id(Long ticketId);
    List<Commentaire> findByAuteur_Id(Long auteurId);
}