![](https://upload-images.jianshu.io/upload_images/9260441-2fe5a79532ab50a1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

通过使用spring 事件来解决业务代码的耦合

下面通过一个下单的业务代码，拆解为使用事件驱动的方式开发

### 原始的业务代码

```java
package com.itunion.example.service;

import com.itunion.example.domain.Order;
import com.itunion.example.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private EmailServiceImpl emailService;

    // 下单
    @Transactional
    public void placeOrder(Order order) {
        // 保存订单
        orderMapper.save(order);

        // 发送邮件通知
        emailService.sendEmail(order);
    }
}
```

这里有个下单接口，首先保存订单到数据库，然后发送邮件通知给客户

> 思考：如果某一段时间邮件服务器挂了，那是不是就下不了单了？
> 如果后续业务变化需要在下单之后增加其他逻辑，是不是需要修改代码

为了不影响下单我们需要把发送邮件解耦出来

### 引入事件发布对象

```java
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
```

删除了邮件的依赖和发送邮件的方法
这里我们引入了 ApplicationEventPublisher 对象，用来发布下单事件

> 发布总要有接收处理事件的地方

### 接收并处理事件

```java
package com.itunion.example.service;

import com.itunion.example.domain.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl {

    public void sendEmail(Order order) {
        System.out.println("发送邮件到: " + order.getUserName().toLowerCase());
    }

    @EventListener
    public void placeOrderNotice(Order order) {
        sendEmail(order);
    }

}
```

sendEmail 是原本的发送邮件方法，增加一个 placeOrderNotice 方法，并加上@EventListener 注解，这样只要是Order 类型的消息都会到这个方法里来，然后调用原本的发送邮件方法

### 运行

```java
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
        order.setUserName("张三");
        order.setGoodsName("iphone X");
        orderService.placeOrder(order);
    }

}
```

编写一个单元测试运行一下
![](https://upload-images.jianshu.io/upload_images/9260441-c927584da8ad3e5d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![](https://upload-images.jianshu.io/upload_images/9260441-ad4ab9fb1fcfc0a7.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

正常业务都执行了

### 模拟异常

```java
    @Test
    public void placeOrder() {
        Order order = new Order();
        order.setUserName(null);
        order.setGoodsName("iphone X");
        orderService.placeOrder(order);
    }
```

单元测试的用户名设置为空，让邮件输出调用toLowerCase方法是报错

![](https://upload-images.jianshu.io/upload_images/9260441-2579580de5542e2f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![](https://upload-images.jianshu.io/upload_images/9260441-27e887b3981635ab.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

邮件报错，订单事务回滚了！这不是我们期望的结果呀

那能不能让我们的方法异步执行呢？答案肯定是可以的

### 开启异步执行

```java
@EnableAsync
@SpringBootApplication
public class SpringBootEventApplication {
```

在我们的启动类上增加一个 @EnableAsync 注解

```java
    @EventListener
    @Async
    public void placeOrderNotice(Order order) {
        sendEmail(order);
    }
```

在下单事件处理的方法上增加 @Async 异步调用注解

![](https://upload-images.jianshu.io/upload_images/9260441-86d261c04283d8fc.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当我们再次执行的时候单元测试执行通过了，但是控制台打印了邮件发送失败的消息，订单也入库了，说明符合我们的逾期结果

仔细看日志打印了一个

```
[cTaskExecutor-1] .a.i.SimpleAsyncUncaughtExceptionHandler
```
说明spring 是通过一个默认的线程池执行了这个发送邮件的方法，@Async 其实也支持指定你自己配置的线程池的

### 自定义线程池

```java
 @Bean
    public ThreadPoolTaskExecutor myExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(50);
        return executor;
    }
```
增加自定义线程池配置 myExecutor ，然后运行查看日志发现输出如下内容

```
2018-09-04 13:55:34.597 ERROR 7072 --- [   myExecutor-1]
```

说明已经在使用我们配置的线程池了

> 也可以增加多个 @EventListener 方法对下单做一连串的后续操作

> 当有多个下单处理的时候可以使用 @org.springframework.core.annotation.Order 注解来设置执行顺序

### 完整的项目结构

![](https://upload-images.jianshu.io/upload_images/9260441-53f86015d2b1fceb.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 更多精彩内容
* [架构实战篇（一）：Spring Boot 整合MyBatis](https://www.jianshu.com/p/5f76bc4bb7cf)
* [架构实战篇（二）：Spring Boot 整合Swagger2](https://www.jianshu.com/p/57a4381a2b45)
* [架构实战篇（三）：Spring Boot 整合MyBatis(二)](https://www.jianshu.com/p/b0668bf8cf60)
* [架构实战篇（四）：Spring Boot 整合 Thymeleaf](https://www.jianshu.com/p/b5a854c0e829)
* [架构实战篇（五）：Spring Boot 表单验证和异常处理](https://www.jianshu.com/p/5152c065d3cb)
* [架构实战篇（六）：Spring Boot RestTemplate的使用](https://www.jianshu.com/p/c96049624891)
* [架构实战篇（七）：Spring Boot Data JPA 快速入门](https://www.jianshu.com/p/9beec5b84a38)
* [架构实战篇（八）：Spring Boot 集成 Druid 数据源监控](https://www.jianshu.com/p/da2b1a069a2b)
* [架构实战篇（九）：Spring Boot 分布式Session共享Redis](https://www.jianshu.com/p/44130d6754c3)
* [架构实战篇（十三）：Spring Boot Logback 邮件通知](https://www.jianshu.com/p/9b3a3f3a7e87)
* [架构实战篇（十四）：Spring Boot 多缓存实战](https://www.jianshu.com/p/e4aa6c86dd59)

#### 关注我们

![](http://upload-images.jianshu.io/upload_images/9260441-fbb877c1ace32df7.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)