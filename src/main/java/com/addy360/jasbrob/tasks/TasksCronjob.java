package com.addy360.jasbrob.tasks;


import com.addy360.jasbrob.dto.SystemData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class TasksCronjob {


    private final SimpMessagingTemplate simpMessagingTemplate;


    @Scheduled( fixedRate = 1000)
    public void getSystemData(){
        Runtime runtime = Runtime.getRuntime();
        SystemData data = new SystemData(
                runtime.availableProcessors(),
                runtime.freeMemory() / (1024 * 1024),
                runtime.maxMemory() / (1024 * 1024),
                runtime.totalMemory() / (1024 * 1024),
                new Date(System.currentTimeMillis())
        );
        simpMessagingTemplate.convertAndSend("/message/system/info",data);
        log.info("System memory data in GB : {}",data);

    }
}
