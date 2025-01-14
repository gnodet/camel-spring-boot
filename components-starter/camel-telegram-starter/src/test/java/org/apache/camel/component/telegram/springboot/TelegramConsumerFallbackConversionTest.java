/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.telegram.springboot;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.awaitility.Awaitility;


@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        TelegramConsumerFallbackConversionTest.class,
        TelegramConsumerFallbackConversionTest.TestConfiguration.class
    }
)
public class TelegramConsumerFallbackConversionTest extends TelegramTestSupport {

    
    static TelegramMockRoutes mockRoutes;
    
    @EndpointInject("direct:message")
    protected ProducerTemplate template;
    
    @EndpointInject("mock:telegram")
    private MockEndpoint endpoint;

    @Test
    public void testEverythingOk() {

        template.sendBody(new BrandNewType("wrapped message"));

        List<OutgoingTextMessage> msgs = Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> mockRoutes.getMock("sendMessage").getRecordedMessages(),
                        rawMessages -> rawMessages.size() == 1)
                .stream()
                .map(message -> (OutgoingTextMessage) message)
                .collect(Collectors.toList());

        assertEquals(msgs.size(), 1, "List should be of size: " + 1 + " but is: " + msgs.size());
        String text = msgs.get(0).getText();
        assertEquals("wrapped message", text);
    }


    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:message")
                            .to("telegram:bots?authorizationToken=mock-token&chatId=1234");
                }
            };
        }

    }
    
    @Override
    @Bean
    protected TelegramMockRoutes createMockRoutes() {
        mockRoutes =
            new TelegramMockRoutes(port)
            .addEndpoint(
                    "sendMessage",
                    "POST",
                    OutgoingTextMessage.class,
                    TelegramTestUtil.stringResource("messages/send-message.json"),
                    TelegramTestUtil.stringResource("messages/send-message.json"),
                    TelegramTestUtil.stringResource("messages/send-message.json"));
        return mockRoutes;
    }
    
    private static class BrandNewType {

        String message;

        BrandNewType(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            // to use default conversion from Object to String
            return message;
        }
    }
}
