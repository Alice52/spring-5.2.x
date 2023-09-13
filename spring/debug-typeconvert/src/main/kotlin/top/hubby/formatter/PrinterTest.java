package top.hubby.formatter;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Printer;
import org.springframework.format.support.FormattingConversionService;

import top.hubby.model.Person;

import java.util.Locale;

/**
 * @author asd <br>
 * @create 2022-02-22 5:54 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class PrinterTest {

    @Test
    public void testFormatting() {
        FormattingConversionService formattingConversionService = new FormattingConversionService();

        // 注册格式化器: Person -> String 由 LongPrinter 处理
        formattingConversionService.addFormatterForFieldType(Person.class, new LongPrinter(), null);
        // 强调：此处绝不能使用lambda表达式代替，否则泛型类型丢失，结果将出错
        formattingConversionService.addConverter(Person.class, Long.class, Person::getId);

        log.info("{}", formattingConversionService.canConvert(Long.class, String.class));
        log.info("{}", formattingConversionService.canConvert(Person.class, String.class));

        // 最终均使用ConversionService统一提供服务转换: person --> long-printer --> string
        log.info(
                "{}",
                formattingConversionService.convert(
                        new Person(1L, "YourBatman", null), String.class));
    }

    @Test
    public void test2() {
        FormattingConversionService formattingConversionService = new FormattingConversionService();

        // 注册格式化器
        formattingConversionService.addPrinter(new LongPrinter());

        // 强调：此处绝不能使用lambda表达式代替，否则泛型类型丢失，结果将出错
        formattingConversionService.addConverter(
                new Converter<Person, Long>() {
                    @Override
                    public Long convert(Person source) {
                        return source.getId();
                    }
                });

        // 最终均使用ConversionService统一提供服务转换
        log.info("{}", formattingConversionService.canConvert(Long.class, String.class));
        log.info("{}", formattingConversionService.canConvert(Person.class, Long.class));
        log.info("{}", formattingConversionService.convert(1L, String.class));
        // No converter found capable of converting from type [model.Person] to type [String]
        log.info(
                "{}",
                formattingConversionService.convert(
                        new Person(1L, "YourBatman", null), String.class));
    }

    static class LongPrinter implements Printer<Long> {

        @Override
        public String print(Long object, Locale locale) {
            object += 10;
            return object.toString();
        }
    }
}
