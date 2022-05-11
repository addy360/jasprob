package com.addy360.jasbrob.controllers;

import com.addy360.jasbrob.dto.Message;
import com.addy360.jasbrob.dto.Welcome;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @MessageMapping("/hi")
    @SendTo("/message/greetings")
    public Welcome index(Message message){
        return new Welcome("Your message was : "+ message.getMessage());
    }

}
