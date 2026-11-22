<script setup lang="ts">
interface Example {
  file: string
  label: string
}

const examples: Example[] = [
  { file: 'helloworld.tys',           label: 'Hello World' },
  { file: 'expressions.tys',          label: 'Basic Expressions' },
  { file: 'strings.tys',              label: 'Strings' },
  { file: 'primes.tys',               label: 'Control flow: for and if Statements' },
  { file: 'loops.tys',                label: 'Loops: for … : items, range and while' },
  { file: 'fibonacci.tys',            label: 'Functions' },
  { file: 'fibonacci_recursive.tys',  label: 'Functions: recursion' },
  { file: 'arrays.tys',               label: 'Working with Arrays' },
  { file: 'closures.tys',             label: 'Closures' },
  { file: 'errors.tys',               label: 'Errors: throw and try / catch' },
  { file: 'oo.tys',                   label: 'Objects and Prototypes' },
  { file: 'modules.tys',              label: 'Modules and the Standard Library' },
  { file: 'people.tys',               label: 'Monitoring Runtime Resources' },
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
