package com.example.jira.controller;

import com.example.jira.dto.RealtimeMessage;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/collaboration")
    @SendTo("/topic/collaboration")
    public RealtimeMessage collaboration(
            RealtimeMessage message) {

        return message;
    }
}