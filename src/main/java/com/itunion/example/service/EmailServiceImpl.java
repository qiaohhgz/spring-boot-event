package com.itunion.example.service;

import com.itunion.example.domain.Order;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl {

    public void sendEmail(Order order) {
        System.out.println("发送邮件到: " + order.getUserName().toLowerCase());
    }

    @EventListener
    @Async
    public void placeOrderNotice(Order order) {
        sendEmail(order);
    }

    @EventListener
    public void placeOrderNotice2(Order order) {
        sendEmail(order);
    }

}
