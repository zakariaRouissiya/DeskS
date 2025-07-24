package com.symolia.DeskS.services;

import com.symolia.DeskS.repositories.TicketRepository;
import com.symolia.DeskS.repositories.UtilisateurRepository;
import com.symolia.DeskS.repositories.DepartementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final TicketRepository ticketRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DepartementRepository departementRepository;

    public Map<String, Object> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTickets", ticketRepository.count());
        stats.put("totalUsers", utilisateurRepository.count());
        stats.put("totalDepartments", departementRepository.count());
        return stats;
    }

    public Map<String, Long> getTicketsByStatus() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            String statut = ticket.getStatut().name();
            result.put(statut, result.getOrDefault(statut, 0L) + 1);
        });
        return result;
    }

    public Map<String, Long> getTicketsByPriority() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            String priorite = ticket.getPriorite().name();
            result.put(priorite, result.getOrDefault(priorite, 0L) + 1);
        });
        return result;
    }

    public Map<String, Long> getTicketsByDepartment() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            String dept = ticket.getDepartment() != null ? ticket.getDepartment().getNom() : "Aucun";
            result.put(dept, result.getOrDefault(dept, 0L) + 1);
        });
        return result;
    }

    public Map<String, Long> getUsersByRole() {
        Map<String, Long> result = new HashMap<>();
        utilisateurRepository.findAll().forEach(user -> {
            String role = user.getRole().name();
            result.put(role, result.getOrDefault(role, 0L) + 1);
        });
        return result;
    }

    public Map<String, Long> getTicketsByMonth() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            if (ticket.getDateCreation() != null) {
                YearMonth ym = YearMonth.from(ticket.getDateCreation());
                String key = ym.toString();
                result.put(key, result.getOrDefault(key, 0L) + 1);
            }
        });
        return result;
    }

    public Map<String, Object> getAverageResolutionTime() {
        double avg = ticketRepository.findAll().stream()
            .filter(t -> t.getDateCreation() != null && t.getDateResolution() != null)
            .mapToLong(t -> java.time.Duration.between(t.getDateCreation(), t.getDateResolution()).toMinutes())
            .average().orElse(0);
        Map<String, Object> map = new HashMap<>();
        map.put("averageResolutionTimeMinutes", avg);
        return map;
    }

    public Map<String, Long> getTicketVolumeByDay() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            if (ticket.getDateCreation() != null) {
                String key = ticket.getDateCreation().toLocalDate().toString();
                result.put(key, result.getOrDefault(key, 0L) + 1);
            }
        });
        return result;
    }

    public Map<String, Long> getTicketVolumeByHour() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            if (ticket.getDateCreation() != null) {
                String key = ticket.getDateCreation().getHour() + "h";
                result.put(key, result.getOrDefault(key, 0L) + 1);
            }
        });
        return result;
    }

    public Map<String, Long> getTicketVolumeByWeek() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            if (ticket.getDateCreation() != null) {
                LocalDate date = ticket.getDateCreation().toLocalDate();
                int week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                String key = date.getYear() + "-W" + week;
                result.put(key, result.getOrDefault(key, 0L) + 1);
            }
        });
        return result;
    }


    public Map<String, Long> getTicketVolumeByYear() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            if (ticket.getDateCreation() != null) {
                String key = String.valueOf(ticket.getDateCreation().getYear());
                result.put(key, result.getOrDefault(key, 0L) + 1);
            }
        });
        return result;
    }

    public Map<String, Long> getTicketVolumeByTechnician() {
        Map<String, Long> result = new HashMap<>();
        ticketRepository.findAll().forEach(ticket -> {
            if (ticket.getAssignedTo() != null) {
                String tech = ticket.getAssignedTo().getNom() + " " + ticket.getAssignedTo().getPrenom();
                result.put(tech, result.getOrDefault(tech, 0L) + 1);
            }
        });
        return result;
    }
}