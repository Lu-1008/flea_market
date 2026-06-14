package com.market.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan({"com.market.user.dao", "com.market.product.dao", "com.market.order.dao"})
@ComponentScan(basePackages = {"com.market.web", "com.market.user", "com.market.product", "com.market.order", "com.market.file"})
public class FleaMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleaMarketApplication.class, args);
    }

}
