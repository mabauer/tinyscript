<script setup lang="ts">
import { onMounted, onBeforeUnmount, watch, ref } from 'vue'
import { EditorView, keymap } from '@codemirror/view'
import { EditorState, StateEffect, StateField } from '@codemirror/state'
import { Decoration, type DecorationSet } from '@codemirror/view'
import { defaultKeymap } from '@codemirror/commands'
import { basicSetup } from 'codemirror'
import { tinyscriptLanguage } from './tinyscript-lang'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{
  'update:modelValue': [value: string]
  'execute': []
}>()

const editorContainer = ref<HTMLElement | null>(null)
let view: EditorView | null = null

// --- Error line decoration ---
const setErrorLineEffect = StateEffect.define<number | null>()

const errorLineField = StateField.define<DecorationSet>({
  create: () => Decoration.none,
  update(decorations, tr) {
    // Clear decoration if document changed
    if (tr.docChanged) return Decoration.none
    for (const effect of tr.effects) {
      if (effect.is(setErrorLineEffect)) {
        const lineNum = effect.value
        if (lineNum === null || lineNum <= 0) return Decoration.none
        try {
          const line = tr.state.doc.line(lineNum)
          return Decoration.set([
            Decoration.line({ class: 'cm-error-line' }).range(line.from),
          ])
        } catch {
          return Decoration.none
        }
      }
    }
    return decorations.map(tr.changes)
  },
  provide: f => EditorView.decorations.from(f),
})

// --- Editor theme ---
const editorTheme = EditorView.theme({
  '&': { height: '400px', overflow: 'auto', fontSize: '14px' },
  '.cm-scroller': { fontFamily: 'monospace' },
  '.cm-error-line': { backgroundColor: '#f2dede' },
})

// --- Ctrl-Enter keymap ---
const executeKeymap = keymap.of([
  {
    key: 'Ctrl-Enter',
    run: () => {
      emit('execute')
      return true
    },
  },
])

function createEditorState(content: string): EditorState {
  return EditorState.create({
    doc: content,
    extensions: [
      basicSetup,
      tinyscriptLanguage,
      errorLineField,
      editorTheme,
      executeKeymap,
      keymap.of(defaultKeymap),
      EditorView.updateListener.of(update => {
        if (update.docChanged) {
          emit('update:modelValue', update.state.doc.toString())
        }
      }),
    ],
  })
}

onMounted(() => {
  if (!editorContainer.value) return
  view = new EditorView({
    state: createEditorState(props.modelValue),
    parent: editorContainer.value,
  })
})

onBeforeUnmount(() => {
  view?.destroy()
  view = null
})

// Sync external modelValue changes into CodeMirror only when content differs
watch(
  () => props.modelValue,
  newVal => {
    if (!view) return
    if (view.state.doc.toString() !== newVal) {
      view.setState(createEditorState(newVal))
    }
  },
)

// --- Public API ---
function setErrorLine(lineNum: number | null): void {
  view?.dispatch({ effects: setErrorLineEffect.of(lineNum) })
}

function focus(): void {
  view?.focus()
}

defineExpose({ setErrorLine, focus })
</script>

<template>
  <div ref="editorContainer" />
</template>
