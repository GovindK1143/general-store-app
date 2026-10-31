
package com.storeservice.service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.storeservice.dto.StoreOperatingHoursRequest;
import com.storeservice.dto.StoreOperatingHoursResponse;
import com.storeservice.exception.StoreNotFoundException;
import com.storeservice.model.Store;
import com.storeservice.model.StoreOperatingHours;
import com.storeservice.repository.StoreOperatingHoursRepository;
import com.storeservice.repository.StoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreOperatingHoursService {

    private final StoreRepository storeRepository;

    private final StoreOperatingHoursRepository operatingHoursRepository;

    public List<StoreOperatingHoursResponse> saveWeeklySchedule(
            Long storeId,
            List<StoreOperatingHoursRequest> requests) {

        validateSchedule(requests);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() ->
                        new StoreNotFoundException(
                                "Store not found with ID: " + storeId
                        )
                );

        List<StoreOperatingHours> existingHours =
                operatingHoursRepository
                        .findByStore_IdOrderByDayOfWeekAsc(storeId);

        Map<Byte, StoreOperatingHours> existingByDay =
                new HashMap<>();

        for (StoreOperatingHours hours : existingHours) {
            existingByDay.put(
                    hours.getDayOfWeek(),
                    hours
            );
        }

        List<StoreOperatingHours> hoursToSave =
                new ArrayList<>();

        for (StoreOperatingHoursRequest request : requests) {

            StoreOperatingHours hours =
                    existingByDay.get(request.getDayOfWeek());

            if (hours == null) {
                hours = new StoreOperatingHours();
            }

            hours.setStore(store);
            hours.setDayOfWeek(request.getDayOfWeek());
            hours.setIsClosed(request.getIsClosed());

            if (Boolean.TRUE.equals(request.getIsClosed())) {

                hours.setOpenTime(null);
                hours.setCloseTime(null);

            } else {

                hours.setOpenTime(request.getOpenTime());
                hours.setCloseTime(request.getCloseTime());
            }

            hoursToSave.add(hours);
        }

        return operatingHoursRepository.saveAll(hoursToSave)
                .stream()
                .sorted((a, b) ->
                        a.getDayOfWeek().compareTo(b.getDayOfWeek()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StoreOperatingHoursResponse> getWeeklySchedule(
            Long storeId) {

        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException(
                    "Store not found with ID: " + storeId
            );
        }

        return operatingHoursRepository
                .findByStore_IdOrderByDayOfWeekAsc(storeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateSchedule(
            List<StoreOperatingHoursRequest> requests) {

        if (requests == null || requests.size() != 7) {

            throw new IllegalArgumentException(
                    "Exactly seven operating-hour entries are required"
            );
        }

        Set<Byte> days = new HashSet<>();

        for (StoreOperatingHoursRequest request : requests) {

            if (request == null ||
                    request.getDayOfWeek() == null ||
                    request.getIsClosed() == null) {

                throw new IllegalArgumentException(
                        "Day and closed status are required"
                );
            }

            Byte day = request.getDayOfWeek();

            if (day < 1 || day > 7) {

                throw new IllegalArgumentException(
                        "Day of week must be between 1 and 7"
                );
            }

            if (!days.add(day)) {

                throw new IllegalArgumentException(
                        "Duplicate day in weekly schedule: " + day
                );
            }

            if (Boolean.TRUE.equals(request.getIsClosed())) {

                if (request.getOpenTime() != null ||
                        request.getCloseTime() != null) {

                    throw new IllegalArgumentException(
                            "Closed days must not contain opening or closing times"
                    );
                }

            } else {

                if (request.getOpenTime() == null ||
                        request.getCloseTime() == null) {

                    throw new IllegalArgumentException(
                            "Opening and closing times are required for open days"
                    );
                }

                if (!request.getOpenTime()
                        .isBefore(request.getCloseTime())) {

                    throw new IllegalArgumentException(
                            "Opening time must be earlier than closing time for "
                                    + DayOfWeek.of(day)
                    );
                }
            }
        }

        if (days.size() != 7) {

            throw new IllegalArgumentException(
                    "Weekly schedule must include all seven days"
            );
        }
    }

    private StoreOperatingHoursResponse toResponse(
            StoreOperatingHours hours) {

        return new StoreOperatingHoursResponse(
                hours.getId(),
                hours.getStore().getId(),
                hours.getDayOfWeek(),
                DayOfWeek.of(hours.getDayOfWeek()).name(),
                hours.getOpenTime(),
                hours.getCloseTime(),
                hours.getIsClosed(),
                hours.getCreatedAt(),
                hours.getUpdatedAt()
        );
    }
}
