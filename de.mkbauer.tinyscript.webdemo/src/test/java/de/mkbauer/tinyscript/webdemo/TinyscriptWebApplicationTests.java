/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mkbauer.tinyscript.webdemo;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import de.mkbauer.tinyscript.webdemo.TinyscriptWebApplication;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Basic integration tests for service demo application.
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TinyscriptWebApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class TinyscriptWebApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	public void testHello() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + port + "/hello", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("Hello World", body.get("message"));
	}
	
	@Test
	public void testExecute() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().postForEntity("http://localhost:" + port + "/execute", "var hello = \"hello1234\"; hello;", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals(0, body.get("errorCode"));
		assertEquals("hello1234", body.get("value"));
	}
	
	@Test
	public void testExecuteWithSyntaxError() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().postForEntity("http://localhost:" + port + "/execute", "var ", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertFalse(0 == Integer.parseInt(body.get("errorCode").toString()));
	}
	
	@Test
	public void testXCompile() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().postForEntity("http://localhost:" + port + "/xcompile", "var hello = \"hello1234\"; hello;", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals(0, body.get("errorCode"));
	}
	
	@Test
	public void testXCompileWithSyntaxError() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().postForEntity("http://localhost:" + port + "/xcompile", "var ", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertFalse(0 == Integer.parseInt(body.get("errorCode").toString()));
	}

}
