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

package org.springframework.jms.support.destination;

import javax.jms.ConnectionFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class JmsDestinationAccessorTests {

    @Test
    public void testChokesIfDestinationResolverIsetToNullExplicitly() throws Exception {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        JmsDestinationAccessor accessor = new StubJmsDestinationAccessor();
        accessor.setConnectionFactory(connectionFactory);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> accessor.setDestinationResolver(null));
    }

    @Test
    public void testSessionTransactedModeReallyDoesDefaultToFalse() throws Exception {
        JmsDestinationAccessor accessor = new StubJmsDestinationAccessor();
        assertThat(accessor.isPubSubDomain())
                .as(
                        "The [pubSubDomain] property of JmsDestinationAccessor must default to "
                                + "false (i.e. Queues are used by default). Change this test (and the "
                                + "attendant Javadoc) if you have changed the default.")
                .isFalse();
    }

    private static class StubJmsDestinationAccessor extends JmsDestinationAccessor {}
}
