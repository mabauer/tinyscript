<script setup lang="ts">
import { computed } from 'vue'
import type { ExecutionResult } from '../types'

const props = defineProps<{
  result: ExecutionResult | null
  executing: boolean
}>()

const stats = computed((): string => {
  if (!props.result) return ''
  const s = props.result.statistics
  let str = '\n(time=' + s.executionTime + 'ms'
  if (s.mxCpuTime > 0)
    str += ', cpu=' + Math.round(s.mxCpuTime / 1_000_000) + 'ms'
  str += ', stmts=' + s.statements
  str += ', calldepth=' + s.callDepth
  if (s.objectsMax > 0)
    str += ', objs=' + s.objectsMax
  if (s.memoryMax > 0)
    str += ', umem=' + Math.round(s.memoryMax / 1024) + 'K'
  str += ', creates=' + s.objectCreations
  if (s.mxMAlloc > 0)
    str += ', malloc=' + Math.round(s.mxMAlloc / 1024) + 'K'
  str += ')\n'
  return str
})

const showSection = computed(() => props.result !== null || props.executing)
</script>

<template>
  <div v-if="showSection" id="status" class="row row_sep">
    <div class="col-md-12">
      <div v-if="executing" class="alert alert-info" role="alert">
        <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        <span class="ms-2">Running...</span>
      </div>
      <div
        v-if="result && result.errorCode === 0"
        class="alert alert-success"
        role="alert"
      >
        <span class="sr-only">Result:</span> OK
      </div>
      <div
        v-if="result && result.errorCode > 0"
        class="alert alert-danger"
        role="alert"
      >
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <span class="sr-only">Error:</span>
        {{ result.errorMessage }}
      </div>
    </div>
  </div>

  <div v-if="result" id="result" class="row">
    <div class="col-md-12">
      <pre>result = {{ result.value }}{{ stats }}</pre>
    </div>
  </div>

  <div v-if="result && result.output" id="output" class="row">
    <div class="col-md-12">
      <pre>{{ result.output }}</pre>
    </div>
  </div>
</template>
