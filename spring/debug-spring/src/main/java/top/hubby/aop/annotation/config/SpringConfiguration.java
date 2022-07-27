package top.hubby.aop.annotation.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan(basePackages = "top.hubby.aop.annotation")
@EnableAspectJAutoProxy
public class SpringConfiguration {}
