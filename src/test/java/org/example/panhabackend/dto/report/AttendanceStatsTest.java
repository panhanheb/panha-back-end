package org.example.panhabackend.dto.report;

import org.example.panhabackend.enums.AttendanceStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceStatsTest {

    private static final Map<AttendanceStatus, Long> COUNTS = Map.of(
            AttendanceStatus.PRESENT, 15L,
            AttendanceStatus.ABSENT, 2L,
            AttendanceStatus.LATE, 2L,
            AttendanceStatus.EXCUSED, 1L);

    @Test
    void countsLateAsPresentWhenConfigured() {
        AttendanceStats stats = AttendanceStats.of(COUNTS, true);

        assertThat(stats.totalRecords()).isEqualTo(20);
        assertThat(stats.present()).isEqualTo(15);
        assertThat(stats.late()).isEqualTo(2);
        // (15 present + 2 late) / 20 * 100
        assertThat(stats.attendancePercentage()).isEqualTo(85.0);
    }

    @Test
    void excludesLateWhenNotCountedAsPresent() {
        AttendanceStats stats = AttendanceStats.of(COUNTS, false);

        // 15 / 20 * 100
        assertThat(stats.attendancePercentage()).isEqualTo(75.0);
    }

    @Test
    void roundsToTwoDecimals() {
        assertThat(AttendanceStats.percentage(2, 3)).isEqualTo(66.67);
    }

    @Test
    void returnsZeroWhenNoRecords() {
        AttendanceStats stats = AttendanceStats.of(Map.of(), true);

        assertThat(stats.totalRecords()).isZero();
        assertThat(stats.attendancePercentage()).isZero();
    }
}
