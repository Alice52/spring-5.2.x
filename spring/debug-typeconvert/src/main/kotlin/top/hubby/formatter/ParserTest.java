package top.hubby.formatter;


import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Parser;
import org.springframework.format.datetime.standard.Jsr310DateTimeFormatAnnotationFormatterFactory;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.NumberUtils;

import top.hubby.model.Person;

import java.text.ParseException;
import java.util.Locale;

/**
 * @author asd <br>
 * @create 2022-02-23 2:13 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class ParserTest {

    @Test
    public void test5() {
        FormattingConversionService formattingConversionService = new FormattingConversionService();
        formattingConversionService.addFormatterForFieldAnnotation(
                new Jsr310DateTimeFormatAnnotationFormatterFactory());
    }

    @Test
    public void test4() {
        FormattingConversionService formattingConversionService = new FormattingConversionService();
        // string --> person: LongParser[只能将 string 转成 Long] 处理
        formattingConversionService.addFormatterForFieldType(
                Person.class, new PrinterTest.LongPrinter(), new LongParser());
        // 因此需要一个将 Long --> person 的转换器
        formattingConversionService.addConverter(
                new Converter<Long, Person>() {
                    @Override
                    public Person convert(Long source) {
                        return new Person(source, "YourBatman", null);
                    }
                });

        log.info("{}", formattingConversionService.canConvert(String.class, Person.class));
        log.info("{}", formattingConversionService.convert("1", Person.class));
    }

    @Test
    public void test3() {
        FormattingConversionService formattingConversionService = new FormattingConversionService();
        formattingConversionService.addParser(new LongParser());

        log.info("{}", formattingConversionService.canConvert(String.class, Long.class));
        log.info("{}", formattingConversionService.convert("1", Long.class));
    }

    private static class LongParser implements Parser<Long> {

        @Override
        public Long parse(String text, Locale locale) throws ParseException {
            return NumberUtils.parseNumber(text, Long.class);
        }
    }
}
