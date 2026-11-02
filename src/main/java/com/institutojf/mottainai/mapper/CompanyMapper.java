package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.CompanyResponse;
import com.institutojf.mottainai.model.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class CompanyMapper {

    private final SubscriptionPlanMapper subscriptionPlanMapper;

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                subscriptionPlanMapper.toResponse(company.getPlan()),
                company.getOfficialName(),
                company.getTradeName(),
                company.getCnpj(),
                company.getEmail(),
                company.getPhone(),
                company.getLatitude(),
                company.getLongitude(),
                company.getActive()
        );
    }
}
