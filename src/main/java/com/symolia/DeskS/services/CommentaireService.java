package com.symolia.DeskS.services;

import com.symolia.DeskS.entities.Commentaire;
import com.symolia.DeskS.repositories.CommentaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentaireService {

    private final CommentaireRepository commentaireRepository;

    public Commentaire createCommentaire(Commentaire commentaire) {
        return commentaireRepository.save(commentaire);
    }

    public List<Commentaire> getCommentairesByTicketId(Long ticketId) {
        return commentaireRepository.findByTicket_Id(ticketId);
    }

    public Optional<Commentaire> getCommentaireById(Long id) {
        return commentaireRepository.findById(id);
    }

    public Commentaire updateCommentaire(Long id, Commentaire commentaire) {
        commentaire.setId(id);
        return commentaireRepository.save(commentaire);
    }

    public void deleteCommentaire(Long id) {
        commentaireRepository.deleteById(id);
    }
}