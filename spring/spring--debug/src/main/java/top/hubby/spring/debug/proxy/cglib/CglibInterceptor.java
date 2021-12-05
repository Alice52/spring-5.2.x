package top.hubby.spring.debug.proxy.cglib;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author zack <br>
 * @create 2021-12-05 21:13 <br>
 * @project spring <br>
 */
public class CglibInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy)
            throws Throwable {
        return methodProxy.invokeSuper(o, objects);
    }
}
