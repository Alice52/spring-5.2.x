package top.hubby.spring.debug.lifecycle.definition;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @author zack <br>
 * @create 2021-11-04 22:09 <br>
 * @project spring <br>
 */
@Slf4j
@Component
public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        final BeanDefinition definition = beanFactory.getBeanDefinition("person4LifeCycle");
        log.info("xml bean definition: {}", definition);
    }
}
