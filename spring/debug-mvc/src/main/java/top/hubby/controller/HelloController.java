package top.hubby.controller;

import java.util.ArrayList;
import java.util.List;

import top.hubby.bean.User;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HelloController {

    @RequestMapping("/userlist")
    public String hello(Model model) {
        System.out.println("hahha");
        List<User> userList = new ArrayList<>();
        User user1 = new User("张三", 12);
        User user2 = new User("李四", 21);
        userList.add(user1);
        userList.add(user2);
        model.addAttribute("users", userList);
        return "userlist";
    }
}
