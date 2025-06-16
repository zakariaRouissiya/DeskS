package com.symolia.DeskS.repositories;

import com.symolia.DeskS.entities.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {
    Optional<PieceJointe> findByTicket_Id(Long ticketId);
}