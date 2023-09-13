package top.hubby.formatter;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.format.datetime.standard.DateTimeFormatterFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * @author asd <br>
 * @create 2022-02-22 3:13 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class Jsr310DateTimeTest {

    @Test
    public void testDateTimeFormatter() {
        log.info(
                "{}",
                new DateTimeFormatterFactory("yyyy-MM-dd HH:mm:ss")
                        .createDateTimeFormatter()
                        .format(LocalDateTime.now()));
        log.info(
                "{}",
                new DateTimeFormatterFactory("yyyy-MM-dd")
                        .createDateTimeFormatter()
                        .format(LocalDate.now()));
        log.info(
                "{}",
                new DateTimeFormatterFactory("HH:mm:ss")
                        .createDateTimeFormatter()
                        .format(LocalTime.now()));
        log.info(
                "{}",
                new DateTimeFormatterFactory("yyyy-MM-dd HH:mm:ss")
                        .createDateTimeFormatter()
                        .format(ZonedDateTime.now()));
    }
}
