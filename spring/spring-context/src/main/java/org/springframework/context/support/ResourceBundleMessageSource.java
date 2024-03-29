/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 该实现类允许用户通过beanName指定一个资源名（包括类路径的完全限定资源名）或者通过beanNames指定一组资源名
 *
 * <p>{@link org.springframework.context.MessageSource} implementation that accesses resource
 * bundles using specified basenames. This class relies on the underlying JDK's {@link
 * java.util.ResourceBundle} implementation, in combination with the JDK's standard message parsing
 * provided by {@link java.text.MessageFormat}.
 *
 * <p>This MessageSource caches both the accessed ResourceBundle instances and the generated
 * MessageFormats for each message. It also implements rendering of no-arg messages without
 * MessageFormat, as supported by the AbstractMessageSource base class. The caching provided by this
 * MessageSource is significantly faster than the built-in caching of the {@code
 * java.util.ResourceBundle} class.
 *
 * <p>The basenames follow {@link java.util.ResourceBundle} conventions: essentially, a
 * fully-qualified classpath location. If it doesn't contain a package qualifier (such as {@code
 * org.mypackage}), it will be resolved from the classpath root. Note that the JDK's standard
 * ResourceBundle treats dots as package separators: This means that "test.theme" is effectively
 * equivalent to "test/theme".
 *
 * <p>On the classpath, bundle resources will be read with the locally configured {@link
 * #setDefaultEncoding encoding}: by default, ISO-8859-1; consider switching this to UTF-8, or to
 * {@code null} for the platform default encoding. On the JDK 9+ module path where locally provided
 * {@code ResourceBundle.Control} handles are not supported, this MessageSource always falls back to
 * {@link ResourceBundle#getBundle} retrieval with the platform default encoding: UTF-8 with a
 * ISO-8859-1 fallback on JDK 9+ (configurable through the
 * "java.util.PropertyResourceBundle.encoding" system property). Note that {@link
 * #loadBundle(Reader)}/{@link #loadBundle(InputStream)} won't be called in this case either,
 * effectively ignoring overrides in subclasses. Consider implementing a JDK 9 {@code
 * java.util.spi.ResourceBundleProvider} instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setBasenames
 * @see ReloadableResourceBundleMessageSource
 * @see java.util.ResourceBundle
 * @see java.text.MessageFormat
 */
public class ResourceBundleMessageSource extends AbstractResourceBasedMessageSource
        implements BeanClassLoaderAware {

    /**
     * 缓存code和区域所对应资源包ResourceBundles
     *
     * <p>Cache to hold loaded ResourceBundles. This Map is keyed with the bundle basename, which
     * holds a Map that is keyed with the Locale and in turn holds the ResourceBundle instances.
     * This allows for very efficient hash lookups, significantly faster than the ResourceBundle
     * class's own cache.
     */
    private final Map<String, Map<Locale, ResourceBundle>> cachedResourceBundles =
            new ConcurrentHashMap<>();

    /**
     * 缓存ResourceBundle和code及locale所对应的messageFormat
     *
     * <p>Cache to hold already generated MessageFormats. This Map is keyed with the ResourceBundle,
     * which holds a Map that is keyed with the message code, which in turn holds a Map that is
     * keyed with the Locale and holds the MessageFormat values. This allows for very efficient hash
     * lookups without concatenated keys.
     *
     * @see #getMessageFormat
     */
    private final Map<ResourceBundle, Map<String, Map<Locale, MessageFormat>>>
            cachedBundleMessageFormats = new ConcurrentHashMap<>();

    // 加载绑定资源文件的类加载器
    @Nullable private ClassLoader bundleClassLoader;

    @Nullable private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    @Nullable private volatile MessageSourceControl control = new MessageSourceControl();

    public ResourceBundleMessageSource() {
        setDefaultEncoding("ISO-8859-1");
    }

    /**
     * Return the ClassLoader to load resource bundles with.
     *
     * <p>Default is the containing BeanFactory's bean ClassLoader.
     *
     * @see #setBundleClassLoader
     */
    @Nullable
    protected ClassLoader getBundleClassLoader() {
        return (this.bundleClassLoader != null ? this.bundleClassLoader : this.beanClassLoader);
    }

    /**
     * Set the ClassLoader to load resource bundles with.
     *
     * <p>Default is the containing BeanFactory's {@link
     * org.springframework.beans.factory.BeanClassLoaderAware bean ClassLoader}, or the default
     * ClassLoader determined by {@link org.springframework.util.ClassUtils#getDefaultClassLoader()}
     * if not running within a BeanFactory.
     */
    public void setBundleClassLoader(ClassLoader classLoader) {
        this.bundleClassLoader = classLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    /**
     * 将给定的消息代码解析为已注册资源包中的key，按原样返回捆绑包中的值（不适用messageFormat解析）
     *
     * <p>Resolves the given message code as key in the registered resource bundles, returning the
     * value found in the bundle as-is (without MessageFormat parsing).
     */
    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        Set<String> basenames = getBasenameSet();
        for (String basename : basenames) {
            ResourceBundle bundle = getResourceBundle(basename, locale);
            if (bundle != null) {
                String result = getStringOrNull(bundle, code);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 将给定的消息代码解析为注册资源包中的key，每个消息代码使用缓存的messageFormat实例
     *
     * <p>Resolves the given message code as key in the registered resource bundles, using a cached
     * MessageFormat instance per message code.
     */
    @Override
    @Nullable
    protected MessageFormat resolveCode(String code, Locale locale) {
        Set<String> basenames = getBasenameSet();
        for (String basename : basenames) {
            ResourceBundle bundle = getResourceBundle(basename, locale);
            if (bundle != null) {
                MessageFormat messageFormat = getMessageFormat(bundle, code, locale);
                if (messageFormat != null) {
                    return messageFormat;
                }
            }
        }
        return null;
    }

    /**
     * 将给定的beanname和代码返回一个ResourceBundle，从缓存中提取已生成的MessageFormats
     *
     * <p>Return a ResourceBundle for the given basename and code, fetching already generated
     * MessageFormats from the cache.
     *
     * @param basename the basename of the ResourceBundle
     * @param locale the Locale to find the ResourceBundle for
     * @return the resulting ResourceBundle, or {@code null} if none found for the given basename
     *     and Locale
     */
    @Nullable
    protected ResourceBundle getResourceBundle(String basename, Locale locale) {
        if (getCacheMillis() >= 0) {
            // Fresh ResourceBundle.getBundle call in order to let ResourceBundle
            // do its native caching, at the expense of more extensive lookup steps.
            // 新建ResourceBundle.getBundle调用，以使ResourceBundle执行其本地缓存，而不必花费更广泛的查找步骤。
            return doGetBundle(basename, locale);
        } else {
            // Cache forever: prefer locale cache over repeated getBundle calls.
            // 永久缓存：在重复的getBundle调用中优先使用语言环境缓存。
            // 先获取缓存
            Map<Locale, ResourceBundle> localeMap = this.cachedResourceBundles.get(basename);
            if (localeMap != null) {
                ResourceBundle bundle = localeMap.get(locale);
                if (bundle != null) {
                    return bundle;
                }
            }
            // 没有找到缓存
            try {
                ResourceBundle bundle = doGetBundle(basename, locale);
                if (localeMap == null) {
                    localeMap = new ConcurrentHashMap<>();
                    Map<Locale, ResourceBundle> existing =
                            this.cachedResourceBundles.putIfAbsent(basename, localeMap);
                    if (existing != null) {
                        localeMap = existing;
                    }
                }
                // 放入缓存
                localeMap.put(locale, bundle);
                return bundle;
            } catch (MissingResourceException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn(
                            "ResourceBundle ["
                                    + basename
                                    + "] not found for MessageSource: "
                                    + ex.getMessage());
                }
                // Assume bundle not found
                // -> do NOT throw the exception to allow for checking parent message source.
                return null;
            }
        }
    }

    /**
     * 获取给定的basename和locale设置的资源包
     *
     * <p>Obtain the resource bundle for the given basename and Locale.
     *
     * @param basename the basename to look for
     * @param locale the Locale to look for
     * @return the corresponding ResourceBundle
     * @throws MissingResourceException if no matching bundle could be found
     * @see java.util.ResourceBundle#getBundle(String, Locale, ClassLoader)
     * @see #getBundleClassLoader()
     */
    protected ResourceBundle doGetBundle(String basename, Locale locale)
            throws MissingResourceException {
        ClassLoader classLoader = getBundleClassLoader();
        Assert.state(classLoader != null, "No bundle ClassLoader set");

        MessageSourceControl control = this.control;
        if (control != null) {
            try {
                return ResourceBundle.getBundle(basename, locale, classLoader, control);
            } catch (UnsupportedOperationException ex) {
                // Probably in a Jigsaw environment on JDK 9+
                this.control = null;
                String encoding = getDefaultEncoding();
                if (encoding != null && logger.isInfoEnabled()) {
                    logger.info(
                            "ResourceBundleMessageSource is configured to read resources with encoding '"
                                    + encoding
                                    + "' but ResourceBundle.Control not supported in current system environment: "
                                    + ex.getMessage()
                                    + " - falling back to plain ResourceBundle.getBundle retrieval with the "
                                    + "platform default encoding. Consider setting the 'defaultEncoding' property to 'null' "
                                    + "for participating in the platform default and therefore avoiding this log message.");
                }
            }
        }

        // Fallback: plain getBundle lookup without Control handle
        return ResourceBundle.getBundle(basename, locale, classLoader);
    }

    /**
     * Load a property-based resource bundle from the given reader.
     *
     * <p>This will be called in case of a {@link #setDefaultEncoding "defaultEncoding"}, including
     * {@link ResourceBundleMessageSource}'s default ISO-8859-1 encoding. Note that this method can
     * only be called with a {@code ResourceBundle.Control}: When running on the JDK 9+ module path
     * where such control handles are not supported, any overrides in custom subclasses will
     * effectively get ignored.
     *
     * <p>The default implementation returns a {@link PropertyResourceBundle}.
     *
     * @param reader the reader for the target resource
     * @return the fully loaded bundle
     * @throws IOException in case of I/O failure
     * @since 4.2
     * @see #loadBundle(InputStream)
     * @see PropertyResourceBundle#PropertyResourceBundle(Reader)
     */
    protected ResourceBundle loadBundle(Reader reader) throws IOException {
        return new PropertyResourceBundle(reader);
    }

    /**
     * Load a property-based resource bundle from the given input stream, picking up the default
     * properties encoding on JDK 9+.
     *
     * <p>This will only be called with {@link #setDefaultEncoding "defaultEncoding"} set to {@code
     * null}, explicitly enforcing the platform default encoding (which is UTF-8 with a ISO-8859-1
     * fallback on JDK 9+ but configurable through the "java.util.PropertyResourceBundle.encoding"
     * system property). Note that this method can only be called with a {@code
     * ResourceBundle.Control}: When running on the JDK 9+ module path where such control handles
     * are not supported, any overrides in custom subclasses will effectively get ignored.
     *
     * <p>The default implementation returns a {@link PropertyResourceBundle}.
     *
     * @param inputStream the input stream for the target resource
     * @return the fully loaded bundle
     * @throws IOException in case of I/O failure
     * @since 5.1
     * @see #loadBundle(Reader)
     * @see PropertyResourceBundle#PropertyResourceBundle(InputStream)
     */
    protected ResourceBundle loadBundle(InputStream inputStream) throws IOException {
        return new PropertyResourceBundle(inputStream);
    }

    /**
     * 为给定的包和代码返回一个MessageFormat,从缓存中提取已生成的MessageFormats
     *
     * <p>Return a MessageFormat for the given bundle and code, fetching already generated
     * MessageFormats from the cache.
     *
     * @param bundle the ResourceBundle to work on
     * @param code the message code to retrieve
     * @param locale the Locale to use to build the MessageFormat
     * @return the resulting MessageFormat, or {@code null} if no message defined for the given code
     * @throws MissingResourceException if thrown by the ResourceBundle
     */
    @Nullable
    protected MessageFormat getMessageFormat(ResourceBundle bundle, String code, Locale locale)
            throws MissingResourceException {
        // 先获取缓存
        Map<String, Map<Locale, MessageFormat>> codeMap =
                this.cachedBundleMessageFormats.get(bundle);
        Map<Locale, MessageFormat> localeMap = null;
        if (codeMap != null) {
            localeMap = codeMap.get(code);
            if (localeMap != null) {
                MessageFormat result = localeMap.get(locale);
                if (result != null) {
                    return result;
                }
            }
        }

        // 缓存为空的话，执行如下步骤，获取资源包中指定key所对应的值
        String msg = getStringOrNull(bundle, code);
        if (msg != null) {
            // 放入缓存
            if (codeMap == null) {
                codeMap = new ConcurrentHashMap<>();
                Map<String, Map<Locale, MessageFormat>> existing =
                        this.cachedBundleMessageFormats.putIfAbsent(bundle, codeMap);
                if (existing != null) {
                    codeMap = existing;
                }
            }
            if (localeMap == null) {
                localeMap = new ConcurrentHashMap<>();
                Map<Locale, MessageFormat> existing = codeMap.putIfAbsent(code, localeMap);
                if (existing != null) {
                    localeMap = existing;
                }
            }
            MessageFormat result = createMessageFormat(msg, locale);
            localeMap.put(locale, result);
            return result;
        }

        return null;
    }

    /**
     * 获取资源包中指定key所对应的值
     *
     * <p>Efficiently retrieve the String value for the specified key, or return {@code null} if not
     * found.
     *
     * <p>As of 4.2, the default implementation checks {@code containsKey} before it attempts to
     * call {@code getString} (which would require catching {@code MissingResourceException} for key
     * not found).
     *
     * <p>Can be overridden in subclasses.
     *
     * @param bundle the ResourceBundle to perform the lookup in
     * @param key the key to look up
     * @return the associated value, or {@code null} if none
     * @since 4.2
     * @see ResourceBundle#getString(String)
     * @see ResourceBundle#containsKey(String)
     */
    @Nullable
    protected String getStringOrNull(ResourceBundle bundle, String key) {
        if (bundle.containsKey(key)) {
            try {
                return bundle.getString(key);
            } catch (MissingResourceException ex) {
                // Assume key not found for some other reason
                // -> do NOT throw the exception to allow for checking parent message source.
            }
        }
        return null;
    }

    /** Show the configuration of this MessageSource. */
    @Override
    public String toString() {
        return getClass().getName() + ": basenames=" + getBasenameSet();
    }

    /**
     * Custom implementation of {@code ResourceBundle.Control}, adding support for custom file
     * encodings, deactivating the fallback to the system locale and activating ResourceBundle's
     * native cache, if desired.
     */
    private class MessageSourceControl extends ResourceBundle.Control {

        @Override
        @Nullable
        public ResourceBundle newBundle(
                String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {

            // Special handling of default encoding
            if (format.equals("java.properties")) {
                // 按不同区域获取所对应的不同资源文件名称
                String bundleName = toBundleName(baseName, locale);
                final String resourceName = toResourceName(bundleName, "properties");
                final ClassLoader classLoader = loader;
                final boolean reloadFlag = reload;
                InputStream inputStream;
                try {
                    inputStream =
                            AccessController.doPrivileged(
                                    (PrivilegedExceptionAction<InputStream>)
                                            () -> {
                                                InputStream is = null;
                                                if (reloadFlag) {
                                                    URL url = classLoader.getResource(resourceName);
                                                    if (url != null) {
                                                        URLConnection connection =
                                                                url.openConnection();
                                                        if (connection != null) {
                                                            connection.setUseCaches(false);
                                                            is = connection.getInputStream();
                                                        }
                                                    }
                                                } else {
                                                    is =
                                                            classLoader.getResourceAsStream(
                                                                    resourceName);
                                                }
                                                return is;
                                            });
                } catch (PrivilegedActionException ex) {
                    throw (IOException) ex.getException();
                }
                if (inputStream != null) {
                    String encoding = getDefaultEncoding();
                    if (encoding != null) {
                        try (InputStreamReader bundleReader =
                                new InputStreamReader(inputStream, encoding)) {
                            return loadBundle(bundleReader);
                        }
                    } else {
                        try (InputStream bundleStream = inputStream) {
                            return loadBundle(bundleStream);
                        }
                    }
                } else {
                    return null;
                }
            } else {
                // Delegate handling of "java.class" format to standard Control
                return super.newBundle(baseName, locale, format, loader, reload);
            }
        }

        @Override
        @Nullable
        public Locale getFallbackLocale(String baseName, Locale locale) {
            Locale defaultLocale = getDefaultLocale();
            return (defaultLocale != null && !defaultLocale.equals(locale) ? defaultLocale : null);
        }

        @Override
        public long getTimeToLive(String baseName, Locale locale) {
            long cacheMillis = getCacheMillis();
            return (cacheMillis >= 0 ? cacheMillis : super.getTimeToLive(baseName, locale));
        }

        @Override
        public boolean needsReload(
                String baseName,
                Locale locale,
                String format,
                ClassLoader loader,
                ResourceBundle bundle,
                long loadTime) {

            if (super.needsReload(baseName, locale, format, loader, bundle, loadTime)) {
                cachedBundleMessageFormats.remove(bundle);
                return true;
            } else {
                return false;
            }
        }
    }
}
