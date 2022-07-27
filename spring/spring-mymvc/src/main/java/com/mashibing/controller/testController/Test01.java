package com.mashibing.controller.testController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class Test01 implements Controller {

    @Override
    public ModelAndView handleRequest(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws Exception {
        System.out.println("Controller接口 ..............");
        return null;
    }
}
