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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.util.ClassUtils;

/**
 * 枚举类型的转换
 *
 * <p>A {@link ConditionalConverter} base implementation for enum-based converters.
 *
 * @author Stephane Nicoll
 * @since 4.3
 */
abstract class AbstractConditionalEnumConverter implements ConditionalConverter {

    // 借助conversionService这个接口完成转换逻辑，
    private final ConversionService conversionService;

    protected AbstractConditionalEnumConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        // 拿到source所有实现的接口，若没有实现任何接口，永远返回true
        for (Class<?> interfaceType :
                ClassUtils.getAllInterfacesForClassAsSet(sourceType.getType())) {
            // 委托给conversionService来判断是否能进行转换
            if (this.conversionService.canConvert(
                    TypeDescriptor.valueOf(interfaceType), targetType)) {
                return false;
            }
        }
        return true;
    }
}
