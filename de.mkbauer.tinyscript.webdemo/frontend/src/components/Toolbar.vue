<script setup lang="ts">
interface Example {
  file: string
  label: string
}

const examples: Example[] = [
  { file: 'helloworld.ts',           label: 'Hello World' },
  { file: 'expressions.ts',          label: 'Basic Expressions' },
  { file: 'strings.ts',              label: 'Strings' },
  { file: 'primes.ts',               label: 'Control flow: for and if Statements' },
  { file: 'fibonacci.ts',            label: 'Functions' },
  { file: 'fibonacci_recursive.ts',  label: 'Functions: recursion' },
  { file: 'arrays.ts',               label: 'Working with Arrays' },
  { file: 'closures.ts',             label: 'Closures' },
  { file: 'oo.ts',                   label: 'Objects and Prototypes' },
  { file: 'people.ts',               label: 'Monitoring Runtime Resources' },
]

defineProps<{ executing: boolean }>()

const emit = defineEmits<{
  'load-script': [url: string]
  'execute': []
}>()
</script>

<template>
  <div id="commands" class="row">
    <div class="col-md-4">
      <div class="btn-group">
        <button
          id="examples"
          type="button"
          class="btn btn-secondary dropdown-toggle"
          data-bs-toggle="dropdown"
          aria-expanded="false"
        >
          Examples
        </button>
        <ul class="dropdown-menu" aria-labelledby="examples">
          <li v-for="ex in examples" :key="ex.file">
            <a class="dropdown-item" href="#" @click.prevent="emit('load-script', ex.file)">
              {{ ex.label }}
            </a>
          </li>
        </ul>
      </div>
      <button
        id="execute"
        class="btn btn-primary ms-2"
        :disabled="executing"
        @click="emit('execute')"
      >
        Execute
      </button>
    </div>
  </div>
</template>
