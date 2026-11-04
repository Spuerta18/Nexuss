package nexussMarket.domain.valueobjects;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import nexussMarket.domain.exceptions.InvalidReportPeriodException;

/**
 * The days a report covers, {@code from} and {@code to} both included, in the
 * application's time zone. {@code from} may not be after {@code to}, and a
 * period covers at most {@value #MAX_DAYS} days; otherwise
 * {@link InvalidReportPeriodException}.
 */
public record ReportPeriod(LocalDate from, LocalDate to) {

    public static final int MAX_DAYS = 366;
    public static final int DEFAULT_DAYS = 30;

    public ReportPeriod {
        if (from == null || to == null) {
            throw new InvalidReportPeriodException("A report period needs both from and to");
        }
        if (from.isAfter(to)) {
            throw new InvalidReportPeriodException("from (" + from + ") must not be after to (" + to + ")");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new InvalidReportPeriodException("A report period covers at most " + MAX_DAYS + " days");
        }
    }

    /**
     * The period asked for; a missing end is today, and a missing start is
     * {@value #DEFAULT_DAYS} days before the end (both included).
     */
    public static ReportPeriod of(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        return new ReportPeriod(from != null ? from : end.minusDays(DEFAULT_DAYS - 1), end);
    }

    /** First instant of the period. */
    public LocalDateTime startInclusive() {
        return from.atStartOfDay();
    }

    /** First instant after the period. */
    public LocalDateTime endExclusive() {
        return to.plusDays(1).atStartOfDay();
    }

    public boolean contains(LocalDateTime instant) {
        return instant != null && !instant.isBefore(startInclusive()) && instant.isBefore(endExclusive());
    }
}
