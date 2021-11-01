package top.hubby.spring.debug;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author zack <br>
 * @create 2021-11-01 22:07 <br>
 * @project spring <br>
 */
@Slf4j
public class Flow {

    public static void main(String[] args) {

        ApplicationContext ac = new ClassPathXmlApplicationContext("application.xml");
        Object zack = ac.getBean("person4LifeCycle");

        log.info("{}", zack);
    }
}
