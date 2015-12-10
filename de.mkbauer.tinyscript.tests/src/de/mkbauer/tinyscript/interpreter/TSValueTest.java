package de.mkbauer.tinyscript.interpreter;

import static org.junit.Assert.*;

import org.junit.Test;

public class TSValueTest {

	@Test
	public void testInt() {
		TSValue i = new TSValue(1);
		assert i.isMathematicalInteger();
		assertEquals(1, i.asInt());
		assertEquals("1", i.toString());
	}

	@Test
	public void testString() {
		TSValue s = new TSValue("Hello!");
		assert s.isString();
		assertEquals("Hello!", s.asString());
		assertEquals("Hello!", s.toString());
	}
	

}
