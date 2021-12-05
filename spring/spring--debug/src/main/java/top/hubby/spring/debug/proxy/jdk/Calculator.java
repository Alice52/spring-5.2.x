package top.hubby.spring.debug.proxy.jdk;

/**
 * @author zack <br>
 * @create 2021-12-05 21:05 <br>
 * @project spring <br>
 */
public class Calculator implements ICalculator {
    public int add(int i, int j) {
        int result = i + j;
        return result;
    }

    public int sub(int i, int j) {
        int result = i - j;
        return result;
    }

    public int mult(int i, int j) {
        int result = i * j;
        return result;
    }

    public int div(int i, int j) {
        int result = i / j;
        return result;
    }
}
