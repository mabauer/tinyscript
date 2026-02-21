<script setup lang="ts">
import { ref } from 'vue'
import type { Ref } from 'vue'
import type { ExecutionResult } from './types'
import Toolbar from './components/Toolbar.vue'
import Editor from './components/Editor.vue'
import Output from './components/Output.vue'

const code: Ref<string> = ref('// Load an example from the dropdown or type your code here\nprint("Hello, World!");\n')
const result: Ref<ExecutionResult | null> = ref(null)
const executing: Ref<boolean> = ref(false)
const editorRef = ref<InstanceType<typeof Editor> | null>(null)

async function loadScript(url: string): Promise<void> {
  try {
    const response = await fetch(url)
    if (response.ok) {
      code.value = await response.text()
    } else {
      code.value = '// Example not available. This should not happen.'
    }
  } catch {
    code.value = '// Example not available. This should not happen.'
  }
  editorRef.value?.focus()
}

async function executeScript(): Promise<void> {
  result.value = null
  executing.value = true
  try {
    const response = await fetch('execute', {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: code.value,
    })
    const data: ExecutionResult = await response.json()
    if (data.errorCode > 0) {
      editorRef.value?.setErrorLine(data.errorLine)
    }
    result.value = data
  } finally {
    executing.value = false
  }
  editorRef.value?.focus()
}
</script>

<template>
  <div id="main">
    <div class="page-header">
      <div id="header" class="container">
        <div class="row">
          <div class="col-md-12">
            <h1>Tinyscript Testdrive</h1>
          </div>
        </div>
        <Toolbar :executing="executing" @load-script="loadScript" @execute="executeScript" />
      </div>
    </div>

    <div id="content" class="container">
      <div id="code" class="row mt-3">
        <div class="col-md-12">
          <Editor ref="editorRef" v-model="code" @execute="executeScript" />
        </div>
      </div>
      <div id="help" class="row">
        <div class="col-md-12">
          <small>or press <kbd>Ctrl</kbd>+<kbd>Enter</kbd></small>
        </div>
      </div>

      <Output :result="result" :executing="executing" />
    </div>

    <div class="page-footer">
      <div id="footer" class="container">
        <div class="row">
          <div class="col-md-12">
            <small><a href="https://github.com/mabauer/tinyscript">Tinyscript</a> Version 0.10.0, Markus Bauer</small>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
