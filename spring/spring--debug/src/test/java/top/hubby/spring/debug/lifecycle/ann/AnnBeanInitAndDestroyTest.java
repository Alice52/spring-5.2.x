package top.hubby.spring.debug.lifecycle.ann;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.hubby.spring.debug.common.model.Person;
import top.hubby.spring.debug.lifecycle.bean.ann.BeanInitAndDestroy;

/**
 * @author zack <br>
 * @create 2021-11-04 22:48 <br>
 * @project spring <br>
 */
public class AnnBeanInitAndDestroyTest {

    private static final Logger log = LoggerFactory.getLogger(AnnBeanInitAndDestroyTest.class);

    private ApplicationContext applicationContext =
            new AnnotationConfigApplicationContext(BeanInitAndDestroy.class);

    @Test
    public void testBeanInitAndDestroy() {
        Person person = applicationContext.getBean(Person.class);
        log.info("{}", person);

        AnnotationConfigApplicationContext context =
                (AnnotationConfigApplicationContext) this.applicationContext;
        context.close();
    }
}
