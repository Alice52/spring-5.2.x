/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ErrorsMethodArgumentResolver;
import org.springframework.web.method.annotation.ExpressionValueMethodArgumentResolver;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.annotation.MapMethodProcessor;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.annotation.ModelFactory;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.annotation.RequestHeaderMapMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMapMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.annotation.SessionAttributesHandler;
import org.springframework.web.method.annotation.SessionStatusMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;
import org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;

/**
 * Extension of {@link AbstractHandlerMethodAdapter} that supports {@link
 * RequestMapping @RequestMapping} annotated {@link HandlerMethod HandlerMethods}.
 *
 * <p>Support for custom argument and return value types can be added via {@link
 * #setCustomArgumentResolvers} and {@link #setCustomReturnValueHandlers}, or alternatively, to
 * re-configure all argument and return value types, use {@link #setArgumentResolvers} and {@link
 * #setReturnValueHandlers}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 * @see HandlerMethodArgumentResolver
 * @see HandlerMethodReturnValueHandler
 */
public class RequestMappingHandlerAdapter extends AbstractHandlerMethodAdapter
        implements BeanFactoryAware, InitializingBean {

    /** MethodFilter that matches {@link InitBinder @InitBinder} methods. */
    public static final MethodFilter INIT_BINDER_METHODS =
            method -> AnnotatedElementUtils.hasAnnotation(method, InitBinder.class);

    /** MethodFilter that matches {@link ModelAttribute @ModelAttribute} methods. */
    public static final MethodFilter MODEL_ATTRIBUTE_METHODS =
            method ->
                    (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)
                            && AnnotatedElementUtils.hasAnnotation(method, ModelAttribute.class));

    // 用于保存实现了ResponseBodyAdvice接口，可以修改返回的ResponseBody的类
    private final List<Object> requestResponseBodyAdvice = new ArrayList<>();

    // ========== 缓存 ==========
    private final Map<Class<?>, SessionAttributesHandler> sessionAttributesHandlerCache =
            new ConcurrentHashMap<>(64);

    private final Map<Class<?>, Set<Method>> initBinderCache = new ConcurrentHashMap<>(64);

    // 用于缓存@controllerAdvice注释的类里面注释了@InitBinder的方法
    private final Map<ControllerAdviceBean, Set<Method>> initBinderAdviceCache =
            new LinkedHashMap<>();

    private final Map<Class<?>, Set<Method>> modelAttributeCache = new ConcurrentHashMap<>(64);

    // 用于缓存@ControllerADvice注释的类里注释了@ModelAttribute的方法
    private final Map<ControllerAdviceBean, Set<Method>> modelAttributeAdviceCache =
            new LinkedHashMap<>();

    @Nullable private List<HandlerMethodArgumentResolver> customArgumentResolvers;

    // 用于给处理器方法和注释了@ModelAttribute的方法设置参数
    @Nullable private HandlerMethodArgumentResolverComposite argumentResolvers;

    // 用于给注释了@initBinder的方法设置参数
    @Nullable private HandlerMethodArgumentResolverComposite initBinderArgumentResolvers;

    @Nullable private List<HandlerMethodReturnValueHandler> customReturnValueHandlers;

    // 用于将处理器的返回值处理成ModelAndView的类型
    @Nullable private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

    @Nullable private List<ModelAndViewResolver> modelAndViewResolvers;

    private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

    private List<HttpMessageConverter<?>> messageConverters;

    @Nullable private WebBindingInitializer webBindingInitializer;

    private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("MvcAsync");

    @Nullable private Long asyncRequestTimeout;

    private CallableProcessingInterceptor[] callableInterceptors =
            new CallableProcessingInterceptor[0];

    private DeferredResultProcessingInterceptor[] deferredResultInterceptors =
            new DeferredResultProcessingInterceptor[0];

    private ReactiveAdapterRegistry reactiveAdapterRegistry =
            ReactiveAdapterRegistry.getSharedInstance();

    private boolean ignoreDefaultModelOnRedirect = false;

    private int cacheSecondsForSessionAttributeHandlers = 0;

    /** 是否对相同 Session 加锁 */
    private boolean synchronizeOnSession = false;

    private SessionAttributeStore sessionAttributeStore = new DefaultSessionAttributeStore();

    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Nullable private ConfigurableBeanFactory beanFactory;

    public RequestMappingHandlerAdapter() {
        // 初始化 messageConverters
        this.messageConverters = new ArrayList<>(4);
        this.messageConverters.add(new ByteArrayHttpMessageConverter());
        this.messageConverters.add(new StringHttpMessageConverter());
        try {
            this.messageConverters.add(new SourceHttpMessageConverter<>());
        } catch (Error err) {
            // Ignore when no TransformerFactory implementation is available
        }
        this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
    }

    /** Return the custom argument resolvers, or {@code null}. */
    @Nullable
    public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
        return this.customArgumentResolvers;
    }

    /**
     * Provide resolvers for custom argument types. Custom resolvers are ordered after built-in
     * ones. To override the built-in support for argument resolution use {@link
     * #setArgumentResolvers} instead.
     */
    public void setCustomArgumentResolvers(
            @Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
        this.customArgumentResolvers = argumentResolvers;
    }

    /**
     * Return the configured argument resolvers, or possibly {@code null} if not initialized yet via
     * {@link #afterPropertiesSet()}.
     */
    @Nullable
    public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
        return (this.argumentResolvers != null ? this.argumentResolvers.getResolvers() : null);
    }

    /**
     * Configure the complete list of supported argument types thus overriding the resolvers that
     * would otherwise be configured by default.
     */
    public void setArgumentResolvers(
            @Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
        if (argumentResolvers == null) {
            this.argumentResolvers = null;
        } else {
            this.argumentResolvers = new HandlerMethodArgumentResolverComposite();
            this.argumentResolvers.addResolvers(argumentResolvers);
        }
    }

    /**
     * Return the argument resolvers for {@code @InitBinder} methods, or possibly {@code null} if
     * not initialized yet via {@link #afterPropertiesSet()}.
     */
    @Nullable
    public List<HandlerMethodArgumentResolver> getInitBinderArgumentResolvers() {
        return (this.initBinderArgumentResolvers != null
                ? this.initBinderArgumentResolvers.getResolvers()
                : null);
    }

    /** Configure the supported argument types in {@code @InitBinder} methods. */
    public void setInitBinderArgumentResolvers(
            @Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
        if (argumentResolvers == null) {
            this.initBinderArgumentResolvers = null;
        } else {
            this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite();
            this.initBinderArgumentResolvers.addResolvers(argumentResolvers);
        }
    }

    /** Return the custom return value handlers, or {@code null}. */
    @Nullable
    public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
        return this.customReturnValueHandlers;
    }

    /**
     * Provide handlers for custom return value types. Custom handlers are ordered after built-in
     * ones. To override the built-in support for return value handling use {@link
     * #setReturnValueHandlers}.
     */
    public void setCustomReturnValueHandlers(
            @Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        this.customReturnValueHandlers = returnValueHandlers;
    }

    /**
     * Return the configured handlers, or possibly {@code null} if not initialized yet via {@link
     * #afterPropertiesSet()}.
     */
    @Nullable
    public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
        return (this.returnValueHandlers != null ? this.returnValueHandlers.getHandlers() : null);
    }

    /**
     * Configure the complete list of supported return value types thus overriding handlers that
     * would otherwise be configured by default.
     */
    public void setReturnValueHandlers(
            @Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        if (returnValueHandlers == null) {
            this.returnValueHandlers = null;
        } else {
            this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();
            this.returnValueHandlers.addHandlers(returnValueHandlers);
        }
    }

    /**
     * Return the configured {@link ModelAndViewResolver ModelAndViewResolvers}, or {@code null}.
     */
    @Nullable
    public List<ModelAndViewResolver> getModelAndViewResolvers() {
        return this.modelAndViewResolvers;
    }

    /**
     * Provide custom {@link ModelAndViewResolver ModelAndViewResolvers}.
     *
     * <p><strong>Note:</strong> This method is available for backwards compatibility only. However,
     * it is recommended to re-write a {@code ModelAndViewResolver} as {@link
     * HandlerMethodReturnValueHandler}. An adapter between the two interfaces is not possible since
     * the {@link HandlerMethodReturnValueHandler#supportsReturnType} method cannot be implemented.
     * Hence {@code ModelAndViewResolver}s are limited to always being invoked at the end after all
     * other return value handlers have been given a chance.
     *
     * <p>A {@code HandlerMethodReturnValueHandler} provides better access to the return type and
     * controller method information and can be ordered freely relative to other return value
     * handlers.
     */
    public void setModelAndViewResolvers(
            @Nullable List<ModelAndViewResolver> modelAndViewResolvers) {
        this.modelAndViewResolvers = modelAndViewResolvers;
    }

    /**
     * Set the {@link ContentNegotiationManager} to use to determine requested media types. If not
     * set, the default constructor is used.
     */
    public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
        this.contentNegotiationManager = contentNegotiationManager;
    }

    /** Return the configured message body converters. */
    public List<HttpMessageConverter<?>> getMessageConverters() {
        return this.messageConverters;
    }

    /**
     * Provide the converters to use in argument resolvers and return value handlers that support
     * reading and/or writing to the body of the request and response.
     */
    public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
        this.messageConverters = messageConverters;
    }

    /**
     * Add one or more {@code RequestBodyAdvice} instances to intercept the request before it is
     * read and converted for {@code @RequestBody} and {@code HttpEntity} method arguments.
     */
    public void setRequestBodyAdvice(@Nullable List<RequestBodyAdvice> requestBodyAdvice) {
        if (requestBodyAdvice != null) {
            this.requestResponseBodyAdvice.addAll(requestBodyAdvice);
        }
    }

    /**
     * Add one or more {@code ResponseBodyAdvice} instances to intercept the response before
     * {@code @ResponseBody} or {@code ResponseEntity} return values are written to the response
     * body.
     */
    public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
        if (responseBodyAdvice != null) {
            this.requestResponseBodyAdvice.addAll(responseBodyAdvice);
        }
    }

    /** Return the configured WebBindingInitializer, or {@code null} if none. */
    @Nullable
    public WebBindingInitializer getWebBindingInitializer() {
        return this.webBindingInitializer;
    }

    /**
     * Provide a WebBindingInitializer with "global" initialization to apply to every DataBinder
     * instance.
     */
    public void setWebBindingInitializer(@Nullable WebBindingInitializer webBindingInitializer) {
        this.webBindingInitializer = webBindingInitializer;
    }

    /**
     * Set the default {@link AsyncTaskExecutor} to use when a controller method return a {@link
     * Callable}. Controller methods can override this default on a per-request basis by returning
     * an {@link WebAsyncTask}.
     *
     * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used. It's recommended to change
     * that default in production as the simple executor does not re-use threads.
     */
    public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * Specify the amount of time, in milliseconds, before concurrent handling should time out. In
     * Servlet 3, the timeout begins after the main request processing thread has exited and ends
     * when the request is dispatched again for further processing of the concurrently produced
     * result.
     *
     * <p>If this value is not set, the default timeout of the underlying implementation is used.
     *
     * @param timeout the timeout value in milliseconds
     */
    public void setAsyncRequestTimeout(long timeout) {
        this.asyncRequestTimeout = timeout;
    }

    /**
     * Configure {@code CallableProcessingInterceptor}'s to register on async requests.
     *
     * @param interceptors the interceptors to register
     */
    public void setCallableInterceptors(List<CallableProcessingInterceptor> interceptors) {
        this.callableInterceptors = interceptors.toArray(new CallableProcessingInterceptor[0]);
    }

    /**
     * Configure {@code DeferredResultProcessingInterceptor}'s to register on async requests.
     *
     * @param interceptors the interceptors to register
     */
    public void setDeferredResultInterceptors(
            List<DeferredResultProcessingInterceptor> interceptors) {
        this.deferredResultInterceptors =
                interceptors.toArray(new DeferredResultProcessingInterceptor[0]);
    }

    /**
     * Return the configured reactive type registry of adapters.
     *
     * @since 5.0
     */
    public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
        return this.reactiveAdapterRegistry;
    }

    /**
     * Configure the registry for reactive library types to be supported as return values from
     * controller methods.
     *
     * @since 5.0.5
     */
    public void setReactiveAdapterRegistry(ReactiveAdapterRegistry reactiveAdapterRegistry) {
        this.reactiveAdapterRegistry = reactiveAdapterRegistry;
    }

    /**
     * By default the content of the "default" model is used both during rendering and redirect
     * scenarios. Alternatively a controller method can declare a {@link RedirectAttributes}
     * argument and use it to provide attributes for a redirect.
     *
     * <p>Setting this flag to {@code true} guarantees the "default" model is never used in a
     * redirect scenario even if a RedirectAttributes argument is not declared. Setting it to {@code
     * false} means the "default" model may be used in a redirect if the controller method doesn't
     * declare a RedirectAttributes argument.
     *
     * <p>The default setting is {@code false} but new applications should consider setting it to
     * {@code true}.
     *
     * @see RedirectAttributes
     */
    public void setIgnoreDefaultModelOnRedirect(boolean ignoreDefaultModelOnRedirect) {
        this.ignoreDefaultModelOnRedirect = ignoreDefaultModelOnRedirect;
    }

    /**
     * Specify the strategy to store session attributes with. The default is {@link
     * org.springframework.web.bind.support.DefaultSessionAttributeStore}, storing session
     * attributes in the HttpSession with the same attribute name as in the model.
     */
    public void setSessionAttributeStore(SessionAttributeStore sessionAttributeStore) {
        this.sessionAttributeStore = sessionAttributeStore;
    }

    /**
     * Cache content produced by {@code @SessionAttributes} annotated handlers for the given number
     * of seconds.
     *
     * <p>Possible values are:
     *
     * <ul>
     *   <li>-1: no generation of cache-related headers
     *   <li>0 (default value): "Cache-Control: no-store" will prevent caching
     *   <li>1 or higher: "Cache-Control: max-age=seconds" will ask to cache content; not advised
     *       when dealing with session attributes
     * </ul>
     *
     * <p>In contrast to the "cacheSeconds" property which will apply to all general handlers (but
     * not to {@code @SessionAttributes} annotated handlers), this setting will apply to
     * {@code @SessionAttributes} handlers only.
     *
     * @see #setCacheSeconds
     * @see org.springframework.web.bind.annotation.SessionAttributes
     */
    public void setCacheSecondsForSessionAttributeHandlers(
            int cacheSecondsForSessionAttributeHandlers) {
        this.cacheSecondsForSessionAttributeHandlers = cacheSecondsForSessionAttributeHandlers;
    }

    /**
     * Set if controller execution should be synchronized on the session, to serialize parallel
     * invocations from the same client.
     *
     * <p>More specifically, the execution of the {@code handleRequestInternal} method will get
     * synchronized if this flag is "true". The best available session mutex will be used for the
     * synchronization; ideally, this will be a mutex exposed by HttpSessionMutexListener.
     *
     * <p>The session mutex is guaranteed to be the same object during the entire lifetime of the
     * session, available under the key defined by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It
     * serves as a safe reference to synchronize on for locking on the current session.
     *
     * <p>In many cases, the HttpSession reference itself is a safe mutex as well, since it will
     * always be the same object reference for the same active logical session. However, this is not
     * guaranteed across different servlet containers; the only 100% safe way is a session mutex.
     *
     * @see org.springframework.web.util.HttpSessionMutexListener
     * @see org.springframework.web.util.WebUtils#getSessionMutex(javax.servlet.http.HttpSession)
     */
    public void setSynchronizeOnSession(boolean synchronizeOnSession) {
        this.synchronizeOnSession = synchronizeOnSession;
    }

    /**
     * Set the ParameterNameDiscoverer to use for resolving method parameter names if needed (e.g.
     * for default attribute names).
     *
     * <p>Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
     */
    public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    /** Return the owning factory of this bean instance, or {@code null} if none. */
    @Nullable
    protected ConfigurableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    /**
     * A {@link ConfigurableBeanFactory} is expected for resolving expressions in method argument
     * default values.
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof ConfigurableBeanFactory) {
            this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        }
    }

    @Override
    public void afterPropertiesSet() {
        // Do this first, it may add ResponseBody advice beans
        // 初始化注释了@ControllerAdvice的类的相关属性
        initControllerAdviceCache();

        // 初始化 argumentResolvers 属性
        if (this.argumentResolvers == null) {
            List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
            this.argumentResolvers =
                    new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
        }
        // 初始化 initBinderArgumentResolvers 属性
        if (this.initBinderArgumentResolvers == null) {
            List<HandlerMethodArgumentResolver> resolvers = getDefaultInitBinderArgumentResolvers();
            this.initBinderArgumentResolvers =
                    new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
        }
        // 初始化 returnValueHandlers 属性
        if (this.returnValueHandlers == null) {
            List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
            this.returnValueHandlers =
                    new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
        }
    }

    private void initControllerAdviceCache() {
        // 判断当前应用程序上下文是否为空，如果为空，直接返回
        if (getApplicationContext() == null) {
            return;
        }

        // 扫描@ControllerAdvice注解的Bean，生成对应的ControllerAdviceBean对象，并将进行排序
        List<ControllerAdviceBean> adviceBeans =
                ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());

        List<Object> requestResponseBodyAdviceBeans = new ArrayList<>();

        // 遍历ControllerAdviceBean数组
        for (ControllerAdviceBean adviceBean : adviceBeans) {
            Class<?> beanType = adviceBean.getBeanType();
            if (beanType == null) {
                throw new IllegalStateException(
                        "Unresolvable type for ControllerAdviceBean: " + adviceBean);
            }
            // 扫描有`@ModelAttribute`，无`@RequestMapping`注解的方法，添加到`modelAttributeAdviceCache`属性中
            // 该类方法用于在执行方法前修改 Model 对象
            Set<Method> attrMethods =
                    MethodIntrospector.selectMethods(beanType, MODEL_ATTRIBUTE_METHODS);
            if (!attrMethods.isEmpty()) {
                this.modelAttributeAdviceCache.put(adviceBean, attrMethods);
            }
            // 扫描有`@InitBinder`注解的方法，添加到`initBinderAdviceCache`属性中
            // 该类方法用于在执行方法前初始化数据绑定器
            Set<Method> binderMethods =
                    MethodIntrospector.selectMethods(beanType, INIT_BINDER_METHODS);
            if (!binderMethods.isEmpty()) {
                this.initBinderAdviceCache.put(adviceBean, binderMethods);
            }
            // 如果是RequestBodyAdvice或ResponseBodyAdvice的子类，添加到requestResponseBodyAdviceBeans中
            if (RequestBodyAdvice.class.isAssignableFrom(beanType)
                    || ResponseBodyAdvice.class.isAssignableFrom(beanType)) {
                requestResponseBodyAdviceBeans.add(adviceBean);
            }
        }

        // 将requestResponseBodyAdviceBeans添加到this.requestResponseBodyAdvice属性中
        if (!requestResponseBodyAdviceBeans.isEmpty()) {
            this.requestResponseBodyAdvice.addAll(0, requestResponseBodyAdviceBeans);
        }

        // 打印日志
        if (logger.isDebugEnabled()) {
            int modelSize = this.modelAttributeAdviceCache.size();
            int binderSize = this.initBinderAdviceCache.size();
            int reqCount = getBodyAdviceCount(RequestBodyAdvice.class);
            int resCount = getBodyAdviceCount(ResponseBodyAdvice.class);
            if (modelSize == 0 && binderSize == 0 && reqCount == 0 && resCount == 0) {
                logger.debug("ControllerAdvice beans: none");
            } else {
                logger.debug(
                        "ControllerAdvice beans: "
                                + modelSize
                                + " @ModelAttribute, "
                                + binderSize
                                + " @InitBinder, "
                                + reqCount
                                + " RequestBodyAdvice, "
                                + resCount
                                + " ResponseBodyAdvice");
            }
        }
    }

    // Count all advice, including explicit registrations..

    private int getBodyAdviceCount(Class<?> adviceType) {
        List<Object> advice = this.requestResponseBodyAdvice;
        return RequestBodyAdvice.class.isAssignableFrom(adviceType)
                ? RequestResponseBodyAdviceChain.getAdviceByType(advice, RequestBodyAdvice.class)
                        .size()
                : RequestResponseBodyAdviceChain.getAdviceByType(advice, ResponseBodyAdvice.class)
                        .size();
    }

    /**
     * Return the list of argument resolvers to use including built-in resolvers and custom
     * resolvers provided via {@link #setCustomArgumentResolvers}.
     */
    private List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(30);

        // Annotation-based argument resolution
        // 添加按注解解析参数的解析器
        resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
        resolvers.add(new RequestParamMapMethodArgumentResolver());
        resolvers.add(new PathVariableMethodArgumentResolver());
        resolvers.add(new PathVariableMapMethodArgumentResolver());
        resolvers.add(new MatrixVariableMethodArgumentResolver());
        resolvers.add(new MatrixVariableMapMethodArgumentResolver());
        resolvers.add(new ServletModelAttributeMethodProcessor(false));
        resolvers.add(
                new RequestResponseBodyMethodProcessor(
                        getMessageConverters(), this.requestResponseBodyAdvice));
        resolvers.add(
                new RequestPartMethodArgumentResolver(
                        getMessageConverters(), this.requestResponseBodyAdvice));
        resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory()));
        resolvers.add(new RequestHeaderMapMethodArgumentResolver());
        resolvers.add(new ServletCookieValueMethodArgumentResolver(getBeanFactory()));
        resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
        resolvers.add(new SessionAttributeMethodArgumentResolver());
        resolvers.add(new RequestAttributeMethodArgumentResolver());

        // Type-based argument resolution
        // 添加按类型解析参数的解析器
        resolvers.add(new ServletRequestMethodArgumentResolver());
        resolvers.add(new ServletResponseMethodArgumentResolver());
        resolvers.add(
                new HttpEntityMethodProcessor(
                        getMessageConverters(), this.requestResponseBodyAdvice));
        resolvers.add(new RedirectAttributesMethodArgumentResolver());
        resolvers.add(new ModelMethodProcessor());
        resolvers.add(new MapMethodProcessor());
        resolvers.add(new ErrorsMethodArgumentResolver());
        resolvers.add(new SessionStatusMethodArgumentResolver());
        resolvers.add(new UriComponentsBuilderMethodArgumentResolver());

        // Custom arguments
        // 添加自定义参数解析器，主要用于解析自定义类型
        if (getCustomArgumentResolvers() != null) {
            resolvers.addAll(getCustomArgumentResolvers());
        }

        // Catch-all
        // 最后两个解析器可以解析所有类型的参数
        resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));
        resolvers.add(new ServletModelAttributeMethodProcessor(true));

        return resolvers;
    }

    /**
     * Return the list of argument resolvers to use for {@code @InitBinder} methods including
     * built-in and custom resolvers.
     */
    private List<HandlerMethodArgumentResolver> getDefaultInitBinderArgumentResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(20);

        // Annotation-based argument resolution
        resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));
        resolvers.add(new RequestParamMapMethodArgumentResolver());
        resolvers.add(new PathVariableMethodArgumentResolver());
        resolvers.add(new PathVariableMapMethodArgumentResolver());
        resolvers.add(new MatrixVariableMethodArgumentResolver());
        resolvers.add(new MatrixVariableMapMethodArgumentResolver());
        resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));
        resolvers.add(new SessionAttributeMethodArgumentResolver());
        resolvers.add(new RequestAttributeMethodArgumentResolver());

        // Type-based argument resolution
        resolvers.add(new ServletRequestMethodArgumentResolver());
        resolvers.add(new ServletResponseMethodArgumentResolver());

        // Custom arguments
        if (getCustomArgumentResolvers() != null) {
            resolvers.addAll(getCustomArgumentResolvers());
        }

        // Catch-all
        resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));

        return resolvers;
    }

    /**
     * Return the list of return value handlers to use including built-in and custom handlers
     * provided via {@link #setReturnValueHandlers}.
     */
    private List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
        List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>(20);

        // Single-purpose return value types
        handlers.add(new ModelAndViewMethodReturnValueHandler());
        handlers.add(new ModelMethodProcessor());
        handlers.add(new ViewMethodReturnValueHandler());
        handlers.add(
                new ResponseBodyEmitterReturnValueHandler(
                        getMessageConverters(),
                        this.reactiveAdapterRegistry,
                        this.taskExecutor,
                        this.contentNegotiationManager));
        handlers.add(new StreamingResponseBodyReturnValueHandler());
        handlers.add(
                new HttpEntityMethodProcessor(
                        getMessageConverters(),
                        this.contentNegotiationManager,
                        this.requestResponseBodyAdvice));
        handlers.add(new HttpHeadersReturnValueHandler());
        handlers.add(new CallableMethodReturnValueHandler());
        handlers.add(new DeferredResultMethodReturnValueHandler());
        handlers.add(new AsyncTaskMethodReturnValueHandler(this.beanFactory));

        // Annotation-based return value types
        handlers.add(new ModelAttributeMethodProcessor(false));
        handlers.add(
                new RequestResponseBodyMethodProcessor(
                        getMessageConverters(),
                        this.contentNegotiationManager,
                        this.requestResponseBodyAdvice));

        // Multi-purpose return value types
        handlers.add(new ViewNameMethodReturnValueHandler());
        handlers.add(new MapMethodProcessor());

        // Custom return value types
        if (getCustomReturnValueHandlers() != null) {
            handlers.addAll(getCustomReturnValueHandlers());
        }

        // Catch-all
        if (!CollectionUtils.isEmpty(getModelAndViewResolvers())) {
            handlers.add(
                    new ModelAndViewResolverMethodReturnValueHandler(getModelAndViewResolvers()));
        } else {
            handlers.add(new ModelAttributeMethodProcessor(true));
        }

        return handlers;
    }

    /**
     * Always return {@code true} since any method argument and return value type will be processed
     * in some way. A method argument not recognized by any HandlerMethodArgumentResolver is
     * interpreted as a request parameter if it is a simple type, or as a model attribute otherwise.
     * A return value not recognized by any HandlerMethodReturnValueHandler will be interpreted as a
     * model attribute.
     */
    @Override
    protected boolean supportsInternal(HandlerMethod handlerMethod) {
        return true;
    }

    @Override
    protected ModelAndView handleInternal(
            HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod)
            throws Exception {

        ModelAndView mav;
        // 校验请求（HttpMethod 和 Session 的校验）
        checkRequest(request);

        // Execute invokeHandlerMethod in synchronized block if required.
        // 如果synchronizeOnSession为true，则对session进行同步，否则不同步
        if (this.synchronizeOnSession) {
            // 同步相同 Session 的逻辑，默认情况false
            HttpSession session = request.getSession(false);
            if (session != null) {
                // 获取Session的锁对象
                Object mutex = WebUtils.getSessionMutex(session);
                synchronized (mutex) {
                    mav = invokeHandlerMethod(request, response, handlerMethod);
                }
            } else {
                // No HttpSession available -> no mutex necessary
                mav = invokeHandlerMethod(request, response, handlerMethod);
            }
        } else {
            // No synchronization on session demanded at all...
            mav = invokeHandlerMethod(request, response, handlerMethod);
        }

        // 响应不包含'Cache-Control'头
        if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
            if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
                applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
            } else {
                prepareResponse(response);
            }
        }

        return mav;
    }

    /**
     * This implementation always returns -1. An {@code @RequestMapping} method can calculate the
     * lastModified value, call {@link WebRequest#checkNotModified(long)}, and return {@code null}
     * if the result of that call is {@code true}.
     */
    @Override
    protected long getLastModifiedInternal(
            HttpServletRequest request, HandlerMethod handlerMethod) {
        return -1;
    }

    /**
     * Return the {@link SessionAttributesHandler} instance for the given handler type (never {@code
     * null}).
     */
    private SessionAttributesHandler getSessionAttributesHandler(HandlerMethod handlerMethod) {
        return this.sessionAttributesHandlerCache.computeIfAbsent(
                handlerMethod.getBeanType(),
                type -> new SessionAttributesHandler(type, this.sessionAttributeStore));
    }

    /**
     * Invoke the {@link RequestMapping} handler method preparing a {@link ModelAndView} if view
     * resolution is required.
     *
     * @since 4.2
     * @see #createInvocableHandlerMethod(HandlerMethod)
     */
    @Nullable
    protected ModelAndView invokeHandlerMethod(
            HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod)
            throws Exception {

        // 使用request和response创建ServletWebRequest对象
        ServletWebRequest webRequest = new ServletWebRequest(request, response);
        try {
            // 创建WebDataBinderFactory对象，此对象用来创建WebDataBinder对象，进行参数绑定，
            // 实现参数跟String之间的类型转换，ArgumentResolver在进行参数解析的过程中会用到WebDataBinder
            WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
            // 创建ModelFactory对象，此对象主要用来处理model，主要是两个功能，1是在处理器具体处理之前对model进行初始化，2是在处理完请求后对model参数进行更新
            ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);

            // 创建ServletInvocableHandlerMethod对象，并设置其相关属性，实际的请求处理就是通过此对象来完成的,参数绑定、处理请求以及返回值处理都在里边完成
            ServletInvocableHandlerMethod invocableMethod =
                    createInvocableHandlerMethod(handlerMethod);
            // 设置参数处理器
            if (this.argumentResolvers != null) {
                invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
            }
            // 设置返回值处理器
            if (this.returnValueHandlers != null) {
                invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
            }
            // 设置参数绑定工厂对象
            invocableMethod.setDataBinderFactory(binderFactory);
            // 设置参数名称发现器
            invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);

            // 创建ModelAndViewContainer对象，用于保存model和View对象
            ModelAndViewContainer mavContainer = new ModelAndViewContainer();
            // 将flashmap中的数据设置到model中
            mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
            // 使用modelFactory将sessionAttributes和注释了@ModelAttribute的方法的参数设置到model中
            modelFactory.initModel(webRequest, mavContainer, invocableMethod);
            // 根据配置对ignoreDefaultModelOnRedirect进行设置
            mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

            // 创建AsyncWebRequest异步请求对象
            AsyncWebRequest asyncWebRequest =
                    WebAsyncUtils.createAsyncWebRequest(request, response);
            asyncWebRequest.setTimeout(this.asyncRequestTimeout);

            // 创建WebAsyncManager异步请求管理器对象
            WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
            asyncManager.setTaskExecutor(this.taskExecutor);
            asyncManager.setAsyncWebRequest(asyncWebRequest);
            asyncManager.registerCallableInterceptors(this.callableInterceptors);
            asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);

            if (asyncManager.hasConcurrentResult()) {
                Object result = asyncManager.getConcurrentResult();
                mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];
                asyncManager.clearConcurrentResult();
                LogFormatUtils.traceDebug(
                        logger,
                        traceOn -> {
                            String formatted = LogFormatUtils.formatValue(result, !traceOn);
                            return "Resume with async result [" + formatted + "]";
                        });
                invocableMethod = invocableMethod.wrapConcurrentResult(result);
            }

            // 执行调用
            invocableMethod.invokeAndHandle(webRequest, mavContainer);
            if (asyncManager.isConcurrentHandlingStarted()) {
                return null;
            }
            // 处理完请求后的后置处理，此处一共做了三件事，
            // 1、调用ModelFactory的updateModel方法更新model，包括设置SessionAttribute和给Model设置BinderResult
            // 2、根据mavContainer创建了ModelAndView
            // 3、如果mavContainer里的model是RedirectAttributes类型，则将其设置到FlashMap
            return getModelAndView(mavContainer, modelFactory, webRequest);
        } finally {
            // 标记请求完成
            webRequest.requestCompleted();
        }
    }

    /**
     * Create a {@link ServletInvocableHandlerMethod} from the given {@link HandlerMethod}
     * definition.
     *
     * @param handlerMethod the {@link HandlerMethod} definition
     * @return the corresponding {@link ServletInvocableHandlerMethod} (or custom subclass thereof)
     * @since 4.2
     */
    protected ServletInvocableHandlerMethod createInvocableHandlerMethod(
            HandlerMethod handlerMethod) {
        return new ServletInvocableHandlerMethod(handlerMethod);
    }

    private ModelFactory getModelFactory(
            HandlerMethod handlerMethod, WebDataBinderFactory binderFactory) {
        // 获取sessionAttributesHandler
        SessionAttributesHandler sessionAttrHandler = getSessionAttributesHandler(handlerMethod);
        // 获取处理器类的类型
        Class<?> handlerType = handlerMethod.getBeanType();
        // 获取处理器类中注释了@modelAttribute而且没有注释@RequestMapping的类型，第一个获取后添加到缓存，以后直接从缓存中获取
        Set<Method> methods = this.modelAttributeCache.get(handlerType);
        if (methods == null) {
            methods = MethodIntrospector.selectMethods(handlerType, MODEL_ATTRIBUTE_METHODS);
            this.modelAttributeCache.put(handlerType, methods);
        }
        // 注释了@ModelAttribute的方法
        List<InvocableHandlerMethod> attrMethods = new ArrayList<>();
        // Global methods first
        // 先添加全局的@ModelAttribute方法，后添加当前处理器定义的@ModelAttribute方法
        this.modelAttributeAdviceCache.forEach(
                (controllerAdviceBean, methodSet) -> {
                    if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
                        Object bean = controllerAdviceBean.resolveBean();
                        for (Method method : methodSet) {
                            attrMethods.add(
                                    createModelAttributeMethod(binderFactory, bean, method));
                        }
                    }
                });
        for (Method method : methods) {
            Object bean = handlerMethod.getBean();
            attrMethods.add(createModelAttributeMethod(binderFactory, bean, method));
        }
        // 新建ModelFactory对象，此处需要三个参数，第一个是注释了@ModelAttribute的方法，第二个是WebDataBinderFactory,第三个是SessionAttributeHandler
        return new ModelFactory(attrMethods, binderFactory, sessionAttrHandler);
    }

    private InvocableHandlerMethod createModelAttributeMethod(
            WebDataBinderFactory factory, Object bean, Method method) {
        InvocableHandlerMethod attrMethod = new InvocableHandlerMethod(bean, method);
        if (this.argumentResolvers != null) {
            attrMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
        }
        attrMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
        attrMethod.setDataBinderFactory(factory);
        return attrMethod;
    }

    private WebDataBinderFactory getDataBinderFactory(HandlerMethod handlerMethod)
            throws Exception {
        Class<?> handlerType = handlerMethod.getBeanType();
        // 检查当前Handler中的initBinder方法是否已经存在于缓存中
        Set<Method> methods = this.initBinderCache.get(handlerType);
        // 如果没有则查找并设置到缓冲中
        if (methods == null) {
            methods =
                    MethodIntrospector.selectMethods(
                            handlerType,
                            INIT_BINDER_METHODS); // 将当前Controller中所有被@InitBinder注解修饰的方法都获取到
            this.initBinderCache.put(handlerType, methods);
        }
        // 定义保存InitBinder方法的临时变量
        List<InvocableHandlerMethod> initBinderMethods = new ArrayList<>();
        // Global methods first
        // 将所有符合条件的全局InitBinder方法添加到initBinderMethods
        this.initBinderAdviceCache.forEach(
                (controllerAdviceBean, methodSet) -> {
                    if (controllerAdviceBean.isApplicableToBeanType(handlerType)) {
                        Object bean = controllerAdviceBean.resolveBean();
                        for (Method method : methodSet) {
                            initBinderMethods.add(createInitBinderMethod(bean, method));
                        }
                    }
                });
        // 将当前handler中的initBinder方法添加到initBinderMethods
        for (Method method : methods) {
            // 创建当前方法对应的bean对象
            Object bean = handlerMethod.getBean();
            // 将method适配为可执行的invocableHandlerMethod
            initBinderMethods.add(createInitBinderMethod(bean, method));
        }
        // 创建DataBinderFactory并返回
        return createDataBinderFactory(initBinderMethods);
    }

    private InvocableHandlerMethod createInitBinderMethod(Object bean, Method method) {
        InvocableHandlerMethod binderMethod = new InvocableHandlerMethod(bean, method);
        if (this.initBinderArgumentResolvers != null) {
            binderMethod.setHandlerMethodArgumentResolvers(this.initBinderArgumentResolvers);
        }
        binderMethod.setDataBinderFactory(new DefaultDataBinderFactory(this.webBindingInitializer));
        binderMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);
        return binderMethod;
    }

    /**
     * Template method to create a new InitBinderDataBinderFactory instance.
     *
     * <p>The default implementation creates a ServletRequestDataBinderFactory. This can be
     * overridden for custom ServletRequestDataBinder subclasses.
     *
     * @param binderMethods {@code @InitBinder} methods
     * @return the InitBinderDataBinderFactory instance to use
     * @throws Exception in case of invalid state or arguments
     */
    protected InitBinderDataBinderFactory createDataBinderFactory(
            List<InvocableHandlerMethod> binderMethods) throws Exception {

        return new ServletRequestDataBinderFactory(binderMethods, getWebBindingInitializer());
    }

    @Nullable
    private ModelAndView getModelAndView(
            ModelAndViewContainer mavContainer,
            ModelFactory modelFactory,
            NativeWebRequest webRequest)
            throws Exception {

        // 更新model(设置sessionAttributes和给model设置BindingResult)
        modelFactory.updateModel(webRequest, mavContainer);
        // 情况一，如果 mavContainer 已处理，则返回“空”的 ModelAndView 对象。
        if (mavContainer.isRequestHandled()) {
            return null;
        }
        // 情况二，如果mavContainer未处理，则基于`mavContainer`生成ModelAndView对象
        ModelMap model = mavContainer.getModel();
        // 创建 ModelAndView 对象，并设置相关属性
        ModelAndView mav =
                new ModelAndView(mavContainer.getViewName(), model, mavContainer.getStatus());
        // 如果mavContainer里的view不是引用，也就是不是string类型，则设置到mv中
        if (!mavContainer.isViewReference()) {
            mav.setView((View) mavContainer.getView());
        }
        if (model instanceof RedirectAttributes) {
            Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request != null) {
                RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
            }
        }
        return mav;
    }
}
