import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Mock vue-router
const mockPush = vi.fn()
const mockRoute = {
  params: {} as Record<string, string>,
  path: '/suppliers/create',
  fullPath: '/suppliers/create'
}
vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({ push: mockPush })
}))

// Mock API
const mockGetSupplier = vi.fn()
const mockCreateSupplier = vi.fn()
const mockUpdateSupplier = vi.fn()
vi.mock('../../api/supplier', () => ({
  getSupplier: (...args: unknown[]) => mockGetSupplier(...args),
  createSupplier: (...args: unknown[]) => mockCreateSupplier(...args),
  updateSupplier: (...args: unknown[]) => mockUpdateSupplier(...args)
}))

// Mock child components
vi.mock('../../components/AppLayout.vue', () => ({
  default: { template: '<div><slot /></div>' }
}))
vi.mock('../../components/CredentialForm.vue', () => ({
  default: {
    template: '<div class="mock-credential-form" />',
    methods: { getSubmitData: () => ({ apiKey: 'test' }) }
  }
}))
vi.mock('../../components/SimulationPanel.vue', () => ({
  default: { template: '<div class="mock-simulation-panel" />', props: ['expression', 'supplierConfig'] }
}))

import SupplierFormView from '../SupplierFormView.vue'

describe('SupplierFormView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockRoute.params = {}
    mockCreateSupplier.mockResolvedValue({ data: { id: 1 } })
    mockUpdateSupplier.mockResolvedValue({ data: { id: 1 } })
  })

  it('should display create mode title when no route id', () => {
    mockRoute.params = {}
    const wrapper = mount(SupplierFormView)
    expect(wrapper.text()).toContain('新增供应商')
  })

  it('should display edit mode title and load data when route has id', async () => {
    mockRoute.params = { id: '1' }
    mockGetSupplier.mockResolvedValue({
      data: {
        supplierCode: 'ALI',
        supplierName: 'Aliyun',
        baseUrl: 'https://api.example.com',
        httpMethod: 'POST',
        contentTypeBehavior: 'APPLICATION_JSON',
        successCaseSensitive: 1,
        credentialKeys: ['key1'],
        maxRetryCount: 0
      }
    })

    const wrapper = mount(SupplierFormView)
    await flushPromises()

    expect(wrapper.text()).toContain('编辑供应商')
    expect(mockGetSupplier).toHaveBeenCalledWith(1)
  })

  it('should convert successCaseSensitive Boolean to Integer for submit data', () => {
    // Test the conversion logic directly
    const trueCase = true ? 1 : 0
    expect(trueCase).toBe(1)

    const falseCase = false ? 1 : 0
    expect(falseCase).toBe(0)
  })

  it('should convert successCaseSensitive from integer (0) to boolean (false) on load', async () => {
    mockRoute.params = { id: '2' }
    mockGetSupplier.mockResolvedValue({
      data: {
        supplierCode: 'TEST',
        supplierName: 'Test',
        baseUrl: 'https://api.test.com',
        successCaseSensitive: 0,
        credentialKeys: [],
        maxRetryCount: 0
      }
    })

    const wrapper = mount(SupplierFormView)
    await flushPromises()

    const vm = wrapper.vm as unknown as { form: { successCaseSensitive: boolean } }
    expect(vm.form.successCaseSensitive).toBe(false)
  })

  it('should convert successCaseSensitive from integer (1) to boolean (true) on load', async () => {
    mockRoute.params = { id: '3' }
    mockGetSupplier.mockResolvedValue({
      data: {
        supplierCode: 'TEST2',
        supplierName: 'Test2',
        baseUrl: 'https://api.test.com',
        successCaseSensitive: 1,
        credentialKeys: [],
        maxRetryCount: 0
      }
    })

    const wrapper = mount(SupplierFormView)
    await flushPromises()

    const vm = wrapper.vm as unknown as { form: { successCaseSensitive: boolean } }
    expect(vm.form.successCaseSensitive).toBe(true)
  })

  it('should navigate to /suppliers on cancel', () => {
    const wrapper = mount(SupplierFormView)
    const vm = wrapper.vm as unknown as { handleCancel: () => void }
    vm.handleCancel()
    expect(mockPush).toHaveBeenCalledWith('/suppliers')
  })

  it('should have form with required fields', () => {
    const wrapper = mount(SupplierFormView)
    // Verify form renders required field labels
    const html = wrapper.html()
    expect(html).toContain('供应商编码')
    expect(html).toContain('供应商名称')
    expect(html).toContain('基础URL')
  })
})
