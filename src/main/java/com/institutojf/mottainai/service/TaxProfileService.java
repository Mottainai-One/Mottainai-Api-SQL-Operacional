package com.institutojf.mottainai.service;

import com.institutojf.mottainai.dto.request.TaxProfileRequest;
import com.institutojf.mottainai.dto.response.TaxProfileResponse;
import com.institutojf.mottainai.exception.BusinessException;
import com.institutojf.mottainai.exception.ConflictException;
import com.institutojf.mottainai.exception.ResourceNotFoundException;
import com.institutojf.mottainai.model.AppUser;
import com.institutojf.mottainai.model.TaxProfile;
import com.institutojf.mottainai.repository.AppUserRepository;
import com.institutojf.mottainai.repository.AuditLogRepository;
import com.institutojf.mottainai.repository.ProductRepository;
import com.institutojf.mottainai.repository.TaxProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class TaxProfileService {

    private final TaxProfileRepository taxProfileRepository;

    private final ProductRepository productRepository;

    private final AppUserRepository appUserRepository;

    private final StaffUserLookup staffUserLookup;

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<TaxProfileResponse> findAll(Pageable pageable) {
        return taxProfileRepository.findAllByActiveTrueAndDeletedAtIsNull(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaxProfileResponse findById(Integer id) {
        return toResponse(findActiveProfile(id));
    }

    @Transactional
    public TaxProfileResponse create(TaxProfileRequest request, String actorEmail) {
        String code = request.code().trim();
        if (taxProfileRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Tax profile code already exists");
        }
        AppUser actor = findActor(actorEmail);
        TaxProfile profile = new TaxProfile();
        apply(profile, request);
        profile.setCode(code);
        profile.setActive(true);
        profile = taxProfileRepository.save(profile);
        auditLogRepository.record("tax_profile", "INSERT", profile.getId().toString(), actor.getId(), null,
                toResponse(profile));
        return toResponse(profile);
    }

    @Transactional
    public TaxProfileResponse update(Integer id, TaxProfileRequest request, String actorEmail) {
        TaxProfile profile = findActiveProfile(id);
        String code = request.code().trim();
        taxProfileRepository.findByCodeIgnoreCase(code)
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ConflictException("Tax profile code already exists");
            });
        AppUser actor = findActor(actorEmail);
        TaxProfileResponse oldData = toResponse(profile);
        apply(profile, request);
        profile.setCode(code);
        profile = taxProfileRepository.save(profile);
        auditLogRepository.record("tax_profile", "UPDATE", profile.getId().toString(), actor.getId(), oldData,
                toResponse(profile));
        return toResponse(profile);
    }

    @Transactional
    public void delete(Integer id, String actorEmail) {
        TaxProfile profile = findActiveProfile(id);
        if (productRepository.existsByTaxProfile_IdAndActiveTrueAndDeletedAtIsNull(id)) {
            throw new BusinessException("Tax profile is used by active products");
        }
        AppUser actor = findActor(actorEmail);
        TaxProfileResponse oldData = toResponse(profile);
        profile.setActive(false);
        profile.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        profile = taxProfileRepository.save(profile);
        auditLogRepository.record("tax_profile", "UPDATE", profile.getId().toString(), actor.getId(), oldData,
                toResponse(profile));
    }

    private void apply(TaxProfile profile, TaxProfileRequest request) {
        profile.setName(request.name().trim());
        profile.setDescription(request.description());
        profile.setCfop(request.cfop());
        profile.setIcmsCst(request.icmsCst());
        profile.setIcmsCsosn(request.icmsCsosn());
        profile.setIcmsRate(request.icmsRate());
        profile.setIpiCst(request.ipiCst());
        profile.setIpiRate(request.ipiRate());
        profile.setPisCst(request.pisCst());
        profile.setPisRate(request.pisRate());
        profile.setCofinsCst(request.cofinsCst());
        profile.setCofinsRate(request.cofinsRate());
    }

    private TaxProfile findActiveProfile(Integer id) {
        return taxProfileRepository.findByIdAndActiveTrueAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tax profile not found"));
    }

    private AppUser findActor(String email) {
        return staffUserLookup.findActive(email)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private TaxProfileResponse toResponse(TaxProfile profile) {
        return new TaxProfileResponse(profile.getId(), profile.getCode(), profile.getName(), profile.getDescription(),
                profile.getCfop(), profile.getIcmsCst(), profile.getIcmsCsosn(), profile.getIcmsRate(),
                profile.getIpiCst(), profile.getIpiRate(), profile.getPisCst(), profile.getPisRate(),
                profile.getCofinsCst(), profile.getCofinsRate(), profile.getActive(), profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

}
