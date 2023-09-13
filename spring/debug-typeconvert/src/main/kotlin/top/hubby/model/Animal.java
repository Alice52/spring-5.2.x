package top.hubby.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author asd <br>
 * @create 2022-01-19 4:37 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
@Data
public abstract class Animal {
    private Long id;
    private String name;
}
