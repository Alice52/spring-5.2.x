package top.hubby.converter;


import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import top.hubby.model.Customer;
import top.hubby.model.Person;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author asd <br>
 * @create 2022-02-09 2:04 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class ConverterTests {
    @Test
    public void testObjectToObjectConverter() {

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Peking");

        ConversionService conversionService = new DefaultConversionService();
        Person person = conversionService.convert(customer, Person.class);
        log.info("{}", person);

        Customer c2 = conversionService.convert(customer, Customer.class);
        assert c2 == customer;
    }

    @Test
    public void testIdToEntityConverter() {
        ConversionService conversionService = new DefaultConversionService();
        Person person = conversionService.convert("1", Person.class);
        log.info("{}", person);
    }

    @Test
    public void testObjectToOptionalConverter() {
        ConversionService conversionService = new DefaultConversionService();
        Optional result = conversionService.convert(Arrays.asList(2), Optional.class);

        log.info("{}", result);
    }
}
