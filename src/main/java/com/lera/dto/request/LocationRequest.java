package com.lera.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LocationRequest {
    @NotNull @DecimalMin("-90")  @DecimalMax("90")  private Double lat;
    @NotNull @DecimalMin("-180") @DecimalMax("180") private Double lng;
}
