package com.project.pointsync.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        //  KST 시스템 시계 사용
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
