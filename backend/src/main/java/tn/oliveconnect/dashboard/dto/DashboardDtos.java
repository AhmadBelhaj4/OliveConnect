package tn.oliveconnect.dashboard.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardSummaryResponse(
            LocalDate from, LocalDate to, double totalHarvestedKg, double totalReceivedKg,
            long completedTours, long openAlerts, Map<String, Long> requestsByStatus) {}

    public record OrchardYieldItem(String orchardId, String orchardName, double totalHarvestedKg, long collectionCount) {}
    public record YieldByOrchardResponse(LocalDate from, LocalDate to, List<OrchardYieldItem> items) {}

    public record CollectionDelaysResponse(
            LocalDate from, LocalDate to, long sampleSize,
            Double avgSubmittedToScheduledHours, Double avgScheduledToCompletedHours) {}

    public record BinFillRateItem(String binId, String binCode, int capacityKg, long receptionCount, Double avgFillRate) {}
    public record BinFillRatesResponse(LocalDate from, LocalDate to, List<BinFillRateItem> items) {}
}
