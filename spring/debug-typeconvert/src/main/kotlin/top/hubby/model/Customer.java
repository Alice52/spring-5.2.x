package top.hubby.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author asd <br>
 * @create 2022-02-09 2:03 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
@Data
public class Customer {

    private Long id;
    private String name;

    public Person toPerson() {
        Person person = new Person();
        person.setId(getId());
        person.setName("YourBatman-".concat(getName()));
        return person;
    }
}
