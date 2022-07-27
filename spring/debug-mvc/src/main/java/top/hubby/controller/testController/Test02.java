package top.hubby.controller.testController;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.HttpRequestHandler;

public class Test02 implements HttpRequestHandler {
    @Override
    public void handleRequest(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        System.out.println("HttpRequestHandler.........");
    }
}
