## this repo code is fork from spring.io

1. version:
   - 5.2.18.BUILD-SNAPSHOT

## source code

### 1.FactoryBean & BeanFactory

### 2.@Configuration + ConfigurationClassPostProcessor

### 3.@Transactional 源码及常见的失效情况分析

1. 源码
2. 事务隔离级别
3. 失效问题

### 4.AOP 原理实现: AspectJ/Proxy

1. aop 实现源码
2. aop 4/5 的执行顺序

### 5.IOC 实现原理

### 6.Bean 的生命周期

1. 创建流程
2. 创建方式
3. bean 注入获取方式

### 7.BeanFactory & FactoryBean & getBean 流程

1. BeanFactory
2. FactoryBean
3. getBean 流程

### 8.Spring 中常见的接口

### 9.循环依赖

### 10.Validattion 源码

### 11.Async 源码

### 12.Autowired & Resource 源码

### 13.Spring 中的设计模式

### 14.单例的 Bean 如何注入 Prototype 的 Bean 这个问题

1. Bean 默认是单例的，所以单例的 Controller 注入的 Service 也是一次性创建的，即使 Service 本身标识了 prototype 的范围也没用。
2. @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
3. 会注入 两个 beanDefnition: original, **target.original**[primary]
