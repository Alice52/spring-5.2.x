package top.hubby.spring.debug.proxy.cglib;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.proxy.Enhancer;
import top.hubby.spring.debug.proxy.jdk.Calculator;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * @author zack <br>
 * @create 2021-12-05 21:06 <br>
 * @project spring <br>
 */
@Slf4j
public class ProxyTest {

    @SneakyThrows
    public static void saveGeneratedCGlibProxyFiles(String dir) {
        Field field = System.class.getDeclaredField("props");
        field.setAccessible(true);
        Properties props = (Properties) field.get(null);
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, dir); // dir为保存文件路径
        props.put("net.sf.cglib.core.DebuggingClassWriter.traceEnabled", "true");
    }

    @Test
    public void testProxy() {
        // 动态代理创建的class文件存储到本地
        saveGeneratedCGlibProxyFiles(System.getProperty("user.dir") + "/proxy/cglib");
        // 通过cglib动态代理获取代理对象的过程，创建调用的对象,在后续创建过程中EnhanceKey的对象
        // 所以在进行enhancer对象创建的时候需要把EnhancerKey（newInstance）对象准备好,恰好这个对象也需要动态代理来生成
        Enhancer enhancer = new Enhancer();
        // 设置enhancer对象的父类
        enhancer.setSuperclass(Calculator.class);
        // 设置enhancer的回调对象
        enhancer.setCallback(new CglibInterceptor());
        // 创建代理对象
        Calculator myCalculator = (Calculator) enhancer.create();
        // 通过代理对象调用目标方法
        myCalculator.add(1, 1);
        log.info("{}", myCalculator.getClass());
    }
}
