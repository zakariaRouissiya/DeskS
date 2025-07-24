package com.symolia.DeskS.web;

import com.symolia.DeskS.services.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/global-stats")
    public Map<String, Object> getGlobalStats() {
        return reportingService.getGlobalStats();
    }

    @GetMapping("/tickets-by-status")
    public Map<String, Long> getTicketsByStatus() {
        return reportingService.getTicketsByStatus();
    }

    @GetMapping("/tickets-by-priority")
    public Map<String, Long> getTicketsByPriority() {
        return reportingService.getTicketsByPriority();
    }

    @GetMapping("/tickets-by-department")
    public Map<String, Long> getTicketsByDepartment() {
        return reportingService.getTicketsByDepartment();
    }

    @GetMapping("/users-by-role")
    public Map<String, Long> getUsersByRole() {
        return reportingService.getUsersByRole();
    }

    @GetMapping("/tickets-by-month")
    public Map<String, Long> getTicketsByMonth() {
        return reportingService.getTicketsByMonth();
    }
    @GetMapping("/average-resolution-time")
    public Map<String, Object> getAverageResolutionTime() {
        return reportingService.getAverageResolutionTime();
    }
    @GetMapping("/ticket-volume-by-day")
    public Map<String, Long> getTicketVolumeByDay() {
        return reportingService.getTicketVolumeByDay();
    }
    @GetMapping("/ticket-volume-by-hour")
    public Map<String, Long> getTicketVolumeByHour() {
        return reportingService.getTicketVolumeByHour();
    }
    @GetMapping("/ticket-volume-by-week")
    public Map<String, Long> getTicketVolumeByWeek() {
        return reportingService.getTicketVolumeByWeek();
    }
    @GetMapping("/ticket-volume-by-year")
    public Map<String, Long> getTicketVolumeByYear() {
        return reportingService.getTicketVolumeByYear();
    }
    @GetMapping("/ticket-volume-by-technician")
    public Map<String, Long> getTicketVolumeByTechnician() {
        return reportingService.getTicketVolumeByTechnician();
    }

}