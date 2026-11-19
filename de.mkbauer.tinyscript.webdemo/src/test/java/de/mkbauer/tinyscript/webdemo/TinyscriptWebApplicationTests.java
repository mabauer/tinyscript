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

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import de.mkbauer.tinyscript.webdemo.TinyscriptWebApplication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Basic integration tests for service demo application.
 *
 */
@SpringBootTest(classes = TinyscriptWebApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class TinyscriptWebApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	public void testVersion() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + port + "/api/version", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertFalse(body.get("version").toString().isEmpty());
	}
	
	/**
	 * /test is the health check endpoint used by external monitoring (checkmk).
	 */
	@Test
	public void testHealthCheckEndpoint() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + port + "/test", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals(0, body.get("errorCode"));
		assertEquals("Hello, Tinyscript", body.get("value"));
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
