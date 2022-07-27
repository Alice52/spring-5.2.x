package top.hubby.tx.annotation;

import top.hubby.tx.annotation.config.TransactionConfig;
import top.hubby.tx.annotation.dao.BookDao;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TransactionTest {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext =
                new AnnotationConfigApplicationContext();
        applicationContext.register(TransactionConfig.class);
        applicationContext.refresh();
        //        BookService bean = applicationContext.getBean(BookService.class);
        //        bean.checkout("zhangsan",1);
        BookDao bean = applicationContext.getBean(BookDao.class);
        bean.test();
    }
}
