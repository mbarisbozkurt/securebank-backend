package com.securebank.controller;

import com.securebank.dto.BeneficiaryRequest;
import com.securebank.dto.BeneficiaryResponse;
import com.securebank.dto.UpdateBeneficiaryRequest;
import com.securebank.service.BeneficiaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public List<BeneficiaryResponse> getMyBeneficiaries() {
        return beneficiaryService.getBeneficiariesForCurrentUser();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse createBeneficiary(@Valid @RequestBody BeneficiaryRequest request) {
        return beneficiaryService.createBeneficiary(request);
    }

    @PatchMapping("/{beneficiaryId}")
    public BeneficiaryResponse updateBeneficiary(
            @PathVariable Long beneficiaryId,
            @Valid @RequestBody UpdateBeneficiaryRequest request
    ) {
        return beneficiaryService.updateBeneficiary(beneficiaryId, request);
    }

    @DeleteMapping("/{beneficiaryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBeneficiary(@PathVariable Long beneficiaryId) {
        beneficiaryService.deleteBeneficiary(beneficiaryId);
    }
}
