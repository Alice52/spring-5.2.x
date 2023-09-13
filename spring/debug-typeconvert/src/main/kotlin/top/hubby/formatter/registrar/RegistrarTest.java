package top.hubby.formatter.registrar;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;

import top.hubby.model.Person;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author asd <br>
 * @create 2022-02-28 4:25 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class RegistrarTest {
    @Test
    public void testDateFormatterRegistrar() {
        FormattingConversionService conversionService = new FormattingConversionService();
        // 注册员负责添加格式化器以支持Date系列的转换
        new DateFormatterRegistrar().registerFormatters(conversionService);

        // 1、普通使用
        long currMills = System.currentTimeMillis();
        log.info("{}", "当前时间戳：" + currMills);
        // Date -> Calendar
        log.info("{}", conversionService.convert(new Date(currMills), Calendar.class));
        // Long ->  Date
        log.info("{}", conversionService.convert(currMills, Date.class));
        // Calendar -> Long
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(currMills);
        log.info("{}", conversionService.convert(calendar, Long.class));
    }

    @Test
    public void testDateFormatterRegistrar4Bean() {
        FormattingConversionService conversionService = new FormattingConversionService();
        DefaultConversionService.addDefaultConverters(conversionService);
        // 注册员负责添加格式化器以支持Date系列的转换
        new DateFormatterRegistrar().registerFormatters(conversionService);

        // 1、注解使用
        Person son = Person.builder().secBirthday(new Date()).build();
        // 输出：将Date类型输出为Long类型
        log.info("{}", conversionService.convert(son.getSecBirthday(), Long.class));
        // 输出：将String烈性输入为Date类型
        // log.info("{}", conversionService.convert("2021-02-12", Date.class)); // 报错
        log.info("{}", conversionService.convert(1613034123709L, Date.class));
    }

    @Test
    public void test1() {
        FormattingConversionService conversionService = new FormattingConversionService();
        // 注册员负责添加格式化器以支持Date系列的转换
        new DateTimeFormatterRegistrar().registerFormatters(conversionService);

        // 1、普通使用(API方式)
        LocalDateTime now = LocalDateTime.now();
        log.info("{}", "当前时间：" + now);
        log.info(
                "{}",
                "LocalDateTime转为LocalDate：" + conversionService.convert(now, LocalDate.class));
        log.info(
                "{}",
                "LocalDateTime转为LocalTime：" + conversionService.convert(now, LocalTime.class));

        // 时间戳转Instant
        long currMills = System.currentTimeMillis();
        log.info("{}", "当前时间戳：" + currMills);
        log.info("{}", "时间戳转Instant：" + conversionService.convert(currMills, Instant.class));
    }

    public void test() {
        FormattingConversionService object =
                new FormattingConversionServiceFactoryBean().getObject();
    }
}
