package top.hubby.formatter;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.springframework.format.number.CurrencyStyleFormatter;
import org.springframework.format.number.NumberStyleFormatter;

import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Locale;

/**
 * @author asd <br>
 * @create 2022-02-22 3:25 PM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class NumberFormatterTest {
    @Test
    public void testNumberStyleFormatter() throws ParseException {
        NumberStyleFormatter formatter = new NumberStyleFormatter();

        double myNum = 1220.0455;
        log.info("{}", formatter.print(myNum, Locale.getDefault()));

        formatter.setPattern("#.##");
        log.info("{}", formatter.print(myNum, Locale.getDefault()));

        // 转换
        Number parsedResult = formatter.parse("1220.045", Locale.getDefault());
        log.info("{}", parsedResult.getClass() + "-->" + parsedResult);
    }

    @Test
    public void testPercentStyleFormatter() throws ParseException {
        CurrencyStyleFormatter formatter = new CurrencyStyleFormatter();

        double myNum = 1220.0455;
        log.info("{}", formatter.print(myNum, Locale.getDefault()));

        log.info("{}", "--------------定制化--------------");
        // 指定货币种类（如果你知道的话）
        // formatter.setCurrency(Currency.getInstance(Locale.getDefault()));
        // 指定所需的分数位数。默认是2
        formatter.setFractionDigits(1);
        // 舍入模式。默认是RoundingMode#UNNECESSARY
        formatter.setRoundingMode(RoundingMode.CEILING);
        // 格式化数字的模版
        formatter.setPattern("#.#¤¤");

        log.info("{}", formatter.print(myNum, Locale.getDefault()));

        // 转换
        // Number parsedResult = formatter.parse("￥1220.05", Locale.getDefault());
        // Number parsedResult = formatter.parse("1220.1CNY", Locale.getDefault());
        // log.info("{}", parsedResult.getClass() + "-->" + parsedResult);
    }
}
