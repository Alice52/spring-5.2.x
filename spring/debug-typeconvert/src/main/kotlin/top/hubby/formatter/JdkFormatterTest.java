package top.hubby.formatter;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import java.text.*;
import java.util.Date;

/**
 * @author asd <br>
 * @create 2022-02-22 10:00 AM <br>
 * @project boot-typeconvert <br>
 */
@Slf4j
public class JdkFormatterTest {

    @Test
    public void testMFUsage() {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into user ( name, accountId, zhName, enname,  status ) values (");
        // 字符串
        sb.append("  ''{0}'',");
        sb.append("  {1},");
        sb.append("  ''{2}'',");
        sb.append("  ''{3}'',");
        sb.append("  {4},");
        sb.append(")");

        log.info(sb.toString());
    }

    @Test
    public void testMessageFormat() {
        String sourceStrPattern = "Hello {0},my name is {1}";
        String formatedStr = MessageFormat.format(sourceStrPattern, "girl", "YourBatman");
        log.info("{}", formatedStr);

        MessageFormat messageFormat =
                new MessageFormat(
                        "Hello, my name is {0}. I’am {1,number,#.##} years old. Today is {2}");
        messageFormat.setFormatByArgumentIndex(2, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        log.info("{}", messageFormat.format(new Object[] {"YourBatman", 24.123456, new Date()}));

        log.info("{}", "---------------------------------");
        log.info("{}", MessageFormat.format("{1} - {1}", new Object[] {1})); // {1} - {1}
        log.info("{}", MessageFormat.format("{0} - {1}", new Object[] {1})); // 输出：1 - {1}
        log.info("{}", MessageFormat.format("{0} - {1}", new Object[] {1, 2, 3})); // 输出：1 - 2

        log.info("{}", MessageFormat.format("'{0} - {1}", new Object[] {1, 2})); // 输出：{0} - {1}
        log.info("{}", MessageFormat.format("''{0} - {1}", new Object[] {1, 2})); // 输出：'1 - 2
        log.info("{}", MessageFormat.format("'{0}' - {1}", new Object[] {1, 2})); // {0} - 2
        // 若你数据库值两边都需要''包起来，请你这么写
        log.info("{}", MessageFormat.format("''{0}'' - {1}", new Object[] {1, 2})); // '1' - 2

        log.info("{}", "---------------------------------");
        log.info("{}", MessageFormat.format("0} - {1}", new Object[] {1, 2})); // 0} - 2
        // log.info("{}", MessageFormat.format("{0 - {1}", 1, 2));
        // java.lang.IllegalArgumentException: Unmatched braces in the pattern.

    }

    @Test
    public void testNumberFormat() {
        double myNum = 1220.0455;

        log.info(
                "{}",
                NumberFormat.getInstance().getClass()
                        + "-->"
                        + NumberFormat.getInstance().format(myNum));
        log.info(
                "{}",
                NumberFormat.getCurrencyInstance().getClass()
                        + "-->"
                        + NumberFormat.getCurrencyInstance().format(myNum));
        log.info(
                "{}",
                NumberFormat.getIntegerInstance().getClass()
                        + "-->"
                        + NumberFormat.getIntegerInstance().format(myNum));
        log.info(
                "{}",
                NumberFormat.getNumberInstance().getClass()
                        + "-->"
                        + NumberFormat.getNumberInstance().format(myNum));
        log.info(
                "{}",
                NumberFormat.getPercentInstance().getClass()
                        + "-->"
                        + NumberFormat.getPercentInstance().format(myNum));
    }

    @Test
    public void testDecimalFormat() {
        double myNum = 1220.0455;

        log.info("{}", "===============0 的使用===============");
        log.info("{}", "只保留整数部分：" + new DecimalFormat("0").format(myNum));
        log.info("{}", "保留3位小数：" + new DecimalFormat("0.000").format(myNum));
        log.info(
                "{}",
                "整数部分、小数部分都5位。不够的都用0补位(整数高位部，小数低位补)："
                        + new DecimalFormat("00000.00000").format(myNum));

        log.info("{}", "===============# 的使用===============");
        log.info("{}", "只保留整数部分：" + new DecimalFormat("#").format(myNum));
        log.info("{}", "保留2位小数并以百分比输出：" + new DecimalFormat("#.##%").format(myNum));

        // 非标准数字: 不建议这么用
        log.info("{}", "===============非标准数字的使用===============");
        log.info("{}", new DecimalFormat("666").format(myNum));
        log.info("{}", new DecimalFormat(".6666").format(myNum));

        log.info("{}", "===============科学计数法E===============");
        log.info("{}", new DecimalFormat("0E0").format(myNum));
        log.info("{}", new DecimalFormat("0E00").format(myNum));
        log.info("{}", new DecimalFormat("00000E00000").format(myNum));
        log.info("{}", new DecimalFormat("#E0").format(myNum));
        log.info("{}", new DecimalFormat("#E00").format(myNum));
        log.info("{}", new DecimalFormat("#####E00000").format(myNum));

        log.info("{}", "===============分组分隔符,===============");
        log.info("{}", new DecimalFormat(",###").format(myNum));
        log.info("{}", new DecimalFormat(",##").format(myNum));
        log.info("{}", new DecimalFormat(",##").format(123456789));
        // 分隔符,左边是无效的
        log.info("{}", new DecimalFormat("###,##").format(myNum));

        log.info("{}", "===============百分号%===============");
        log.info("{}", "百分位表示：" + new DecimalFormat("#.##%").format(myNum));
        log.info("{}", "千分位表示：" + new DecimalFormat("#.##\u2030").format(myNum));

        log.info("{}", "===============本地货币符号¤===============");
        log.info("{}", new DecimalFormat(",000.00¤").format(myNum));
        log.info("{}", new DecimalFormat(",000.¤00").format(myNum));
        log.info("{}", new DecimalFormat("¤,000.00").format(myNum));
        log.info("{}", new DecimalFormat("¤,000.¤00").format(myNum));
        // 世界货币表达形式
        log.info("{}", new DecimalFormat(",000.00¤¤").format(myNum));
    }

    @Test
    public void testChoiceFormat() {
        double[] limits = {1, 2, 3, 4, 5, 6, 7};
        String[] formats = {"周一", "周二", "周三", "周四", "周五", "周六", "周天"};
        NumberFormat numberFormat = new ChoiceFormat(limits, formats);

        log.info("{}", numberFormat.format(1));
        log.info("{}", numberFormat.format(4.3));
        log.info("{}", numberFormat.format(5.8));
        log.info("{}", numberFormat.format(9.1));
        log.info("{}", numberFormat.format(11));
    }
}
