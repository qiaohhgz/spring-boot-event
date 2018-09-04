package com.itunion.example.service;

import com.itunion.example.domain.Order;
import com.itunion.example.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ApplicationEventPublisher publisher;

    // 下单
    @Transactional
    public void placeOrder(Order order) {
        // 保存订单
        orderMapper.save(order);

        // 发布下单事件
        publisher.publishEvent(order);
    }
}
