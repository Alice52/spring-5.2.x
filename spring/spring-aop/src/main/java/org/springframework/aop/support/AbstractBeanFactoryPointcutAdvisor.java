/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.aop.support;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 实现了BeanFactoryAware接口，若在Bean容器里注册可议注入BeanFactory，从而访问里面的实例
 *
 * <p>Abstract BeanFactory-based PointcutAdvisor that allows for any Advice to be configured as
 * reference to an Advice bean in a BeanFactory.
 *
 * <p>Specifying the name of an advice bean instead of the advice object itself (if running within a
 * BeanFactory) increases loose coupling at initialization time, in order to not initialize the
 * advice object until the pointcut actually matches.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #setAdviceBeanName
 * @see DefaultBeanFactoryPointcutAdvisor
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryPointcutAdvisor extends AbstractPointcutAdvisor
        implements BeanFactoryAware {

    @Nullable private String adviceBeanName;

    @Nullable private BeanFactory beanFactory;

    @Nullable private transient volatile Advice advice;

    private transient volatile Object adviceMonitor = new Object();

    /** Return the name of the advice bean that this advisor refers to, if any. */
    @Nullable
    public String getAdviceBeanName() {
        return this.adviceBeanName;
    }

    /**
     * Specify the name of the advice bean that this advisor should refer to.
     *
     * <p>An instance of the specified bean will be obtained on first access of this advisor's
     * advice. This advisor will only ever obtain at most one single instance of the advice bean,
     * caching the instance for the lifetime of the advisor.
     *
     * @see #getAdvice()
     */
    public void setAdviceBeanName(@Nullable String adviceBeanName) {
        this.adviceBeanName = adviceBeanName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        // 若在Spring环境下，会给AdviceMonitor重新赋值为：getSingletonMutex()
        resetAdviceMonitor();
    }

    private void resetAdviceMonitor() {
        if (this.beanFactory instanceof ConfigurableBeanFactory) {
            this.adviceMonitor = ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
        } else {
            this.adviceMonitor = new Object();
        }
    }

    @Override
    public Advice getAdvice() {
        Advice advice = this.advice;
        // 非Spring环境一般手动set进来，所以就直接返回吧
        if (advice != null) {
            return advice;
        }

        Assert.state(this.adviceBeanName != null, "'adviceBeanName' must be specified");
        Assert.state(
                this.beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");

        // 若bean是单例的，那就没什么好说的，直接去工厂里拿出来就完事了（Advice.class），有可能返回null
        if (this.beanFactory.isSingleton(this.adviceBeanName)) {
            // Rely on singleton semantics provided by the factory.
            advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
            this.advice = advice;
            return advice;
        }
        // 若是多例的，就加锁，然后调用getBean()给他生成一个新的实例即可
        else {
            // No singleton guarantees from the factory -> let's lock locally but
            // reuse the factory's singleton lock, just in case a lazy dependency
            // of our advice bean happens to trigger the singleton lock implicitly...
            synchronized (this.adviceMonitor) {
                advice = this.advice;
                if (advice == null) {
                    advice = this.beanFactory.getBean(this.adviceBeanName, Advice.class);
                    this.advice = advice;
                }
                return advice;
            }
        }
    }

    /**
     * Specify a particular instance of the target advice directly, avoiding lazy resolution in
     * {@link #getAdvice()}.
     *
     * @since 3.1
     */
    public void setAdvice(Advice advice) {
        synchronized (this.adviceMonitor) {
            this.advice = advice;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append(": advice ");
        if (this.adviceBeanName != null) {
            sb.append("bean '").append(this.adviceBeanName).append("'");
        } else {
            sb.append(this.advice);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Serialization support
    // ---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization, just initialize state after deserialization.
        ois.defaultReadObject();

        // Initialize transient fields.
        resetAdviceMonitor();
    }
}
