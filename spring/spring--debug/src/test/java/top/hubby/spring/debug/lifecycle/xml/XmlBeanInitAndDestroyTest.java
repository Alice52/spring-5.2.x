package top.hubby.spring.debug.lifecycle.xml;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import top.hubby.spring.debug.common.model.Person;
import top.hubby.spring.debug.extension.CustomClassPathXmlApplicationContext;

import java.util.Map;

/**
 * @author zack <br>
 * @create 2021-11-04 22:45 <br>
 * @project spring <br>
 */
@Slf4j
public class XmlBeanInitAndDestroyTest {

    ApplicationContext ac = new CustomClassPathXmlApplicationContext("beans.xml");

    @Test
    public void testBeanInitAndDestroy() {
        Object zack = ac.getBean("person4LifeCycle");
        log.info("[{}]zack: {}", zack.hashCode(), zack);

        final Map<String, Person> beans = ac.getBeansOfType(Person.class);
        log.info("person beans: {}", beans);

        ClassPathXmlApplicationContext context = (ClassPathXmlApplicationContext) this.ac;
        context.close();
    }
}
