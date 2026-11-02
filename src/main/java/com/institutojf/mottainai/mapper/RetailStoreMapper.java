package com.institutojf.mottainai.mapper;

import com.institutojf.mottainai.dto.response.RetailStoreResponse;
import com.institutojf.mottainai.model.RetailStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetailStoreMapper {

    private final CompanyMapper companyMapper;

    private final AddressMapper addressMapper;

    public RetailStoreResponse toResponse(RetailStore store) {
        return new RetailStoreResponse(store.getId(), companyMapper.toResponse(store.getCompany()),
                addressMapper.toResponse(store.getAddress()), store.getName(), store.getCnpj(), store.getEmail(),
                store.getPhone(), store.getLatitude(), store.getLongitude(), store.getActive());
    }

}
