package com.application.layered.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(@NotBlank String name, @NotNull @DecimalMin("0.0") BigDecimal price, @Min(0) int stock) {
	
}
