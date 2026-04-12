package com.lera.dto.request;

import com.lera.model.Availability;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityRequest {
    @NotNull private Availability availability;
}