package com.addy360.jasbrob.controllers;

import com.addy360.jasbrob.dto.Message;
import com.addy360.jasbrob.dto.SystemData;
import com.addy360.jasbrob.dto.Welcome;
import com.addy360.jasbrob.tasks.TasksCronjob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@Slf4j
public class HomeController {
    @Autowired
    TasksCronjob tasksCronjob;

    @MessageMapping("/hi")
    @SendTo("/message/greetings")
    public Welcome index(Message message){
        return new Welcome("Your message was : "+ message.getMessage());
    }

    @MessageMapping("/sys")
    @SendTo("/message/system/info")
    public SystemData systemData(){

        Runtime runtime = Runtime.getRuntime();
        SystemData data = new SystemData(
                runtime.availableProcessors(),
                runtime.freeMemory()/(1024 * 1024),
                runtime.maxMemory()/(1024 * 1024),
                runtime.totalMemory()/(1024 * 1024),
                new Date(System.currentTimeMillis())
        );
        log.info("system data was triggered, sending data : {}", data);
        return data;
    }

}
