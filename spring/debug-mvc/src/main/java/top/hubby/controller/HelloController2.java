package top.hubby.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import top.hubby.bean.User;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class HelloController2 extends AbstractController {
    @Override
    protected ModelAndView handleRequestInternal(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("hahha");
        List<User> userList = new ArrayList<>();
        User user1 = new User("张三", 12);
        User user2 = new User("李四", 21);
        userList.add(user1);
        userList.add(user2);
        return new ModelAndView("userlist", "users", userList);
    }
}
