## 单例的 Bean 如何注入 Prototype 的 Bean 这个问题

1. Bean 默认是单例的，所以单例的 Controller 注入的 Service 也是一次性创建的，即使 Service 本身标识了 prototype 的范围也没用。
2. @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
3. 会注入 两个 beanDefnition: original, **target.original**[primary]

## reference

1. https://github.com/Alice52/spring-5.2.x/issues/6
