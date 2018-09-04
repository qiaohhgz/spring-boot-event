package com.itunion.example;

import com.itunion.example.domain.Order;
import com.itunion.example.service.OrderServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootEventApplicationTests {

    @Autowired
    private OrderServiceImpl orderService;

    @Test
    public void placeOrder() {
        Order order = new Order();
        order.setUserName(null);
        order.setGoodsName("iphone X");
        orderService.placeOrder(order);
    }

}
