package top.hubby.spring.debug.extension;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author zack <br>
 * @create 2021-11-07<br>
 * @project spring <br>
 */
@Slf4j
@NoArgsConstructor
public class CustomClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

    public CustomClassPathXmlApplicationContext(String... configLocations) throws BeansException {
        super(configLocations);
    }

    /**
     * 扩展: 属性相关 <br>
     * 可以做一些属性验证或者业务
     */
    @Override
    protected void initPropertySources() {
        super.initPropertySources();
        log.info("execute initPropertySources method");
        // validate by validateRequiredProperties
        getEnvironment().setRequiredProperties("username");
    }

    /**
     * 扩展: BeanFactory 相关配置
     *
     * @param beanFactory the newly created bean factory for this context
     */
    @Override
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        log.info("execute customizeBeanFactory method");

        /**
         * AllowBeanDefinitionOverriding
         *
         * <pre>
         *    1. <lookup-method/> & <replace-method/>
         *    2. 允许 BeanDefinition 的重写
         * </pre>
         */
        beanFactory.setAllowBeanDefinitionOverriding(false);
        /**
         * AllowCircularReferences
         *
         * <pre>
         *    1. 是否自动解决循环依赖
         * </pre>
         */
        super.setAllowCircularReferences(false);
        super.customizeBeanFactory(beanFactory);
    }
}
