package com.sportstock.ingestion.controller;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.ingestion.service.SeasonQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonQueryService seasonQueryService;

    @GetMapping("/current-week")
    @ResponseStatus(HttpStatus.OK)
    public CurrentWeekResponse getCurrentWeek() {
        return seasonQueryService.getCurrentWeek();
    }

}
