package top.hubby.spring.debug.proxy.jdk;

import org.junit.Test;

/**
 * @author zack <br>
 * @create 2021-12-05 21:06 <br>
 * @project spring <br>
 */
public class ProxyTest {
    @Test
    public void testProxy() {
        System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
        ICalculator proxy = CalculatorProxy.getProxy(new Calculator());
        proxy.add(1, 1);
        System.out.println(proxy.getClass());
    }
}
