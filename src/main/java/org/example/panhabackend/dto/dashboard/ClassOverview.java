package org.example.panhabackend.dto.dashboard;

public record ClassOverview(Long classId,
                            String code,
                            String name,
                            Integer gradeLevel,
                            long studentCount,
                            boolean attendanceTakenToday) {
}
