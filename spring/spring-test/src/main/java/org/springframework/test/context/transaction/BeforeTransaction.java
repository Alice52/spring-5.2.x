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

package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test annotation which indicates that the annotated {@code void} method should be executed
 * <em>before</em> a transaction is started for a test method configured to run within a transaction
 * via Spring's {@code @Transactional} annotation.
 *
 * <p>{@code @BeforeTransaction} methods declared in superclasses or as interface default methods
 * will be executed before those of the current test class.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom <em>composed
 * annotations</em>.
 *
 * <p>As of Spring Framework 4.3, {@code @BeforeTransaction} may also be declared on Java 8 based
 * interface default methods.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.transaction.annotation.Transactional
 * @see AfterTransaction
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeforeTransaction {}
