package com.securebank.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBeneficiaryRequest {

    @Size(max = 80)
    private String nickname;
}
