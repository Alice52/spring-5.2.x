/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 此接口主要用来在redirect中传递参数
 *
 * <p>A strategy interface for retrieving and saving FlashMap instances. See {@link FlashMap} for a
 * general overview of flash attributes.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see FlashMap
 */
public interface FlashMapManager {

    /**
     * 从session中获取所有flashmap,然后删除过期的flashmap，寻找匹配当前请求的flashmap,并将匹配上的flashmap也删除
     * flashmap中有失效时间，目标请求参数，目标请求路径 该方法用于重定向后，获取flashmap，从而获取重定向前设置的参数
     *
     * <p>Find a FlashMap saved by a previous request that matches to the current request, remove it
     * from underlying storage, and also remove other expired FlashMap instances.
     *
     * <p>This method is invoked in the beginning of every request in contrast to {@link
     * #saveOutputFlashMap}, which is invoked only when there are flash attributes to be saved -
     * i.e. before a redirect.
     *
     * @param request the current request
     * @param response the current response
     * @return a FlashMap matching the current request or {@code null}
     */
    @Nullable
    FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response);

    /**
     * 保存参数到存储介质中
     *
     * <p>Save the given FlashMap, in some underlying storage and set the start of its expiration
     * period.
     *
     * <p><strong>NOTE:</strong> Invoke this method prior to a redirect in order to allow saving the
     * FlashMap in the HTTP session or in a response cookie before the response is committed.
     *
     * @param flashMap the FlashMap to save
     * @param request the current request
     * @param response the current response
     */
    void saveOutputFlashMap(
            FlashMap flashMap, HttpServletRequest request, HttpServletResponse response);
}
