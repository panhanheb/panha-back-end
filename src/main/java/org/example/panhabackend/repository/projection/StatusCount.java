package org.example.panhabackend.repository.projection;

import org.example.panhabackend.enums.AttendanceStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Projection of "count of attendance records grouped by status".
 */
public interface StatusCount {

    AttendanceStatus getStatus();

    Long getTotal();

    static Map<AttendanceStatus, Long> toMap(List<? extends StatusCount> counts) {
        Map<AttendanceStatus, Long> map = new EnumMap<>(AttendanceStatus.class);
        for (StatusCount c : counts) {
            map.merge(c.getStatus(), c.getTotal(), Long::sum);
        }
        return map;
    }
}
