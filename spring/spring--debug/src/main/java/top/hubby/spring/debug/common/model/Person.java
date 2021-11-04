package top.hubby.spring.debug.common.model;

import lombok.Data;

import java.util.Date;

/**
 * @author zack <br>
 * @create 2021-11-01 23:15 <br>
 * @project spring <br>
 */
@Data
public class Person {
    private int age;
    private Date birthDay;
    private String name;
    private boolean gender;
}
