package com.symolia.DeskS.repositories;

import com.symolia.DeskS.entities.Ticket;
import com.symolia.DeskS.enums.Priorite;
import com.symolia.DeskS.enums.Statut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT DISTINCT t FROM Ticket t " +
           "LEFT JOIN FETCH t.piecesJointes " +
           "LEFT JOIN FETCH t.user u " +
           "LEFT JOIN FETCH u.department " +
           "WHERE t.user.id = :userId " +
           "ORDER BY t.dateCreation DESC")
    List<Ticket> findByUserIdWithPiecesJointes(@Param("userId") Long userId);

    @Query("SELECT DISTINCT t FROM Ticket t " +
           "LEFT JOIN FETCH t.piecesJointes " +
           "LEFT JOIN FETCH t.user u " +
           "LEFT JOIN FETCH u.department " +
           "LEFT JOIN FETCH t.assignedTo " +
           "LEFT JOIN FETCH t.delegatedTo " +
           "WHERE t.id = :ticketId")
    Optional<Ticket> findByIdWithPiecesJointes(@Param("ticketId") Long ticketId);

    @Query("SELECT DISTINCT t FROM Ticket t " +
           "LEFT JOIN FETCH t.piecesJointes " +
           "LEFT JOIN FETCH t.user u " +
           "LEFT JOIN FETCH u.department " +
           "WHERE t.department.id = :departmentId " +
           "ORDER BY t.dateCreation DESC")
    List<Ticket> findByDepartmentIdWithPiecesJointes(@Param("departmentId") Long departmentId);

    @Query("SELECT DISTINCT t FROM Ticket t " +
           "LEFT JOIN FETCH t.user u " +
           "LEFT JOIN FETCH u.department " +
           "LEFT JOIN FETCH t.assignedTo " +
           "LEFT JOIN FETCH t.department " +
           "LEFT JOIN FETCH t.piecesJointes " +
           "WHERE t.assignedTo.id = :technicianId " +
           "ORDER BY t.dateCreation DESC")
    List<Ticket> findByAssignedToIdWithPiecesJointes(@Param("technicianId") Long technicianId);

    @Query(value = """
           SELECT t.*, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email,
                  d.nom as department_nom,
                  pj.id as piece_jointe_id, pj.nom_du_fichier, pj.type_du_fichier, 
                  pj.url, pj.taille
           FROM ticket t
           LEFT JOIN utilisateur u ON t.user_id = u.id
           LEFT JOIN departement d ON u.department_id = d.id
           LEFT JOIN piece_jointe pj ON pj.ticket_id = t.id
           WHERE t.user_id = :userId
           ORDER BY t.date_creation DESC
           """, nativeQuery = true)
    List<Object[]> findTicketsWithDetailsNative(@Param("userId") Long userId);

    @Query(value = """
           SELECT t.*, 
                  COUNT(pj.id) as pieces_jointes_count,
                  STRING_AGG(pj.nom_du_fichier, ', ') as fichiers_noms
           FROM ticket t
           LEFT JOIN piece_jointe pj ON pj.ticket_id = t.id
           WHERE t.statut = :statut
           GROUP BY t.id, t.titre, t.description, t.statut, t.priorite, 
                    t.date_creation, t.date_resolution, t.type, t.user_id, 
                    t.department_id, t.assigned_to_id, t.delegated_to_id
           ORDER BY t.date_creation DESC
           """, nativeQuery = true)
    List<Object[]> findTicketsByStatutWithAttachmentCount(@Param("statut") String statut);

    @Query(value = """
           SELECT DISTINCT t.* FROM ticket t
           LEFT JOIN utilisateur u ON t.user_id = u.id
           LEFT JOIN departement d ON u.department_id = d.id
           LEFT JOIN piece_jointe pj ON pj.ticket_id = t.id
           WHERE (
               LOWER(t.titre) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(d.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(pj.nom_du_fichier) LIKE LOWER(CONCAT('%', :search, '%'))
           )
           ORDER BY t.date_creation DESC
           """, nativeQuery = true)
    List<Ticket> searchTicketsFullText(@Param("search") String search);

    @Query(value = """
           SELECT 
               t.statut,
               COUNT(*) as count,
               AVG(EXTRACT(EPOCH FROM (t.date_resolution - t.date_creation))/3600) as avg_resolution_hours
           FROM ticket t
           WHERE t.date_creation >= :startDate AND t.date_creation <= :endDate
           GROUP BY t.statut
           ORDER BY count DESC
           """, nativeQuery = true)
    List<Object[]> getTicketStatisticsByPeriod(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);

    @Query(value = """
           SELECT 
               d.nom as department_name,
               COUNT(t.id) as ticket_count,
               COUNT(CASE WHEN t.statut = 'OUVERT' THEN 1 END) as open_count,
               COUNT(CASE WHEN t.statut = 'FERME' THEN 1 END) as closed_count,
               AVG(CASE WHEN t.date_resolution IS NOT NULL 
                   THEN EXTRACT(EPOCH FROM (t.date_resolution - t.date_creation))/3600 
                   END) as avg_resolution_hours
           FROM ticket t
           JOIN utilisateur u ON t.user_id = u.id
           JOIN departement d ON u.department_id = d.id
           WHERE t.date_creation >= :startDate
           GROUP BY d.id, d.nom
           ORDER BY ticket_count DESC
           """, nativeQuery = true)
    List<Object[]> getTicketStatisticsByDepartment(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT DISTINCT t FROM Ticket t " +
           "LEFT JOIN FETCH t.piecesJointes " +
           "LEFT JOIN FETCH t.user u " +
           "LEFT JOIN FETCH u.department d " +
           "WHERE (:statut IS NULL OR t.statut = :statut) " +
           "AND (:priorite IS NULL OR t.priorite = :priorite) " +
           "AND (:departmentId IS NULL OR d.id = :departmentId) " +
           "AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId) " +
           "AND (:dateDebut IS NULL OR t.dateCreation >= :dateDebut) " +
           "AND (:dateFin IS NULL OR t.dateCreation <= :dateFin) " +
           "AND (:search IS NULL OR " +
           "     LOWER(t.titre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.dateCreation DESC")
    List<Ticket> findTicketsWithFilters(@Param("statut") Statut statut,
                                       @Param("priorite") Priorite priorite,
                                       @Param("departmentId") Long departmentId,
                                       @Param("assignedToId") Long assignedToId,
                                       @Param("dateDebut") LocalDateTime dateDebut,
                                       @Param("dateFin") LocalDateTime dateFin,
                                       @Param("search") String search);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
           "FROM Ticket t WHERE t.id = :ticketId AND t.user.id = :userId")
    boolean existsByIdAndUserId(@Param("ticketId") Long ticketId, @Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.user.id = :userId AND t.statut = :statut")
    long countByUserIdAndStatut(@Param("userId") Long userId, @Param("statut") Statut statut);

    @Query("SELECT t FROM Ticket t " +
           "WHERE t.department.id = :departmentId " +
           "AND t.assignedTo IS NULL " +
           "AND t.statut = 'OUVERT' " +
           "ORDER BY t.priorite DESC, t.dateCreation ASC")
    List<Ticket> findUnassignedTicketsByDepartment(@Param("departmentId") Long departmentId);

    long countByDepartmentId(Long departmentId);
    long countByDepartmentIdAndStatut(Long departmentId, Statut statut);
    long countByAssignedToIdAndStatutIn(Long assignedToId, List<Statut> statuts);

    List<Ticket> findByUserIdOrderByDateCreationDesc(Long userId);
    List<Ticket> findByDepartmentIdOrderByDateCreationDesc(Long departmentId);
    List<Ticket> findByAssignedToIdOrderByDateCreationDesc(Long assignedToId);
    List<Ticket> findByDelegatedToIdOrderByDateCreationDesc(Long delegatedToId);
    List<Ticket> findByStatutOrderByDateCreationDesc(Statut statut);
    List<Ticket> findByPrioriteOrderByDateCreationDesc(Priorite priorite);
    List<Ticket> findByDateCreationBetweenOrderByDateCreationDesc(LocalDateTime start, LocalDateTime end);
    List<Ticket> findByTitreContainingIgnoreCaseOrderByDateCreationDesc(String titre);
    List<Ticket> findAllByOrderByDateCreationDesc();
    List<Ticket> findByStatut(Statut statut);
    Optional<Ticket> findById(Long id);
    List<Ticket> findByUserId(Long userId);

    @Query("SELECT COUNT(pj) FROM PieceJointe pj WHERE pj.ticket.id = :ticketId")
    long countPiecesJointesByTicketId(@Param("ticketId") Long ticketId);

    @Query(value = """
           SELECT t.id, t.titre, 
                  CASE WHEN COUNT(pj.id) > 0 THEN true ELSE false END as has_attachments
           FROM ticket t
           LEFT JOIN piece_jointe pj ON pj.ticket_id = t.id
           WHERE t.user_id = :userId
           GROUP BY t.id, t.titre
           ORDER BY t.date_creation DESC
           """, nativeQuery = true)
    List<Object[]> findTicketsWithAttachmentStatus(@Param("userId") Long userId);

    @Query(value = """
           SELECT t.* FROM ticket t
           WHERE t.priorite IN ('HAUTE', 'CRITIQUE')
           AND t.statut NOT IN ('FERME', 'VALIDE')
           AND t.date_creation >= :since
           ORDER BY 
               CASE t.priorite 
                   WHEN 'CRITIQUE' THEN 1 
                   WHEN 'HAUTE' THEN 2 
                   ELSE 3 
               END,
               t.date_creation DESC
           """, nativeQuery = true)
    List<Ticket> findHighPriorityTicketsSince(@Param("since") LocalDateTime since);

    @Query("SELECT t FROM Ticket t " +
           "LEFT JOIN FETCH t.piecesJointes " +
           "LEFT JOIN FETCH t.user " +
           "WHERE t.assignedTo IS NULL " +
           "AND t.statut = 'OUVERT' " +
           "ORDER BY t.priorite DESC, t.dateCreation ASC")
    List<Ticket> findUnassignedOpenTickets();

}