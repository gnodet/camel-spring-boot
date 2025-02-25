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
package org.apache.camel.component.mongodb.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mongodb.MongoDbConstants;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;

import org.junit.jupiter.api.Test;

import org.bson.Document;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import com.mongodb.BasicDBObject;

import java.util.ArrayList;
import java.util.List;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@CamelSpringBootTest
@SpringBootTest(
		classes = {
				CamelAutoConfiguration.class,
				MongoDbInsertBatchIT.class,
				MongoDbInsertBatchIT.TestConfiguration.class,
				AbstractMongoDbITSupport.MongoConfiguration.class
		}
)
@DisabledIfSystemProperty(named = "ci.env.name", matches = "github.com", disabledReason = "Disabled on GH Action due to Docker limit")
public class MongoDbInsertBatchIT extends AbstractMongoDbITSupport {

	@Test
	public void testInsertBatch() {
		assertEquals(0, testCollection.countDocuments());

		Document a = new Document(MongoDbConstants.MONGO_ID, "testInsert1");
		a.append("MyId", 1).toJson();
		Document b = new Document(MongoDbConstants.MONGO_ID, "testInsert2");
		b.append("MyId", 2).toJson();
		Document c = new Document(MongoDbConstants.MONGO_ID, "testInsert3");
		c.append("MyId", 3).toJson();

		List<Document> taxGroupList = new ArrayList<>();
		taxGroupList.add(a);
		taxGroupList.add(b);
		taxGroupList.add(c);

		Exchange out = context.createFluentProducerTemplate()
				.to("direct:insert").withBody(taxGroupList).send();

		List oid = out.getMessage().getHeader(MongoDbConstants.OID, List.class);
		assertNotNull(oid);
		assertEquals(3, oid.size());

		Document out1 = testCollection.find(new BasicDBObject("_id", oid.get(0))).first();
		assertNotNull(out1);
		assertEquals(1, out1.getInteger("MyId"));
		Document out2 = testCollection.find(new BasicDBObject("_id", oid.get(1))).first();
		assertNotNull(out2);
		assertEquals(2, out2.getInteger("MyId"));
		Document out3 = testCollection.find(new BasicDBObject("_id", oid.get(2))).first();
		assertNotNull(out3);
		assertEquals(3, out3.getInteger("MyId"));
	}

	@Configuration
	public class TestConfiguration {

		@Bean
		public RouteBuilder routeBuilder() {
			return new RouteBuilder() {
				@Override
				public void configure() {
					from("direct:insert")
							.to("mongodb:myDb?database={{mongodb.testDb}}&collection={{mongodb.testCollection}}&operation=insert");
				}
			};
		}
	}
}
