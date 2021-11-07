package top.hubby.spring.debug.lifecycle.definition;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * @author zack <br>
 * @create 2021-11-07<br>
 * @project spring <br>
 */
@Slf4j
public class BeanDefinitionReaderSample {
    public static void main(String[] args) {

        // 注册中心
        // BeanDefinitionRegistry register = new SimpleBeanDefinitionRegistry();
        DefaultListableBeanFactory register = new DefaultListableBeanFactory();
        // 读取器
        XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(register);

        // 装载构建 bean 定义: resource-loader && location
        DefaultResourceLoader loader = new DefaultResourceLoader();
        xmlBeanDefinitionReader.loadBeanDefinitions(loader.getResource("beans.xml"));

        // org.springframework.context.annotation.internalCommonAnnotationProcessor
        // org.springframework.context.event.internalEventListenerProcessor
        // org.springframework.context.annotation.internalConfigurationAnnotationProcessor
        // beanInitAndDestroy
        // org.springframework.context.event.internalEventListenerFactory
        // personBean
        // org.springframework.context.annotation.internalAutowiredAnnotationProcessor
        // person4LifeCycle
        // customBeanFactoryPostProcessor
        Arrays.stream(register.getBeanDefinitionNames()).forEach(log::info);
    }
}
