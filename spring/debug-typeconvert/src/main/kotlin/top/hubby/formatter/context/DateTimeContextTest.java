package top.hubby.formatter.context;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.format.datetime.standard.DateTimeContext;
import org.springframework.format.datetime.standard.DateTimeContextHolder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author asd <br>
 * @create 2022-03-01 3:04 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class DateTimeContextTest {

    /** 全局通用的日期-时间格式化器（当然还可以有日期专用的、时间专用的...） */
    public static final DateTimeFormatter GLOBAL_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(Locale.CHINA)
                    .withZone(ZoneId.of("Asia/Shanghai"))
                    .withChronology(IsoChronology.INSTANCE);

    @Test
    public void test1() throws InterruptedException {
        // 模拟请求参数(同一个参数，在不同接口里的不同表现)
        Instant start = Instant.now();

        // 模拟Controller的接口1：zoneId不一样
        new Thread(
                        () -> {
                            DateTimeContext context = new DateTimeContext();
                            context.setTimeZone(ZoneId.of("America/New_York"));
                            DateTimeContextHolder.setDateTimeContext(context);
                            // 基于全局的格式化器 + 自己的上下文自定义一个本接口专用的格式化器
                            DateTimeFormatter primaryFormatter =
                                    DateTimeContextHolder.getFormatter(
                                            GLOBAL_DATETIME_FORMATTER, null);

                            System.out.printf(
                                    "北京时间%s 接口1时间%s \n",
                                    GLOBAL_DATETIME_FORMATTER.format(start),
                                    primaryFormatter.format(start));
                        })
                .start();

        // 模拟Controller的接口2：Locale不一样
        new Thread(
                        () -> {
                            // 基于全局的格式化器 + 自己的上下文自定义一个本接口专用的格式化器
                            DateTimeFormatter primaryFormatter =
                                    DateTimeContextHolder.getFormatter(
                                            GLOBAL_DATETIME_FORMATTER, Locale.US);

                            System.out.printf(
                                    "北京时间%s 接口2时间%s \n",
                                    GLOBAL_DATETIME_FORMATTER.format(start),
                                    primaryFormatter.format(start));
                        })
                .start();

        TimeUnit.SECONDS.sleep(2);
    }
}
