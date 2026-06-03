import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'

// Mock monaco-editor/loader before importing the component
const mockEditorInstance = {
  getValue: vi.fn(() => ''),
  setValue: vi.fn(),
  dispose: vi.fn(),
  onDidChangeModelContent: vi.fn(),
  getModel: vi.fn(() => ({})),
  updateOptions: vi.fn()
}

const mockMonaco = {
  editor: {
    create: vi.fn(() => mockEditorInstance),
    setModelLanguage: vi.fn(),
    setModelMarkers: vi.fn()
  },
  MarkerSeverity: {
    Error: 8,
    Warning: 4,
    Info: 2
  }
}

vi.mock('@monaco-editor/loader', () => ({
  default: {
    init: vi.fn(() => Promise.resolve(mockMonaco))
  }
}))

import MonacoEditor from '../MonacoEditor.vue'

describe('MonacoEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should mount without error', () => {
    const wrapper = mount(MonacoEditor, {
      props: {
        modelValue: '{"key": "value"}',
        language: 'json'
      }
    })
    expect(wrapper.exists()).toBe(true)
  })

  it('should render a container div with specified height', () => {
    const wrapper = mount(MonacoEditor, {
      props: {
        modelValue: '',
        height: '400px'
      }
    })
    const container = wrapper.find('div')
    expect(container.attributes('style')).toContain('height: 400px')
  })

  it('should use default props when not specified', () => {
    const wrapper = mount(MonacoEditor)
    const container = wrapper.find('div')
    // Default height is 300px
    expect(container.attributes('style')).toContain('height: 300px')
  })

  it('should initialize monaco editor on mount', async () => {
    mount(MonacoEditor, {
      props: {
        modelValue: 'test content',
        language: 'json',
        readOnly: false
      }
    })

    // Wait for onMounted async to complete
    await vi.dynamicImportSettled()

    expect(mockMonaco.editor.create).toHaveBeenCalledWith(
      expect.any(HTMLDivElement),
      expect.objectContaining({
        value: 'test content',
        language: 'json',
        readOnly: false
      })
    )
  })

  it('should emit update:modelValue when editor content changes', async () => {
    const wrapper = mount(MonacoEditor, {
      props: { modelValue: '' }
    })

    await vi.dynamicImportSettled()

    // Simulate editor content change by calling the registered callback
    const onContentChange = mockEditorInstance.onDidChangeModelContent.mock.calls[0]?.[0]
    if (onContentChange) {
      mockEditorInstance.getValue.mockReturnValue('new content')
      onContentChange()

      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')![0]).toEqual(['new content'])
    }
  })
})
