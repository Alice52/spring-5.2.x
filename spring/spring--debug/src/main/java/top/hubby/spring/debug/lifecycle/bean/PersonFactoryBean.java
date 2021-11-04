package top.hubby.spring.debug.lifecycle.bean;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;
import top.hubby.spring.debug.common.model.Person;

/**
 * @author zack <br>
 * @create 2021-11-04 22:27 <br>
 * @project spring <br>
 */
@Component("personBean")
public class PersonFactoryBean implements FactoryBean<Person> {

    @Override
    public Person getObject() throws Exception {
        return new Person(18, "factory");
    }

    @Override
    public Class<?> getObjectType() {
        return Person.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
