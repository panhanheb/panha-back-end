package org.example.panhabackend.dto.report;

import org.example.panhabackend.enums.AttendanceStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Attendance counters plus the derived percentage.
 * <p>
 * Attendance Percentage = (Present / Total) x 100, where Total is the number of recorded sessions
 * (school days when one roll call per day is taken) and Present optionally includes LATE.
 */
public record AttendanceStats(long totalRecords,
                              long present,
                              long absent,
                              long late,
                              long excused,
                              double attendancePercentage) {

    public static AttendanceStats empty() {
        return new AttendanceStats(0, 0, 0, 0, 0, 0.0);
    }

    public static AttendanceStats of(Map<AttendanceStatus, Long> counts, boolean countLateAsPresent) {
        long present = counts.getOrDefault(AttendanceStatus.PRESENT, 0L);
        long absent = counts.getOrDefault(AttendanceStatus.ABSENT, 0L);
        long late = counts.getOrDefault(AttendanceStatus.LATE, 0L);
        long excused = counts.getOrDefault(AttendanceStatus.EXCUSED, 0L);
        long total = present + absent + late + excused;
        long attended = countLateAsPresent ? present + late : present;
        return new AttendanceStats(total, present, absent, late, excused, percentage(attended, total));
    }

    /** Percentage rounded to two decimals; 0 when there is nothing to divide by. */
    public static double percentage(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
