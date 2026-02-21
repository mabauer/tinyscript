import { StreamLanguage } from '@codemirror/language'
import { tags } from '@lezer/highlight'

interface State {
  inBlockComment: boolean
}

const KEYWORDS = new Set([
  'var', 'function', 'if', 'else', 'for', 'while', 'return',
  'break', 'continue', 'new', 'delete',
])

const ATOMS = new Set([
  'assert', 'true', 'false', 'null', 'undefined',
])

/**
 * Tokenize a single line of code into an array of {text, type} tokens.
 * Used by tests; the StreamLanguage definition uses the same logic.
 */
export function tokenizeLine(line: string): Array<{ text: string; type: string | null }> {
  const result: Array<{ text: string; type: string | null }> = []
  const state: State = { inBlockComment: false }

  // Minimal stream impl that covers what our token() function needs
  let pos = 0

  function makeToken(): { text: string; type: string | null } | null {
    if (pos >= line.length) return null
    const start = pos

    // Inside block comment
    if (state.inBlockComment) {
      while (pos < line.length) {
        if (line[pos] === '*' && line[pos + 1] === '/') {
          pos += 2
          state.inBlockComment = false
          break
        }
        pos++
      }
      return { text: line.slice(start, pos), type: 'comment' }
    }

    // Skip whitespace
    if (/\s/.test(line[pos])) {
      while (pos < line.length && /\s/.test(line[pos])) pos++
      return { text: line.slice(start, pos), type: null }
    }

    const ch = line[pos++]

    // Strings
    if (ch === '"' || ch === "'") {
      while (pos < line.length) {
        const c = line[pos++]
        if (c === '\\') { pos++; continue }
        if (c === ch) break
      }
      return { text: line.slice(start, pos), type: 'string' }
    }

    // Comments
    if (ch === '/') {
      if (line[pos] === '/') {
        pos = line.length
        return { text: line.slice(start, pos), type: 'comment' }
      }
      if (line[pos] === '*') {
        pos++
        state.inBlockComment = true
        while (pos < line.length) {
          if (line[pos] === '*' && line[pos + 1] === '/') {
            pos += 2
            state.inBlockComment = false
            break
          }
          pos++
        }
        return { text: line.slice(start, pos), type: 'comment' }
      }
      // Eat compound operators like /=
      if (line[pos] === '=') pos++
      return { text: line.slice(start, pos), type: 'operator' }
    }

    // Numbers
    if (/\d/.test(ch)) {
      while (pos < line.length && /[\d.]/.test(line[pos])) pos++
      return { text: line.slice(start, pos), type: 'number' }
    }

    // Identifiers and keywords
    if (/[a-zA-Z_$]/.test(ch)) {
      while (pos < line.length && /[\w$]/.test(line[pos])) pos++
      const word = line.slice(start, pos)
      if (KEYWORDS.has(word)) return { text: word, type: 'keyword' }
      if (ATOMS.has(word)) return { text: word, type: 'atom' }
      return { text: word, type: 'variableName' }
    }

    // Multi-char operators
    if (/[+\-*%=<>!&|^~?]/.test(ch)) {
      while (pos < line.length && /[+\-*%=<>!&|^~?]/.test(line[pos])) pos++
      return { text: line.slice(start, pos), type: 'operator' }
    }

    // Single-char punctuation (parens, brackets, comma, colon, semicolon, dot, …)
    return { text: ch, type: null }
  }

  while (pos < line.length) {
    const tok = makeToken()
    if (tok) result.push(tok)
  }
  return result
}

export const tinyscriptLanguage = StreamLanguage.define<State>({
  startState: () => ({ inBlockComment: false }),

  token(stream, state): string | null {
    // Inside block comment
    if (state.inBlockComment) {
      while (!stream.eol()) {
        if (stream.match('*/')) {
          state.inBlockComment = false
          break
        }
        stream.next()
      }
      return 'comment'
    }

    if (stream.eatSpace()) return null

    const ch = stream.next()
    if (ch === null) return null

    // Strings
    if (ch === '"' || ch === "'") {
      while (!stream.eol()) {
        const c = stream.next()
        if (c === '\\') { stream.next(); continue }
        if (c === ch) break
      }
      return 'string'
    }

    // Comments
    if (ch === '/') {
      if (stream.eat('/')) {
        stream.skipToEnd()
        return 'comment'
      }
      if (stream.eat('*')) {
        state.inBlockComment = true
        while (!stream.eol()) {
          if (stream.match('*/')) {
            state.inBlockComment = false
            break
          }
          stream.next()
        }
        return 'comment'
      }
      stream.eat('=')
      return 'operator'
    }

    // Numbers
    if (/\d/.test(ch)) {
      stream.eatWhile(/[\d.]/)
      return 'number'
    }

    // Identifiers and keywords
    if (/[a-zA-Z_$]/.test(ch)) {
      stream.eatWhile(/[\w$]/)
      const word = stream.current()
      if (KEYWORDS.has(word)) return 'keyword'
      if (ATOMS.has(word)) return 'atom'
      return 'variableName'
    }

    // Operators
    if (/[+\-*%=<>!&|^~?]/.test(ch)) {
      stream.eatWhile(/[+\-*%=<>!&|^~?]/)
      return 'operator'
    }

    return null
  },

  languageData: {
    commentTokens: { line: '//', block: { open: '/*', close: '*/' } },
  },
})

// Expose tags for consumers that need to configure a highlighter
export { tags }
