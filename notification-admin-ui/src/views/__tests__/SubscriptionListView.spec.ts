import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Mock vue-router
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/subscriptions' }),
  useRouter: () => ({ push: vi.fn() })
}))

// Mock API
const mockGetSubscriptionList = vi.fn()
const mockCreateSubscription = vi.fn()
const mockUpdateSubscription = vi.fn()
const mockDeleteSubscription = vi.fn()
vi.mock('../../api/subscription', () => ({
  getSubscriptionList: (...args: unknown[]) => mockGetSubscriptionList(...args),
  createSubscription: (...args: unknown[]) => mockCreateSubscription(...args),
  updateSubscription: (...args: unknown[]) => mockUpdateSubscription(...args),
  deleteSubscription: (...args: unknown[]) => mockDeleteSubscription(...args)
}))

// Mock supplier API
const mockGetSupplierByCode = vi.fn()
vi.mock('../../api/supplier', () => ({
  getSupplierByCode: (...args: unknown[]) => mockGetSupplierByCode(...args)
}))

// Mock AppLayout and auth
vi.mock('../../components/AppLayout.vue', () => ({
  default: { template: '<div><slot /></div>' }
}))
vi.mock('../../api/auth', () => ({
  logout: vi.fn()
}))

import SubscriptionListView from '../SubscriptionListView.vue'

interface SubscriptionRecord {
  id: number
  subscriberCode: string
  eventTypeCode: string
  status: string
  managedBy: string
  pathTemplate: string | null
  queryTemplate: string | null
  headerTemplate: string | null
  bodyTemplate: string | null
  connectTimeoutMs: number | null
  readTimeoutMs: number | null
  maxRetryCount: number | null
  retryBackoffInitialMs: number | null
  retryBackoffMultiplier: number | null
  retryBackoffMaxMs: number | null
  successHttpCodes: string | null
  successBodyPattern: string | null
  successBodyMatchMode: string | null
  createTime: string
  updateTime: string
}

const subscriptionRecords: SubscriptionRecord[] = [
  {
    id: 1,
    subscriberCode: 'ALIYUN',
    eventTypeCode: 'ORDER_CREATED',
    status: 'ACTIVE',
    managedBy: 'SUBSCRIBER',
    pathTemplate: '/custom/path',
    queryTemplate: null,
    headerTemplate: null,
    bodyTemplate: '{"orderId": orderId}',
    connectTimeoutMs: null,
    readTimeoutMs: 8000,
    maxRetryCount: 5,
    retryBackoffInitialMs: null,
    retryBackoffMultiplier: null,
    retryBackoffMaxMs: null,
    successHttpCodes: null,
    successBodyPattern: null,
    successBodyMatchMode: null,
    createTime: '2026-01-01 00:00:00',
    updateTime: '2026-01-01 00:00:00'
  },
  {
    id: 2,
    subscriberCode: 'WECHAT',
    eventTypeCode: 'ORDER_CREATED',
    status: 'SUSPENDED',
    managedBy: 'PLATFORM',
    pathTemplate: null,
    queryTemplate: null,
    headerTemplate: null,
    bodyTemplate: null,
    connectTimeoutMs: null,
    readTimeoutMs: null,
    maxRetryCount: null,
    retryBackoffInitialMs: null,
    retryBackoffMultiplier: null,
    retryBackoffMaxMs: null,
    successHttpCodes: null,
    successBodyPattern: null,
    successBodyMatchMode: null,
    createTime: '2026-01-02 00:00:00',
    updateTime: '2026-01-02 00:00:00'
  }
]

describe('SubscriptionListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockGetSubscriptionList.mockResolvedValue({
      data: { records: subscriptionRecords, total: 2 }
    })
  })

  it('should fetch subscriptions on mount', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    expect(mockGetSubscriptionList).toHaveBeenCalledWith({
      page: 1,
      size: 10
    })

    const vm = wrapper.vm as unknown as { tableData: SubscriptionRecord[]; total: number }
    expect(vm.tableData.length).toBe(2)
    expect(vm.total).toBe(2)
    expect(vm.tableData[0].subscriberCode).toBe('ALIYUN')
  })

  it('should pass subscriberCode filter when searching', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      subscriberCodeFilter: string
      handleSearch: () => void
    }
    vm.subscriberCodeFilter = 'ALIYUN'
    vm.handleSearch()
    await flushPromises()

    expect(mockGetSubscriptionList).toHaveBeenLastCalledWith(
      expect.objectContaining({ subscriberCode: 'ALIYUN' })
    )
  })

  it('should pass eventTypeCode filter when searching', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      eventTypeCodeFilter: string
      handleSearch: () => void
    }
    vm.eventTypeCodeFilter = 'ORDER_CREATED'
    vm.handleSearch()
    await flushPromises()

    expect(mockGetSubscriptionList).toHaveBeenLastCalledWith(
      expect.objectContaining({ eventTypeCode: 'ORDER_CREATED' })
    )
  })

  it('should pass status filter when set', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      statusFilter: string
      handleSearch: () => void
    }
    vm.statusFilter = 'ACTIVE'
    vm.handleSearch()
    await flushPromises()

    expect(mockGetSubscriptionList).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'ACTIVE' })
    )
  })

  it('should reset page on search', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      currentPage: number
      handleSearch: () => void
    }
    vm.currentPage = 3
    vm.handleSearch()
    expect(vm.currentPage).toBe(1)
  })

  it('should count overrides correctly', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      countOverrides: (row: SubscriptionRecord) => number
    }
    // Record 0 has pathTemplate, bodyTemplate, readTimeoutMs, maxRetryCount = 4
    expect(vm.countOverrides(subscriptionRecords[0])).toBe(4)
    // Record 1 has no overrides = 0
    expect(vm.countOverrides(subscriptionRecords[1])).toBe(0)
  })

  it('should detect hasOverrides correctly', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      hasOverrides: (row: SubscriptionRecord) => boolean
    }
    expect(vm.hasOverrides(subscriptionRecords[0])).toBe(true)
    expect(vm.hasOverrides(subscriptionRecords[1])).toBe(false)
  })

  it('should open create dialog with defaults', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      dialogVisible: boolean
      isEdit: boolean
      showOverrides: boolean
      form: { subscriberCode: string; status: string; managedBy: string }
      handleCreate: () => void
    }
    vm.handleCreate()

    expect(vm.dialogVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.showOverrides).toBe(false)
    expect(vm.form.subscriberCode).toBe('')
    expect(vm.form.status).toBe('ACTIVE')
    expect(vm.form.managedBy).toBe('SUBSCRIBER')
  })

  it('should open edit dialog with row data and show overrides if present', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      dialogVisible: boolean
      isEdit: boolean
      showOverrides: boolean
      editingId: number | null
      form: { subscriberCode: string; eventTypeCode: string; pathTemplate: string; bodyTemplate: string }
      handleEdit: (row: SubscriptionRecord) => void
    }
    vm.handleEdit(subscriptionRecords[0])

    expect(vm.dialogVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.editingId).toBe(1)
    expect(vm.showOverrides).toBe(true) // has overrides
    expect(vm.form.subscriberCode).toBe('ALIYUN')
    expect(vm.form.eventTypeCode).toBe('ORDER_CREATED')
    expect(vm.form.pathTemplate).toBe('/custom/path')
    expect(vm.form.bodyTemplate).toBe('{"orderId": orderId}')
  })

  it('should not show overrides section for edit with no overrides', async () => {
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      showOverrides: boolean
      handleEdit: (row: SubscriptionRecord) => void
    }
    vm.handleEdit(subscriptionRecords[1]) // WECHAT has no overrides

    expect(vm.showOverrides).toBe(false)
  })

  it('should call createSubscription with only non-empty override fields', async () => {
    mockCreateSubscription.mockResolvedValue({ data: {} })
    const wrapper = mount(SubscriptionListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      handleCreate: () => void
      form: { subscriberCode: string; eventTypeCode: string; status: string; managedBy: string; bodyTemplate: string }
      handleSubmit: () => Promise<void>
    }
    vm.handleCreate()
    vm.form.subscriberCode = 'DINGTALK'
    vm.form.eventTypeCode = 'ORDER_CREATED'
    vm.form.bodyTemplate = '{"msg": content}'
    await vm.handleSubmit()
    await flushPromises()

    expect(mockCreateSubscription).toHaveBeenCalledWith(
      expect.objectContaining({
        subscriberCode: 'DINGTALK',
        eventTypeCode: 'ORDER_CREATED',
        status: 'ACTIVE',
        managedBy: 'SUBSCRIBER',
        bodyTemplate: '{"msg": content}'
      })
    )
    // Should NOT contain empty override fields
    const callArg = mockCreateSubscription.mock.calls[0][0]
    expect(callArg).not.toHaveProperty('pathTemplate')
    expect(callArg).not.toHaveProperty('connectTimeoutMs')
  })
})
