## 1. Aware 接口的作用

1. 注释: 通过 callback 方法可以被 IOC 容器通知`[可以获取IOC容器中的Bean对象]`的标志 bean 接口
2. 可以通过自定义 XxAware 接口的方式从 IOC 容器中获取指定对象
3. 当 Spring 容器创建 Bean 对象时, 如果需要容器中的其他对象, 可以通过实现 Aware 接口来获取需要的对象

## 2. spring 在不同的阶段要处理不同的工作, 怎么实现

1. 观察者模式: 监听器, 监听事件, 多播器[广播器]

## 3. BeanFactory 与 FactoryBean 的区别

1. 都是用来创建对象的
2. 当使用 BeanFactory[ioc 的基础] 的时候必须遵循完整的创建过程: spring 管理的流程
3. 当使用 FactoryBean[适合复杂对象] 的时候, 只需要调用 getObject 就可以得到具体的对象[对象的创建过程是用户自定义的{灵活}]
4. FactoryBean 在容器创建好之后时没有 Person 对象的, 只有 PersonFactoryBean, 当调用 getObject() 才会创建 Person 并放入 IOC 容器中; 第二次则可以直接从 ioc 容器中 获取

## 4. BeanFactory 与 ApplicationContext 的区别

1. applicationContext 具有 beanFactory 的所有功能
2. 还有 MessageSource 国际化的消息访问
3. 资源访问: URL + 文件
4. 事件传播: ApplicaitionListener
5. 载入多个有继承关系的上下文, 使的每个上下文都专注于自己的层次, 如 web 层
