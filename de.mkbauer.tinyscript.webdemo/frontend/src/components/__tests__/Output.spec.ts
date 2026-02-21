import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Output from '../Output.vue'
import type { ExecutionResult, Statistics } from '../../types'

function makeStats(overrides: Partial<Statistics> = {}): Statistics {
  return {
    executionTime: 10,
    mxCpuTime: 0,
    statements: 5,
    callDepth: 2,
    objectsMax: 0,
    memoryMax: 0,
    objectCreations: 1,
    mxMAlloc: 0,
    ...overrides,
  }
}

function makeResult(overrides: Partial<ExecutionResult> = {}): ExecutionResult {
  return {
    errorCode: 0,
    value: 'undefined',
    output: '',
    statistics: makeStats(),
    errorMessage: '',
    errorLine: 0,
    ...overrides,
  }
}

describe('Output.vue', () => {
  it('renders nothing when result is null and not executing', () => {
    const wrapper = mount(Output, { props: { result: null, executing: false } })
    expect(wrapper.find('#status').exists()).toBe(false)
    expect(wrapper.find('#result').exists()).toBe(false)
  })

  it('shows spinner when executing', () => {
    const wrapper = mount(Output, { props: { result: null, executing: true } })
    expect(wrapper.find('.spinner-border').exists()).toBe(true)
  })

  it('stats with all-zero optional fields omits cpu/objs/umem/malloc', () => {
    const result = makeResult({ statistics: makeStats() })
    const wrapper = mount(Output, { props: { result, executing: false } })
    const text = wrapper.find('pre').text()
    expect(text).toContain('time=10ms')
    expect(text).toContain('stmts=5')
    expect(text).toContain('calldepth=2')
    expect(text).toContain('creates=1')
    expect(text).not.toContain('cpu=')
    expect(text).not.toContain('objs=')
    expect(text).not.toContain('umem=')
    expect(text).not.toContain('malloc=')
  })

  it('stats with all non-zero fields includes all segments in correct format', () => {
    const result = makeResult({
      statistics: makeStats({
        executionTime: 42,
        mxCpuTime: 5_000_000,
        statements: 100,
        callDepth: 8,
        objectsMax: 12,
        memoryMax: 2048,
        objectCreations: 7,
        mxMAlloc: 1024,
      }),
    })
    const wrapper = mount(Output, { props: { result, executing: false } })
    const text = wrapper.find('pre').text()
    expect(text).toContain('time=42ms')
    expect(text).toContain('cpu=5ms')
    expect(text).toContain('stmts=100')
    expect(text).toContain('calldepth=8')
    expect(text).toContain('objs=12')
    expect(text).toContain('umem=2K')
    expect(text).toContain('creates=7')
    expect(text).toContain('malloc=1K')
  })

  it('shows error alert with errorMessage when errorCode > 0', () => {
    const result = makeResult({ errorCode: 1, errorMessage: 'Syntax error at line 3' })
    const wrapper = mount(Output, { props: { result, executing: false } })
    expect(wrapper.find('.alert-danger').exists()).toBe(true)
    expect(wrapper.find('.alert-danger').text()).toContain('Syntax error at line 3')
  })

  it('shows success alert when errorCode === 0', () => {
    const result = makeResult({ errorCode: 0 })
    const wrapper = mount(Output, { props: { result, executing: false } })
    expect(wrapper.find('.alert-success').exists()).toBe(true)
    expect(wrapper.find('.alert-danger').exists()).toBe(false)
  })
})
