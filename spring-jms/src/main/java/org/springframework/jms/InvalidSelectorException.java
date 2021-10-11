/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jms;

/**
 * Runtime exception mirroring the JMS InvalidSelectorException.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see javax.jms.InvalidSelectorException
 */
@SuppressWarnings("serial")
public class InvalidSelectorException extends JmsException {

    public InvalidSelectorException(javax.jms.InvalidSelectorException cause) {
        super(cause);
    }
}
