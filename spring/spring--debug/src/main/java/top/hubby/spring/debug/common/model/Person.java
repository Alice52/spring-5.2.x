package top.hubby.spring.debug.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * @author zack <br>
 * @create 2021-11-01 23:15 <br>
 * @project spring <br>
 */
@Builder
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    private int age;
    private Date birthDay;
    private String name;
    private boolean gender;

    public Person(int age, String name) {
        this.age = age;
        this.name = name;
    }

    public void init() {
        log.info("Person object init method execute.");
    }

    public void destroy() {
        log.info("Person object destroy method execute.");
    }
}
