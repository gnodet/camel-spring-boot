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



import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.boot.CamelAutoConfiguration;

import org.junit.jupiter.api.Test;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
    classes = {
        CamelAutoConfiguration.class,
        StatefulQuartzRouteTest.class,
        StatefulQuartzRouteTest.TestConfiguration.class
    }
)
public class StatefulQuartzRouteTest extends BaseQuartzTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @Autowired
    ProducerTemplate template;
    
    @Autowired
    CamelContext context;
    
    @EndpointInject("mock:result")
    MockEndpoint resultEndpoint;
    
    @Test
    public void testQuartz() throws Exception {
        
        resultEndpoint.expectedMessageCount(2);
        resultEndpoint.message(0).header("triggerName").isEqualTo("myTimerName");
        resultEndpoint.message(0).header("triggerGroup").isEqualTo("myGroup");

        // lets test the receive worked
        resultEndpoint.assertIsSatisfied();

        List<Exchange> list = resultEndpoint.getReceivedExchanges();
        for (Exchange exchange : list) {
            Message in = exchange.getIn();
            log.debug("Received: {} with headers: {}", in, in.getHeaders());
        }
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
                    // START SNIPPET: example
                    from("quartz://myGroup/myTimerName?trigger.repeatInterval=2&trigger.repeatCount=1&stateful=true")
                            .to("mock:result");
                    // END SNIPPET: example
                }
            };
        }
    }
    
   

}
