package top.hubby.spring.debug.proxy.cglib;

/**
 * @author zack <br>
 * @create 2021-12-05 21:12 <br>
 * @project spring <br>
 */
public class Calculator {
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
