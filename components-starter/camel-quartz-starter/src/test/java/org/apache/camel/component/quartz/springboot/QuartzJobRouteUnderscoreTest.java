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
package org.apache.camel.component.quartz.springboot;



import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.quartz.JobDetail;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        QuartzJobRouteUnderscoreTest.class,
        QuartzJobRouteUnderscoreTest.TestConfiguration.class
    }
)
public class QuartzJobRouteUnderscoreTest extends BaseQuartzTest {

    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint mock;
    
    @Test
    public void testQuartzRoute() throws Exception {
        mock.expectedMessageCount(2);
        mock.message(0).header("triggerGroup").isEqualTo("my_group");
        mock.message(0).header("triggerName").isEqualTo("my_timer");

        mock.assertIsSatisfied();

        JobDetail detail = mock.getReceivedExchanges().get(0).getIn().getHeader("jobDetail", JobDetail.class);
        assertNotNull(detail);
        assertEquals("my_job", detail.getKey().getName());
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
                    from("quartz://my_group/my_timer?trigger.repeatInterval=2&trigger.repeatCount=1&job.name=my_job")
                            .to("mock:result");
                }
            };
        }
    }
    
   

}
