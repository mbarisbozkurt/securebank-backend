package com.securebank.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeneficiaryRequest {

    @Size(max = 34)
    private String iban;

    @Size(max = 32)
    private String accountNumber;

    @Size(max = 80)
    private String nickname;
}
