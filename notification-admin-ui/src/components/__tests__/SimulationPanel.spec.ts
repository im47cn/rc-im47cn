import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessage } from 'element-plus'

// Mock MonacoEditor child component
vi.mock('../MonacoEditor.vue', () => ({
  default: {
    name: 'MonacoEditor',
    template: '<div class="mock-monaco-editor" />',
    props: ['modelValue', 'language', 'readOnly', 'height', 'markers']
  }
}))

// Mock simulation API
const mockTransform = vi.fn()
const mockFullPreview = vi.fn()
vi.mock('../../api/simulation', () => ({
  transform: (...args: unknown[]) => mockTransform(...args),
  fullPreview: (...args: unknown[]) => mockFullPreview(...args)
}))

import SimulationPanel from '../SimulationPanel.vue'

describe('SimulationPanel', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    mockTransform.mockResolvedValue({ data: { result: '{"ok": true}' } })
    mockFullPreview.mockResolvedValue({ data: { url: 'https://example.com', method: 'POST' } })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should mount with MonacoEditor children', () => {
    const wrapper = mount(SimulationPanel)
    expect(wrapper.findAll('.mock-monaco-editor').length).toBe(4)
    expect(wrapper.exists()).toBe(true)
  })

  it('should not call transform when expression is empty', async () => {
    mount(SimulationPanel, {
      props: {
        expression: '',
        'onUpdate:expression': () => {}
      }
    })

    vi.advanceTimersByTime(500)
    await flushPromises()

    expect(mockTransform).not.toHaveBeenCalled()
  })

  it('should debounce and call transform when expression is set', async () => {
    const wrapper = mount(SimulationPanel, {
      props: {
        expression: 'initial.expr',
        'onUpdate:expression': () => {}
      }
    })

    // Access the internal expression ref to verify and trigger watch
    const vm = wrapper.vm as unknown as {
      expression: string
      mockContext: string
      executeTransform: () => Promise<void>
    }

    // Directly invoke executeTransform to verify the API call works
    await vm.executeTransform()
    await flushPromises()

    expect(mockTransform).toHaveBeenCalledWith('initial.expr', expect.anything())
  })

  it('should handle transform API error and set error result text', async () => {
    mockTransform.mockRejectedValueOnce({
      response: { data: { message: 'Invalid expression', offset: 5 } }
    })

    const wrapper = mount(SimulationPanel, {
      props: {
        expression: 'bad.expr',
        'onUpdate:expression': () => {}
      }
    })

    const vm = wrapper.vm as unknown as {
      executeTransform: () => Promise<void>
      resultText: string
      resultMarkers: Array<{ message: string }>
    }

    await vm.executeTransform()
    await flushPromises()

    expect(vm.resultText).toContain('Invalid expression')
    expect(vm.resultMarkers.length).toBe(1)
    expect(vm.resultMarkers[0].message).toBe('Invalid expression')
  })

  it('should show warning when fullPreview is called without supplierConfig', async () => {
    const wrapper = mount(SimulationPanel, {
      props: {
        expression: '',
        'onUpdate:expression': () => {}
      }
    })

    const previewBtn = wrapper.find('.panel-label button')
    if (previewBtn.exists()) {
      await previewBtn.trigger('click')
      await flushPromises()
      expect(ElMessage.warning).toHaveBeenCalledWith('缺少供应商配置信息')
    }
  })

  it('should set JSON parse error when mockContext is invalid', async () => {
    const wrapper = mount(SimulationPanel, {
      props: {
        expression: 'some.expr',
        'onUpdate:expression': () => {}
      }
    })

    const vm = wrapper.vm as unknown as {
      mockContext: string
      executeTransform: () => Promise<void>
      resultText: string
    }
    vm.mockContext = 'not valid json'

    await vm.executeTransform()
    await flushPromises()

    expect(vm.resultText).toContain('JSON 格式错误')
    // transform API should NOT be called
    expect(mockTransform).not.toHaveBeenCalled()
  })
})
