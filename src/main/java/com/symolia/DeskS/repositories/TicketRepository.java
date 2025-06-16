package com.symolia.DeskS.repositories;

import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.enums.Priorite;
import com.symolia.DeskS.enums.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUser_Id(Long userId);
    List<Ticket> findByDepartment_Id(Long departmentId);
    List<Ticket> findByStatut(Statut statut);
    List<Ticket> findByPriorite(Priorite priorite);
    List<Ticket> findByAssignedTo_Id(Long technicienId);
    List<Ticket> findByDelegatedTo_Id(Long technicienId);
    List<Ticket> findByDateCreationBetween(LocalDateTime start, LocalDateTime end);
    List<Ticket> findByTitreContainingIgnoreCase(String titre);
}