package top.hubby.formatter;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.datetime.DateTimeFormatAnnotationFormatterFactory;
import org.springframework.format.datetime.standard.Jsr310DateTimeFormatAnnotationFormatterFactory;

import top.hubby.model.Person;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author asd <br>
 * @create 2022-02-25 3:35 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class AnnotationTests {

    @Test
    public void testLocalDate() throws NoSuchFieldException, ParseException {
        AnnotationFormatterFactory annotationFormatterFactory =
                new Jsr310DateTimeFormatAnnotationFormatterFactory();

        // 找到该field
        Field field = Person.class.getDeclaredField("newBirthday");
        DateTimeFormat annotation = field.getAnnotation(DateTimeFormat.class);
        Class<?> type = field.getType();

        // 输出：
        log.info("{}", "输出：LocalDate -> String====================");
        Printer printer = annotationFormatterFactory.getPrinter(annotation, type);
        Person father = new Person(LocalDate.now());
        log.info("{}", printer.print(father.getNewBirthday(), Locale.US));

        // 输入：
        log.info("{}", "输入：String -> Date====================");
        Parser parser = annotationFormatterFactory.getParser(annotation, type);
        Object output = parser.parse("2021-02-07", Locale.US);
        father = new Person((LocalDate) output);
        log.info("{}", father);
    }

    @Test
    public void testDateAnno() throws Exception {
        AnnotationFormatterFactory annotationFormatterFactory =
                new DateTimeFormatAnnotationFormatterFactory();

        Field field = Person.class.getDeclaredField("birthday");
        DateTimeFormat annotation = field.getAnnotation(DateTimeFormat.class);
        Class<?> type = field.getType();

        log.info("{}", "输出：Date -> String====================");
        Printer printer = annotationFormatterFactory.getPrinter(annotation, type);
        Person person = new Person(new Date());
        log.info("{}", printer.print(person.getBirthday(), Locale.US));

        log.info("{}", "输入：String -> Date====================");
        Parser parser = annotationFormatterFactory.getParser(annotation, type);
        Object output = parser.parse("2021-02-06 19:00:00", Locale.US);
        person = new Person((Date) output);
        log.info("{}", person);
    }

    @Test
    public void test2() throws NoSuchMethodException, ParseException {
        AnnotationFormatterFactory annotationFormatterFactory =
                new DateTimeFormatAnnotationFormatterFactory() {
                    @Override
                    public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
                        if (fieldType.isAssignableFrom(Calendar.class)) {
                            return (Parser<Calendar>)
                                    (text, locale) -> {
                                        // 先翻译为Date
                                        Formatter<Date> formatter =
                                                getFormatter(annotation, fieldType);
                                        Date date = formatter.parse(text, locale);

                                        // 再翻译为Calendar
                                        Calendar calendar =
                                                Calendar.getInstance(TimeZone.getDefault());
                                        calendar.setTime(date);
                                        return calendar;
                                    };
                        }
                        return super.getParser(annotation, fieldType);
                    }
                };

        // 拿到方法入参
        Method method = this.getClass().getDeclaredMethod("method", Calendar.class);
        Parameter parameter = method.getParameters()[0];
        DateTimeFormat annotation = parameter.getAnnotation(DateTimeFormat.class);
        Class<?> type = parameter.getType();

        // 输入：
        log.info("{}", "输入：String -> Calendar====================");
        Parser parser = annotationFormatterFactory.getParser(annotation, type);
        Object output = parser.parse("2021-02-06 19:00:00", Locale.US);
        // 给该方法传入“转换好的”参数，表示输入
        method((Calendar) output);
    }

    public void method(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Calendar calendar) {
        log.info("{}", calendar);
        log.info("{}", calendar.getTime());
    }
}
