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
package org.apache.camel.component.validator;

import org.apache.camel.Exchange;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                FileValidatorRouteTest.class
        }
)
public class FileValidatorRouteTest extends ContextTestSupport {

    @Test
    public void testValidMessage() throws Exception {
        validEndpoint.expectedMessageCount(1);
        finallyEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader(fileUri(),
                "<mail xmlns='http://foo.com/bar'><subject>Hey</subject><body>Hello world!</body></mail>", Exchange.FILE_NAME,
                "valid.xml");

        MockEndpoint.assertIsSatisfied(validEndpoint, invalidEndpoint, finallyEndpoint);

        // should be able to delete the file
        oneExchangeDone.matchesWaitTime();
        assertTrue(FileUtil.deleteFile(testFile("valid.xml").toFile()),
                "Should be able to delete the file");
    }

    @Test
    public void testInvalidMessage() throws Exception {
        invalidEndpoint.expectedMessageCount(1);
        finallyEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader(fileUri(),
                "<mail xmlns='http://foo.com/bar'><body>Hello world!</body></mail>", Exchange.FILE_NAME, "invalid.xml");

        MockEndpoint.assertIsSatisfied(validEndpoint, invalidEndpoint, finallyEndpoint);

        // should be able to delete the file
        oneExchangeDone.matchesWaitTime();
        assertTrue(FileUtil.deleteFile(testFile("invalid.xml").toFile()),
                "Should be able to delete the file");
    }

    @BeforeEach
    public void setUp() throws Exception {
        deleteTestDirectory();
        validEndpoint = context.getEndpoint("mock:valid", MockEndpoint.class);
        invalidEndpoint = context.getEndpoint("mock:invalid", MockEndpoint.class);
        finallyEndpoint = context.getEndpoint("mock:finally", MockEndpoint.class);

        validEndpoint.reset();
        invalidEndpoint.reset();
        finallyEndpoint.reset();

        oneExchangeDone = event().whenDone(1).create();
    }

    @Bean
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(fileUri("?noop=true")).doTry()
                        .to("validator:org/apache/camel/component/validator/schema.xsd").to("mock:valid")
                        .doCatch(ValidationException.class).to("mock:invalid").doFinally().to("mock:finally").end();
            }
        };
    }

}
