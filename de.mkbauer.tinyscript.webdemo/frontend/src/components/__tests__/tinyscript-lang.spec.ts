import { describe, it, expect } from 'vitest'
import { tokenizeLine } from '../tinyscript-lang'

function typeOf(line: string, word: string): string | null {
  const tokens = tokenizeLine(line)
  const tok = tokens.find(t => t.text === word)
  return tok ? tok.type : null
}

describe('tinyscript-lang tokenizer', () => {
  it('assert tokenizes as atom, not variableName', () => {
    expect(typeOf('assert', 'assert')).toBe('atom')
  })

  it('true and false tokenize as atom', () => {
    expect(typeOf('true', 'true')).toBe('atom')
    expect(typeOf('false', 'false')).toBe('atom')
  })

  it('null and undefined tokenize as atom', () => {
    expect(typeOf('null', 'null')).toBe('atom')
    expect(typeOf('undefined', 'undefined')).toBe('atom')
  })

  it('keywords tokenize as keyword', () => {
    for (const kw of ['var', 'function', 'if', 'for', 'return', 'else', 'while', 'break', 'continue', 'new', 'delete']) {
      expect(typeOf(kw, kw)).toBe('keyword')
    }
  })

  it('string literals tokenize as string', () => {
    const tokens = tokenizeLine('"hello world"')
    expect(tokens.find(t => t.type === 'string')?.text).toBe('"hello world"')
  })

  it('single-quoted string literals tokenize as string', () => {
    const tokens = tokenizeLine("'hello'")
    expect(tokens.find(t => t.type === 'string')?.text).toBe("'hello'")
  })

  it('line comments tokenize as comment', () => {
    const tokens = tokenizeLine('// this is a comment')
    expect(tokens.find(t => t.type === 'comment')).toBeTruthy()
  })

  it('block comments tokenize as comment', () => {
    const tokens = tokenizeLine('/* block comment */')
    expect(tokens.find(t => t.type === 'comment')).toBeTruthy()
  })

  it('numeric literals tokenize as number', () => {
    expect(typeOf('42', '42')).toBe('number')
    expect(typeOf('3.14', '3.14')).toBe('number')
  })

  it('identifiers tokenize as variableName', () => {
    expect(typeOf('myVar', 'myVar')).toBe('variableName')
    expect(typeOf('print', 'print')).toBe('variableName')
  })

  it('Tinyscript for (var i = 1,3) — comma tokenizes without error', () => {
    // No token should be undefined; just check the tokenizer does not throw
    expect(() => tokenizeLine('for (var i = 1,3)')).not.toThrow()
    const tokens = tokenizeLine('for (var i = 1,3)')
    const types = tokens.map(t => t.type)
    // comma and digits should be present
    expect(tokens.some(t => t.text === '1')).toBe(true)
    expect(tokens.some(t => t.text === '3')).toBe(true)
    // no token should have type 'undefined' as a string
    expect(types.every(t => t !== 'undefined')).toBe(true)
  })

  it('Tinyscript for (var x: arr) — colon tokenizes without error', () => {
    expect(() => tokenizeLine('for (var x: arr)')).not.toThrow()
    const tokens = tokenizeLine('for (var x: arr)')
    expect(tokens.some(t => t.text === 'x')).toBe(true)
    expect(tokens.some(t => t.text === 'arr')).toBe(true)
  })
})
