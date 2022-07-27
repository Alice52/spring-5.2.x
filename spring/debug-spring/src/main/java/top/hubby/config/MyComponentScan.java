package top.hubby.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ComponentScan("top.hubby.selftag")
public class MyComponentScan {

    @ComponentScan("top.hubby.selftag")
    @Configuration
    @Order(90)
    class InnerClass {}
}
