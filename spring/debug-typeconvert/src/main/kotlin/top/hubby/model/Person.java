package top.hubby.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Date;

/**
 * @author asd <br>
 * @create 2022-01-19 5:10 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    private Long id;
    private String name;
    private Cat cat;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Date secBirthday;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date birthday;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate newBirthday;

    public Person(LocalDate newBirthday) {
        this.newBirthday = newBirthday;
    }

    public Person(Date birthday) {
        this.birthday = birthday;
    }

    public Person(Customer customer) {
        this.id = customer.getId();
        this.name = customer.getName();
    }

    public Person(Long id, String name, Cat cat) {
        this.id = id;
        this.name = name;
        this.cat = cat;
    }

    /** 方法名称可以是：valueOf、of、from */
    public static Person valueOf(Customer customer) {
        Person person = new Person();
        person.setId(customer.getId());
        person.setName("YourBatman-".concat(customer.getName()));
        return person;
    }

    /** 根据 ID 定位一个Person实例 */
    public static Person findPerson(Long id) {
        // 一般根据id从数据库查, 本处通过new来模拟
        Person person = new Person();
        person.setId(id);
        person.setName("YourBatman-byFindPerson");
        return person;
    }
}
