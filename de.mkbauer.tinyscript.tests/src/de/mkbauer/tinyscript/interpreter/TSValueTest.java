package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.*;

import org.junit.Test;

public class TSValueTest {
	
	@Test
	public void testBoolean() {
		TSValue wahr = new TSValue(true);
		assert wahr.isBoolean();
		assertEquals(true, wahr.asBoolean());
		assertEquals("true", wahr.toString());
		TSValue falsch = new TSValue(false);
		assert falsch.isBoolean();
		assertEquals(false, falsch.asBoolean());
		assertEquals("false", falsch.toString());
	}

	@Test
	public void testInt() {
		TSValue i = new TSValue(1);
		assert i.isMathematicalInteger();
		assertEquals(1, i.asInt());
		assertEquals("1", i.toString());
	}
	
	@Test
	public void testDouble() {
		TSValue d = new TSValue(1.0);
		assert d.isMathematicalInteger();
		assertEquals(1, d.asDouble(), 0.00001);
		assertEquals("1", d.asString());
		assertEquals("1.0", d.toString());
	}

	@Test
	public void testString() {
		TSValue s = new TSValue("Hello!");
		assert s.isString();
		assertEquals("Hello!", s.asString());
		assertEquals("Hello!", s.toString());
	}
	
	@Test
	public void testObject() {
		TSObject o = new TSObject();
		o.put("key1", new TSValue("Hello"));
		o.put("key2", new TSValue("World"));
		TSValue v = new TSValue(o);
		assert v.isObject();
		assertEquals("{ key1: Hello, key2: World }", v.toString());
	}
	
	

}
