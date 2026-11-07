package org.example.panhabackend.service.impl;

import org.example.panhabackend.dto.report.ReportQuery;
import org.example.panhabackend.enums.ReportPeriod;
import org.example.panhabackend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the date windows used by daily / weekly / monthly / custom reports.
 */
class ReportRangeTest {

    private final ReportServiceImpl service = new ReportServiceImpl(
            null, null, null, null, null, null, null, null, null);

    private static ReportQuery query(ReportPeriod period, LocalDate date, LocalDate from, LocalDate to) {
        return new ReportQuery(period, date, from, to, null, null, null);
    }

    @Test
    void weeklyRangeRunsMondayToSunday() {
        // 2026-11-05 is a Thursday
        ReportServiceImpl.DateRange range = service.resolveRange(
                query(ReportPeriod.WEEKLY, LocalDate.of(2026, 11, 5), null, null));

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 11, 8));
    }

    @Test
    void monthlyRangeCoversCalendarMonth() {
        ReportServiceImpl.DateRange range = service.resolveRange(
                query(ReportPeriod.MONTHLY, LocalDate.of(2026, 2, 14), null, null));

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void periodDefaultsToCustomWhenDatesGiven() {
        ReportServiceImpl.DateRange range = service.resolveRange(
                query(null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));

        assertThat(range.period()).isEqualTo(ReportPeriod.CUSTOM);
    }

    @Test
    void customRequiresBothDates() {
        assertThatThrownBy(() -> service.resolveRange(query(ReportPeriod.CUSTOM, null, LocalDate.now(), null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void invertedRangeIsRejected() {
        assertThatThrownBy(() -> service.resolveRange(
                query(ReportPeriod.CUSTOM, null, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1))))
                .isInstanceOf(BadRequestException.class);
    }
}
