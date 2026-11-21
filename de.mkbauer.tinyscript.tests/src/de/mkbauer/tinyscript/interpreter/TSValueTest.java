package de.mkbauer.tinyscript.interpreter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TSValueTest {
	
	@Test
	public void testBoolean() {
		TSValue wahr = TSValue.of(true);
		assert wahr.isBoolean();
		assertEquals(true, wahr.asBoolean());
		assertEquals("true", wahr.toString());
		TSValue falsch = TSValue.of(false);
		assert falsch.isBoolean();
		assertEquals(false, falsch.asBoolean());
		assertEquals("false", falsch.toString());
	}

	@Test
	public void testInt() {
		TSValue i = TSValue.of(1);
		assert i.isMathematicalInteger();
		assertEquals(1, i.asInt());
		assertEquals("1", i.toString());
	}
	
	@Test
	public void testDouble() {
		TSValue d = TSValue.of(1.0);
		assert d.isMathematicalInteger();
		assertEquals(1, d.asDouble(), 0.00001);
		assertEquals("1", d.asString());
		assertEquals("1", d.toString());
	}

	@Test
	public void testString() {
		TSValue s = TSValue.of("Hello!");
		assert s.isString();
		assertEquals("Hello!", s.asString());
		assertEquals("\"Hello!\"", s.toString());
	}
	
	@Test
	public void testObject() {
		/* TSObject o = new TSObject();
		o.put("key1", TSValue.of("Hello"));
		o.put("key2", TSValue.of("World"));
		TSValue v = TSValue.of(o);
		assert v.isObject();
		assertEquals("{key1: 'Hello', key2: 'World'}", v.toString());
		*/
	}
	
	

}
