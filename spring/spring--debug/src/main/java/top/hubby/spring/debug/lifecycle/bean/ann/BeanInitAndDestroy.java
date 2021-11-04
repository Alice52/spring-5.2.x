package top.hubby.spring.debug.lifecycle.bean.ann;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import top.hubby.spring.debug.common.model.Person;

/**
 * @author zack <br>
 * @create 2021-11-04 22:52 <br>
 * @project spring <br>
 */
@Configuration
public class BeanInitAndDestroy {

    /**
     * Scope:
     *
     * <pre>
     * singleton: <br>
     *  - init: create an object when the container is created, and call the Init () method <br>
     *  -destroy: called when the container is closed <br>
     * prototype: <br>
     *  -init: the object will be created when it is used in the first time, and the Init () method is called <br>
     *  -destroy: The container will only create this Bean but will not destroy [Manage] <br>
     * </pre>
     *
     * @return Person
     */
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Bean(value = "personAnn", initMethod = "init", destroyMethod = "destroy")
    public Person injectPerson() {

        return new Person(18, "personAnn");
    }
}
