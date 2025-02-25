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
package org.apache.camel.component.file.remote.springboot.sftp;

import com.jcraft.jsch.ProxyHTTP;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.remote.springboot.ftp.BaseFtp;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext
@CamelSpringBootTest
@SpringBootTest(
        classes = {
                CamelAutoConfiguration.class,
                SftpSimpleProduceThroughProxyTest.class,
                SftpSimpleProduceThroughProxyTest.TestConfiguration.class
        }
)
//Based on SftpSimpleProduceThroughProxyIT
public class SftpSimpleProduceThroughProxyTest extends BaseSftp {

    private static HttpProxyServer proxyServer;
    private static final int proxyPort = AvailablePortFinder.getNextAvailable();

    @BeforeAll
    public static void setupProxy() {
        proxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(proxyPort)
                .withProxyAuthenticator(new ProxyAuthenticator() {
                    @Override
                    public boolean authenticate(String userName, String password) {
                        return "user".equals(userName) && "password".equals(password);
                    }

                    @Override
                    public String getRealm() {
                        return "myrealm";
                    }
                }).start();
    }

    @AfterAll
    public static void cleanup() {
        proxyServer.stop();
    }

    @Test
    public void testSftpSimpleProduceThroughProxy() {
        template.sendBodyAndHeader(
                "sftp://localhost:" + getPort() + "/" + getRootDir()
                                   + "?username=admin&password=admin&proxy=#proxy",
                "Hello World", Exchange.FILE_NAME,
                "hello.txt");

        File file = ftpFile("hello.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);
        assertEquals("Hello World", context.getTypeConverter().convertTo(String.class, file));
    }

    @Test
    public void testSftpSimpleSubPathProduceThroughProxy() {
        template.sendBodyAndHeader(
                "sftp://localhost:" + getPort() + "/" + getRootDir()
                                   + "/mysub?username=admin&password=admin&proxy=#proxy",
                "Bye World", Exchange.FILE_NAME,
                "bye.txt");

        File file = ftpFile("mysub/bye.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);
        assertEquals("Bye World", context.getTypeConverter().convertTo(String.class, file));
    }

    @Test
    public void testSftpSimpleTwoSubPathProduceThroughProxy() {
        template.sendBodyAndHeader("sftp://localhost:" + getPort() + "/" + getRootDir()
                                   + "/mysub/myother?username=admin&password=admin&proxy=#proxy",
                "Farewell World",
                Exchange.FILE_NAME, "farewell.txt");

        File file = ftpFile("mysub/myother/farewell.txt").toFile();
        assertTrue(file.exists(), "File should exist: " + file);
        assertEquals("Farewell World", context.getTypeConverter().convertTo(String.class, file));
    }




    // *************************************
    // Config
    // *************************************

    @Configuration
    public class TestConfiguration extends  BaseFtp.TestConfiguration {
        @Bean
        public RouteBuilder routeBuilder() {

            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:one").to("sftp://localhost:" + getPort() + "/" + getRootDir()
                            + "?username=admin&password=admin&proxy=#proxy");
                }
            };
        }

        @Bean(value = "proxy")
        public ProxyHTTP createProxy() {

            final ProxyHTTP proxyHTTP = new ProxyHTTP("localhost", proxyPort);
            proxyHTTP.setUserPasswd("user", "password");
            return proxyHTTP;
        }
    }

}
