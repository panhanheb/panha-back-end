package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.dashboard.AdminDashboardResponse;
import org.example.panhabackend.dto.dashboard.StudentDashboardResponse;
import org.example.panhabackend.dto.dashboard.TeacherDashboardResponse;
import org.example.panhabackend.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "11. Dashboard", description = "Role-specific dashboard figures")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "School-wide totals and today's attendance (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public AdminDashboardResponse admin() {
        return dashboardService.adminDashboard();
    }

    @Operation(summary = "Assigned classes, students and today's summary (TEACHER)")
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/teacher")
    public TeacherDashboardResponse teacher() {
        return dashboardService.teacherDashboard();
    }

    @Operation(summary = "Personal attendance percentage and recent history (STUDENT)")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student")
    public StudentDashboardResponse student() {
        return dashboardService.studentDashboard();
    }
}
