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

package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * AbstractHandlerMapping是HandlerMapping接口的抽象实现，所有的handlerMapping都要继承此抽象类
 * abstractHandlerMapping采用模板模式设计了HandlerMapping实现子类的整体结构，子类只需要通过模板方法提供一些初始值或者业务逻辑即可
 *
 * <p>handlerMapping是根据request找到Handler和interceptors,获取Handler的过程通过模板方法getHandlerInternal交给了子类，
 * abstractHandlerMapping保存了所用配置的interceptor，在获取到handler之后，根据从request中提取的lookupPath将相应的interceptors装配进去
 * 当然子类也可以通过getHandlerInternal方法设置自己的interceptor。
 *
 * <p>Abstract base class for {@link org.springframework.web.servlet.HandlerMapping}
 * implementations. Supports ordering, a default handler, handler interceptors, including handler
 * interceptors mapped by path patterns.
 *
 * <p>Note: This base class does <i>not</i> support exposure of the {@link
 * #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}. Support for this attribute is up to concrete subclasses,
 * typically based on request URL mappings.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 07.04.2003
 * @see #getHandlerInternal
 * @see #setDefaultHandler
 * @see #setAlwaysUseFullPath
 * @see #setUrlDecode
 * @see org.springframework.util.AntPathMatcher
 * @see #setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
        implements HandlerMapping, Ordered, BeanNameAware {

    /**
     * 用于配置springmvc的拦截器，有两种设置方式， 1、注册handlerMapping时通过属性设置 2、通过子类的extendInterceptors钩子方法进行设置
     *
     * <p>此集合并不会直接使用，而是通过initInterceptors方法按照类型分配到mappedInterceptors和adaptedInterceptors中进行使用
     */
    private final List<Object> interceptors = new ArrayList<>();

    // 初始化后的拦截器handlerInterceptor数组
    private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

    // 默认处理器，使用的对象类型是Object，子类实现的时候使用HandlerMethod,HandlerExecutionChain等
    @Nullable private Object defaultHandler;

    // url路径管理工具
    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    // 基于ant进行path匹配，解决/user/{id}的场景
    private PathMatcher pathMatcher = new AntPathMatcher();

    @Nullable private CorsConfigurationSource corsConfigurationSource;

    private CorsProcessor corsProcessor = new DefaultCorsProcessor();

    // 表示优先级的变量，值越大，优先级越低
    private int order = Ordered.LOWEST_PRECEDENCE; // default: same as non-Ordered

    // 当前bean的名称
    @Nullable private String beanName;

    /** Return the default handler for this handler mapping, or {@code null} if none. */
    @Nullable
    public Object getDefaultHandler() {
        return this.defaultHandler;
    }

    /**
     * Set the default handler for this handler mapping. This handler will be returned if no
     * specific mapping was found.
     *
     * <p>Default is {@code null}, indicating no default handler.
     */
    public void setDefaultHandler(@Nullable Object defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    /**
     * Shortcut to same property on underlying {@link #setUrlPathHelper UrlPathHelper}.
     *
     * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath(boolean)
     */
    public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
        this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource)
                    .setAlwaysUseFullPath(alwaysUseFullPath);
        }
    }

    /**
     * Shortcut to same property on underlying {@link #setUrlPathHelper UrlPathHelper}.
     *
     * @see org.springframework.web.util.UrlPathHelper#setUrlDecode(boolean)
     */
    public void setUrlDecode(boolean urlDecode) {
        this.urlPathHelper.setUrlDecode(urlDecode);
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource)
                    .setUrlDecode(urlDecode);
        }
    }

    /**
     * Shortcut to same property on underlying {@link #setUrlPathHelper UrlPathHelper}.
     *
     * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
     */
    public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
        this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource)
                    .setRemoveSemicolonContent(removeSemicolonContent);
        }
    }

    /** Return the UrlPathHelper implementation to use for resolution of lookup paths. */
    public UrlPathHelper getUrlPathHelper() {
        return this.urlPathHelper;
    }

    /**
     * Set the UrlPathHelper to use for resolution of lookup paths.
     *
     * <p>Use this to override the default UrlPathHelper with a custom subclass, or to share common
     * UrlPathHelper settings across multiple HandlerMappings and MethodNameResolvers.
     */
    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
        this.urlPathHelper = urlPathHelper;
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource)
                    .setUrlPathHelper(urlPathHelper);
        }
    }

    /**
     * Return the PathMatcher implementation to use for matching URL paths against registered URL
     * patterns.
     */
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    /**
     * Set the PathMatcher implementation to use for matching URL paths against registered URL
     * patterns. Default is AntPathMatcher.
     *
     * @see org.springframework.util.AntPathMatcher
     */
    public void setPathMatcher(PathMatcher pathMatcher) {
        Assert.notNull(pathMatcher, "PathMatcher must not be null");
        this.pathMatcher = pathMatcher;
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            ((UrlBasedCorsConfigurationSource) this.corsConfigurationSource)
                    .setPathMatcher(pathMatcher);
        }
    }

    /**
     * Set the interceptors to apply for all handlers mapped by this handler mapping.
     *
     * <p>Supported interceptor types are HandlerInterceptor, WebRequestInterceptor, and
     * MappedInterceptor. Mapped interceptors apply only to request URLs that match its path
     * patterns. Mapped interceptor beans are also detected by type during initialization.
     *
     * @param interceptors array of handler interceptors
     * @see #adaptInterceptor
     * @see org.springframework.web.servlet.HandlerInterceptor
     * @see org.springframework.web.context.request.WebRequestInterceptor
     */
    public void setInterceptors(Object... interceptors) {
        this.interceptors.addAll(Arrays.asList(interceptors));
    }

    /**
     * Set the "global" CORS configurations based on URL patterns. By default the first matching URL
     * pattern is combined with the CORS configuration for the handler, if any.
     *
     * @since 4.2
     * @see #setCorsConfigurationSource(CorsConfigurationSource)
     */
    public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
        Assert.notNull(corsConfigurations, "corsConfigurations must not be null");
        if (!corsConfigurations.isEmpty()) {
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.setCorsConfigurations(corsConfigurations);
            source.setPathMatcher(this.pathMatcher);
            source.setUrlPathHelper(this.urlPathHelper);
            source.setLookupPathAttributeName(LOOKUP_PATH);
            this.corsConfigurationSource = source;
        } else {
            this.corsConfigurationSource = null;
        }
    }

    /**
     * Set the "global" CORS configuration source. By default the first matching URL pattern is
     * combined with the CORS configuration for the handler, if any.
     *
     * @since 5.1
     * @see #setCorsConfigurations(Map)
     */
    public void setCorsConfigurationSource(CorsConfigurationSource corsConfigurationSource) {
        Assert.notNull(corsConfigurationSource, "corsConfigurationSource must not be null");
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /** Return the configured {@link CorsProcessor}. */
    public CorsProcessor getCorsProcessor() {
        return this.corsProcessor;
    }

    /**
     * Configure a custom {@link CorsProcessor} to use to apply the matched {@link
     * CorsConfiguration} for a request.
     *
     * <p>By default {@link DefaultCorsProcessor} is used.
     *
     * @since 4.2
     */
    public void setCorsProcessor(CorsProcessor corsProcessor) {
        Assert.notNull(corsProcessor, "CorsProcessor must not be null");
        this.corsProcessor = corsProcessor;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    /**
     * Specify the order value for this HandlerMapping bean.
     *
     * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
     *
     * @see org.springframework.core.Ordered#getOrder()
     */
    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    protected String formatMappingName() {
        return this.beanName != null ? "'" + this.beanName + "'" : "<unknown>";
    }

    /**
     * Initializes the interceptors.
     *
     * @see #extendInterceptors(java.util.List)
     * @see #initInterceptors()
     */
    @Override
    protected void initApplicationContext() throws BeansException {
        // 空实现，交给子类实现，用于注册自定义的拦截器到interceptors中，目前暂无子类实现
        extendInterceptors(this.interceptors);
        // 扫描已注册的MappedInterceptor的Bean们，添加到adaptedInterceptors中
        detectMappedInterceptors(this.adaptedInterceptors);
        // 将interceptors初始化成 HandlerInterceptor类型，添加到adaptedInterceptors中
        initInterceptors();
    }

    /**
     * 提供给子类扩展拦截器
     *
     * <p>Extension hook that subclasses can override to register additional interceptors, given the
     * configured interceptors (see {@link #setInterceptors}).
     *
     * <p>Will be invoked before {@link #initInterceptors()} adapts the specified interceptors into
     * {@link HandlerInterceptor} instances.
     *
     * <p>The default implementation is empty.
     *
     * @param interceptors the configured interceptor List (never {@code null}), allowing to add
     *     further interceptors before as well as after the existing interceptors
     */
    protected void extendInterceptors(List<Object> interceptors) {}

    /**
     * 扫描应用下的mappeedInterceptor，并添加到mappedInterceptors Detect beans of type {@link
     * MappedInterceptor} and add them to the list of mapped interceptors.
     *
     * <p>This is called in addition to any {@link MappedInterceptor}s that may have been provided
     * via {@link #setInterceptors}, by default adding all beans of type {@link MappedInterceptor}
     * from the current context and its ancestors. Subclasses can override and refine this policy.
     *
     * @param mappedInterceptors an empty list to add to
     */
    protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
        // 扫描已注册的MappedInterceptor的Bean，添加到mappedInterceptors中
        // MappedInterceptor会根据请求路径做匹配，是否进行拦截
        mappedInterceptors.addAll(
                BeanFactoryUtils.beansOfTypeIncludingAncestors(
                                obtainApplicationContext(), MappedInterceptor.class, true, false)
                        .values());
    }

    /**
     * 初始化interceptors,并适配HandlerInterceptors和WebRequestInterceptor
     *
     * <p>Initialize the specified interceptors adapting {@link WebRequestInterceptor}s to {@link
     * HandlerInterceptor}.
     *
     * @see #setInterceptors
     * @see #adaptInterceptor
     */
    protected void initInterceptors() {
        if (!this.interceptors.isEmpty()) {
            for (int i = 0; i < this.interceptors.size(); i++) {
                Object interceptor = this.interceptors.get(i);
                if (interceptor == null) {
                    throw new IllegalArgumentException(
                            "Entry number " + i + " in interceptors array is null");
                }
                // 将interceptors初始化成HandlerInterceptor类型，添加到adaptedInterceptors中
                // 注意，HandlerInterceptor无需进行路径匹配，直接拦截全部
                this.adaptedInterceptors.add(adaptInterceptor(interceptor));
            }
        }
    }

    /**
     * Adapt the given interceptor object to {@link HandlerInterceptor}.
     *
     * <p>By default, the supported interceptor types are {@link HandlerInterceptor} and {@link
     * WebRequestInterceptor}. Each given {@link WebRequestInterceptor} is wrapped with {@link
     * WebRequestHandlerInterceptorAdapter}.
     *
     * @param interceptor the interceptor
     * @return the interceptor downcast or adapted to HandlerInterceptor
     * @see org.springframework.web.servlet.HandlerInterceptor
     * @see org.springframework.web.context.request.WebRequestInterceptor
     * @see WebRequestHandlerInterceptorAdapter
     */
    protected HandlerInterceptor adaptInterceptor(Object interceptor) {
        if (interceptor instanceof HandlerInterceptor) {
            return (HandlerInterceptor) interceptor;
        } else if (interceptor instanceof WebRequestInterceptor) {
            return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
        } else {
            throw new IllegalArgumentException(
                    "Interceptor type not supported: " + interceptor.getClass().getName());
        }
    }

    /**
     * Return the adapted interceptors as {@link HandlerInterceptor} array.
     *
     * @return the array of {@link HandlerInterceptor HandlerInterceptor}s, or {@code null} if none
     */
    @Nullable
    protected final HandlerInterceptor[] getAdaptedInterceptors() {
        return (!this.adaptedInterceptors.isEmpty()
                ? this.adaptedInterceptors.toArray(new HandlerInterceptor[0])
                : null);
    }

    /**
     * Return all configured {@link MappedInterceptor}s as an array.
     *
     * @return the array of {@link MappedInterceptor}s, or {@code null} if none
     */
    @Nullable
    protected final MappedInterceptor[] getMappedInterceptors() {
        List<MappedInterceptor> mappedInterceptors =
                new ArrayList<>(this.adaptedInterceptors.size());
        for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
            if (interceptor instanceof MappedInterceptor) {
                mappedInterceptors.add((MappedInterceptor) interceptor);
            }
        }
        return (!mappedInterceptors.isEmpty()
                ? mappedInterceptors.toArray(new MappedInterceptor[0])
                : null);
    }

    /**
     * Look up a handler for the given request, falling back to the default handler if no specific
     * one is found.
     *
     * @param request current HTTP request
     * @return the corresponding handler instance, or the default handler
     * @see #getHandlerInternal
     */
    @Override
    @Nullable
    public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        // 获得处理器（HandlerMethod或者HandlerExecutionChain），该方法是抽象方法，由子类实现
        Object handler = getHandlerInternal(request);
        // 获得不到，则使用默认处理器
        if (handler == null) {
            handler = getDefaultHandler();
        }
        // 还是获得不到，则返回 null
        if (handler == null) {
            return null;
        }
        // Bean name or resolved handler?
        // 如果找到的处理器是String类型，则从Spring容器中找到对应的Bean作为处理器
        if (handler instanceof String) {
            String handlerName = (String) handler;
            handler = obtainApplicationContext().getBean(handlerName);
        }

        // 创建HandlerExecutionChain对象（包含处理器和拦截器）
        HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

        if (logger.isTraceEnabled()) {
            logger.trace("Mapped to " + handler);
        } else if (logger.isDebugEnabled()
                && !request.getDispatcherType().equals(DispatcherType.ASYNC)) {
            logger.debug("Mapped to " + executionChain.getHandler());
        }

        // 针对跨域请求的处理
        if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
            CorsConfiguration config =
                    (this.corsConfigurationSource != null
                            ? this.corsConfigurationSource.getCorsConfiguration(request)
                            : null);
            CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
            config = (config != null ? config.combine(handlerConfig) : handlerConfig);
            executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
        }

        return executionChain;
    }

    /**
     * Look up a handler for the given request, returning {@code null} if no specific one is found.
     * This method is called by {@link #getHandler}; a {@code null} return value will lead to the
     * default handler, if one is set.
     *
     * <p>On CORS pre-flight requests this method should return a match not for the pre-flight
     * request but for the expected actual request based on the URL path, the HTTP methods from the
     * "Access-Control-Request-Method" header, and the headers from the
     * "Access-Control-Request-Headers" header thus allowing the CORS configuration to be obtained
     * via {@link #getCorsConfiguration(Object, HttpServletRequest)},
     *
     * <p>Note: This method may also return a pre-built {@link HandlerExecutionChain}, combining a
     * handler object with dynamically determined interceptors. Statically specified interceptors
     * will get merged into such an existing chain.
     *
     * @param request current HTTP request
     * @return the corresponding handler instance, or {@code null} if none found
     * @throws Exception if there is an internal error
     */
    @Nullable
    protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

    /**
     * Build a {@link HandlerExecutionChain} for the given handler, including applicable
     * interceptors.
     *
     * <p>The default implementation builds a standard {@link HandlerExecutionChain} with the given
     * handler, the common interceptors of the handler mapping, and any {@link MappedInterceptor
     * MappedInterceptors} matching to the current request URL. Interceptors are added in the order
     * they were registered. Subclasses may override this in order to extend/rearrange the list of
     * interceptors.
     *
     * <p><b>NOTE:</b> The passed-in handler object may be a raw handler or a pre-built {@link
     * HandlerExecutionChain}. This method should handle those two cases explicitly, either building
     * a new {@link HandlerExecutionChain} or extending the existing chain.
     *
     * <p>For simply adding an interceptor in a custom subclass, consider calling {@code
     * super.getHandlerExecutionChain(handler, request)} and invoking {@link
     * HandlerExecutionChain#addInterceptor} on the returned chain object.
     *
     * @param handler the resolved handler instance (never {@code null})
     * @param request current HTTP request
     * @return the HandlerExecutionChain (never {@code null})
     * @see #getAdaptedInterceptors()
     */
    protected HandlerExecutionChain getHandlerExecutionChain(
            Object handler, HttpServletRequest request) {
        // 创建 HandlerExecutionChain 对象
        HandlerExecutionChain chain =
                (handler instanceof HandlerExecutionChain
                        ? (HandlerExecutionChain) handler
                        : new HandlerExecutionChain(handler));

        // 获得请求路径
        String lookupPath = this.urlPathHelper.getLookupPathForRequest(request, LOOKUP_PATH);
        // 遍历 adaptedInterceptors 数组，获得请求匹配的拦截器
        for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
            // 需要匹配，若路径匹配，则添加到 chain 中
            if (interceptor instanceof MappedInterceptor) {
                MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
                if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
                    chain.addInterceptor(mappedInterceptor.getInterceptor());
                }
            }
            // 无需匹配，直接添加到 chain 中
            else {
                chain.addInterceptor(interceptor);
            }
        }
        return chain;
    }

    /**
     * Return {@code true} if there is a {@link CorsConfigurationSource} for this handler.
     *
     * @since 5.2
     */
    protected boolean hasCorsConfigurationSource(Object handler) {
        if (handler instanceof HandlerExecutionChain) {
            handler = ((HandlerExecutionChain) handler).getHandler();
        }
        return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
    }

    /**
     * Retrieve the CORS configuration for the given handler.
     *
     * @param handler the handler to check (never {@code null}).
     * @param request the current request.
     * @return the CORS configuration for the handler, or {@code null} if none
     * @since 4.2
     */
    @Nullable
    protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
        Object resolvedHandler = handler;
        if (handler instanceof HandlerExecutionChain) {
            resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
        }
        if (resolvedHandler instanceof CorsConfigurationSource) {
            return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
        }
        return null;
    }

    /**
     * Update the HandlerExecutionChain for CORS-related handling.
     *
     * <p>For pre-flight requests, the default implementation replaces the selected handler with a
     * simple HttpRequestHandler that invokes the configured {@link #setCorsProcessor}.
     *
     * <p>For actual requests, the default implementation inserts a HandlerInterceptor that makes
     * CORS-related checks and adds CORS headers.
     *
     * @param request the current request
     * @param chain the handler chain
     * @param config the applicable CORS configuration (possibly {@code null})
     * @since 4.2
     */
    protected HandlerExecutionChain getCorsHandlerExecutionChain(
            HttpServletRequest request,
            HandlerExecutionChain chain,
            @Nullable CorsConfiguration config) {

        if (CorsUtils.isPreFlightRequest(request)) {
            HandlerInterceptor[] interceptors = chain.getInterceptors();
            return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
        } else {
            chain.addInterceptor(0, new CorsInterceptor(config));
            return chain;
        }
    }

    private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

        @Nullable private final CorsConfiguration config;

        public PreFlightHandler(@Nullable CorsConfiguration config) {
            this.config = config;
        }

        @Override
        public void handleRequest(HttpServletRequest request, HttpServletResponse response)
                throws IOException {
            corsProcessor.processRequest(this.config, request, response);
        }

        @Override
        @Nullable
        public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
            return this.config;
        }
    }

    private class CorsInterceptor extends HandlerInterceptorAdapter
            implements CorsConfigurationSource {

        @Nullable private final CorsConfiguration config;

        public CorsInterceptor(@Nullable CorsConfiguration config) {
            this.config = config;
        }

        @Override
        public boolean preHandle(
                HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {

            // Consistent with CorsFilter, ignore ASYNC dispatches
            WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
            if (asyncManager.hasConcurrentResult()) {
                return true;
            }

            return corsProcessor.processRequest(this.config, request, response);
        }

        @Override
        @Nullable
        public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
            return this.config;
        }
    }
}
