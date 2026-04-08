package com.lera.dto.request;

import com.lera.model.EmergencyType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateEmergencyRequest {
    @NotNull                              private EmergencyType type;
    @NotNull @DecimalMin("-90")  @DecimalMax("90")  private Double incidentLat;
    @NotNull @DecimalMin("-180") @DecimalMax("180") private Double incidentLng;
    private String district;
}
