package top.hubby.adapter;

public interface HandlerAdapter {
    public boolean supports(Object handler);

    public void handle(Object handler);
}
