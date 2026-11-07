package org.example.panhabackend.service;

import org.example.panhabackend.dto.dashboard.AdminDashboardResponse;
import org.example.panhabackend.dto.dashboard.StudentDashboardResponse;
import org.example.panhabackend.dto.dashboard.TeacherDashboardResponse;

public interface DashboardService {

    AdminDashboardResponse adminDashboard();

    TeacherDashboardResponse teacherDashboard();

    StudentDashboardResponse studentDashboard();
}
