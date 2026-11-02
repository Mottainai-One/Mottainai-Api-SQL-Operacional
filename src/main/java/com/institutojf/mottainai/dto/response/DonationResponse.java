package com.institutojf.mottainai.dto.response;

import com.institutojf.mottainai.model.Donation;
import com.institutojf.mottainai.model.enums.DonationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DonationResponse(
        Integer id,
        Integer storeId,
        Integer employeeId,
        String institution,
        LocalDateTime donationDate,
        DonationStatus status,
        String observation,
        Integer version,
        List<Item> items
) {

    public static DonationResponse from(Donation donation) {
        return new DonationResponse(donation.getId(),
                donation.getStore().getId(),
                donation.getEmployee().getId(),
                donation.getInstitution(),
                donation.getDonationDate(),
                donation.getStatus(),
                donation.getObservation(),
                donation.getVersion(),
                donation.getItems()
                    .stream()
                    .map(i -> new Item(i.getId(), i.getBatch().getId(), i.getDonatedQuantity()))
                    .toList());
    }
    public record Item(
            Integer id,
            Integer batchId,
            BigDecimal donatedQuantity
    ) {
    }
}
