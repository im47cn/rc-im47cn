import { vi } from 'vitest'
import { config } from '@vue/test-utils'

// Mock Element Plus components globally
config.global.stubs = {
  ElButton: { template: '<button><slot /></button>' },
  ElInput: { template: '<input />', props: ['modelValue'] },
  ElInputNumber: { template: '<input type="number" />', props: ['modelValue'] },
  ElForm: { template: '<form><slot /></form>', props: ['model', 'rules'], methods: { validate: () => Promise.resolve(true) } },
  ElFormItem: { template: '<div>{{ label }}<slot /></div>', props: ['label', 'prop'] },
  ElSelect: { template: '<select><slot /></select>', props: ['modelValue'] },
  ElOption: { template: '<option />', props: ['label', 'value'] },
  ElTable: {
    template: '<table><slot /></table>',
    props: ['data'],
    emits: ['selection-change']
  },
  ElTableColumn: { template: '<td />', props: ['prop', 'label', 'type', 'width', 'align', 'fixed', 'showOverflowTooltip', 'minWidth'] },
  ElTag: { template: '<span><slot /></span>', props: ['type'] },
  ElPagination: { template: '<div />', props: ['total', 'currentPage', 'pageSize'] },
  ElContainer: { template: '<div><slot /></div>' },
  ElAside: { template: '<div><slot /></div>' },
  ElHeader: { template: '<div><slot /></div>' },
  ElMain: { template: '<div><slot /></div>' },
  ElMenu: { template: '<div><slot /></div>' },
  ElMenuItem: { template: '<div><slot /></div>' },
  ElIcon: { template: '<span><slot /></span>' },
  ElDivider: { template: '<hr />' },
  ElRow: { template: '<div><slot /></div>' },
  ElCol: { template: '<div><slot /></div>' },
  ElTabs: { template: '<div><slot /></div>', props: ['modelValue'] },
  ElTabPane: { template: '<div><slot /></div>', props: ['label', 'name'] },
  ElSwitch: { template: '<input type="checkbox" />', props: ['modelValue'] },
  ElAlert: { template: '<div><slot /></div>', props: ['title', 'type'] },
  ElTooltip: { template: '<div><slot /></div>', props: ['content'] },
  Setting: { template: '<span />' },
  Warning: { template: '<span />' }
}

// Mock ElMessage / ElMessageBox
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    },
    ElMessageBox: {
      prompt: vi.fn()
    }
  }
})

// Mock sessionStorage for auth store
const sessionStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: (key: string) => store[key] ?? null,
    setItem: (key: string, value: string) => { store[key] = value },
    removeItem: (key: string) => { delete store[key] },
    clear: () => { store = {} }
  }
})()
Object.defineProperty(globalThis, 'sessionStorage', { value: sessionStorageMock })
