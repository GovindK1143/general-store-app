package com.storeservice.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreOperatingHoursRequest {

    @NotNull
    @Min(1)
    @Max(7)
    private Byte dayOfWeek;

    private LocalTime openTime;

    private LocalTime closeTime;

    @NotNull
    private Boolean isClosed;
}