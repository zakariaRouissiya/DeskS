package com.symolia.DeskS.repositories;

import com.symolia.DeskS.entities.Delegation;
import com.symolia.DeskS.enums.StatutDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DelegationRepository extends JpaRepository<Delegation, Long> {
    
    @Query("SELECT d FROM Delegation d " +
           "LEFT JOIN FETCH d.ticket t " +
           "LEFT JOIN FETCH d.fromTechnician ft " +
           "LEFT JOIN FETCH d.toTechnician tt " +
           "LEFT JOIN FETCH d.approvedBy ab " +
           "WHERE d.fromTechnician.id = :technicianId " +
           "ORDER BY d.dateDemande DESC")
    List<Delegation> findByFromTechnicianId(@Param("technicianId") Long technicianId);
    
    @Query("SELECT d FROM Delegation d " +
           "LEFT JOIN FETCH d.ticket t " +
           "LEFT JOIN FETCH d.fromTechnician ft " +
           "LEFT JOIN FETCH d.toTechnician tt " +
           "LEFT JOIN FETCH d.approvedBy ab " +
           "WHERE d.toTechnician.id = :technicianId " +
           "ORDER BY d.dateDemande DESC")
    List<Delegation> findByToTechnicianId(@Param("technicianId") Long technicianId);
    
    @Query("SELECT d FROM Delegation d " +
           "LEFT JOIN FETCH d.ticket t " +
           "LEFT JOIN FETCH d.fromTechnician ft " +
           "LEFT JOIN FETCH d.toTechnician tt " +
           "LEFT JOIN FETCH d.approvedBy ab " +
           "WHERE d.statut = :statut " +
           "ORDER BY d.dateDemande DESC")
    List<Delegation> findByStatut(@Param("statut") StatutDelegation statut);
    
    @Query("SELECT d FROM Delegation d " +
           "LEFT JOIN FETCH d.ticket t " +
           "LEFT JOIN FETCH d.fromTechnician ft " +
           "LEFT JOIN FETCH d.toTechnician tt " +
           "LEFT JOIN FETCH d.approvedBy ab " +
           "WHERE d.statut = 'EN_ATTENTE' " +
           "AND t.department.id = :departmentId " +
           "ORDER BY d.dateDemande DESC")
    List<Delegation> findPendingDelegationsByDepartment(@Param("departmentId") Long departmentId);
    
    @Query("SELECT d FROM Delegation d " +
           "LEFT JOIN FETCH d.ticket t " +
           "LEFT JOIN FETCH d.fromTechnician ft " +
           "LEFT JOIN FETCH d.toTechnician tt " +
           "LEFT JOIN FETCH d.approvedBy ab " +
           "WHERE t.department.id = :departmentId " +
           "ORDER BY d.dateDemande DESC")
    List<Delegation> findAllDelegationsByDepartment(@Param("departmentId") Long departmentId);
    
    @Query("SELECT d FROM Delegation d " +
           "LEFT JOIN FETCH d.ticket t " +
           "LEFT JOIN FETCH d.fromTechnician ft " +
           "LEFT JOIN FETCH d.toTechnician tt " +
           "LEFT JOIN FETCH d.approvedBy ab " +
           "WHERE d.ticket.id = :ticketId " +
           "AND d.statut = 'EN_ATTENTE' " +
           "ORDER BY d.dateDemande DESC")
    Optional<Delegation> findPendingDelegationByTicketId(@Param("ticketId") Long ticketId);
    
    @Query("SELECT d FROM Delegation d " +
           "LEFT JOIN FETCH d.ticket t " +
           "LEFT JOIN FETCH d.fromTechnician ft " +
           "LEFT JOIN FETCH d.toTechnician tt " +
           "LEFT JOIN FETCH d.approvedBy ab " +
           "WHERE d.ticket.id = :ticketId " +
           "ORDER BY d.dateDemande DESC")
    List<Delegation> findByTicketId(@Param("ticketId") Long ticketId);
    
    @Query("SELECT COUNT(d) FROM Delegation d " +
           "WHERE d.statut = 'EN_ATTENTE' " +
           "AND d.ticket.department.id = :departmentId")
    long countPendingDelegationsByDepartment(@Param("departmentId") Long departmentId);
}