package top.hubby.spring.debug.proxy.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @author zack <br>
 * @create 2021-12-05 21:05 <br>
 * @project spring <br>
 */
public class CalculatorProxy {
    public static ICalculator getProxy(final Calculator calculator) {
        ClassLoader loader = calculator.getClass().getClassLoader();
        Class<?>[] interfaces = calculator.getClass().getInterfaces();
        InvocationHandler h = (proxy, method, args) -> method.invoke(calculator, args);
        return (ICalculator) Proxy.newProxyInstance(loader, interfaces, h);
    }
}
