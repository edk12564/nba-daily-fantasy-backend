package com.picknroll.demo.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "nbaScheduleClient",
        url = "https://cdn.nba.com/static/json/staticData",
        configuration = NbaCdnFeignConfig.class)
public interface NbaScheduleClient {

    @GetMapping("/scheduleLeagueV2_1.json")
    String getLeagueSchedule();
}
