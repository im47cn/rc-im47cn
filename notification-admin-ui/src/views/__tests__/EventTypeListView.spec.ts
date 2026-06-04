import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/event-types' }),
  useRouter: () => ({ push: mockPush })
}))

// Mock API
const mockGetEventTypeList = vi.fn()
const mockCreateEventType = vi.fn()
const mockUpdateEventType = vi.fn()
vi.mock('../../api/eventType', () => ({
  getEventTypeList: (...args: unknown[]) => mockGetEventTypeList(...args),
  createEventType: (...args: unknown[]) => mockCreateEventType(...args),
  updateEventType: (...args: unknown[]) => mockUpdateEventType(...args)
}))

// Mock AppLayout and auth
vi.mock('../../components/AppLayout.vue', () => ({
  default: { template: '<div><slot /></div>' }
}))
vi.mock('../../api/auth', () => ({
  logout: vi.fn()
}))

import EventTypeListView from '../EventTypeListView.vue'

interface EventTypeRecord {
  id: number
  eventTypeCode: string
  publisherCode: string
  displayName: string
  description: string
  payloadSchema: string
  status: string
  version: number
  createTime: string
  updateTime: string
}

const eventTypeRecords: EventTypeRecord[] = [
  {
    id: 1,
    eventTypeCode: 'ORDER_CREATED',
    publisherCode: 'order-service',
    displayName: '订单创建',
    description: '订单创建事件',
    payloadSchema: '{"properties":{"orderId":{"type":"string"}}}',
    status: 'ACTIVE',
    version: 2,
    createTime: '2026-01-01 00:00:00',
    updateTime: '2026-01-01 00:00:00'
  },
  {
    id: 2,
    eventTypeCode: 'USER_REGISTERED',
    publisherCode: 'user-service',
    displayName: '用户注册',
    description: '',
    payloadSchema: '',
    status: 'DRAFT',
    version: 1,
    createTime: '2026-01-02 00:00:00',
    updateTime: '2026-01-02 00:00:00'
  }
]

describe('EventTypeListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockGetEventTypeList.mockResolvedValue({
      data: { records: eventTypeRecords, total: 2 }
    })
  })

  it('should fetch event types on mount', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    expect(mockGetEventTypeList).toHaveBeenCalledWith({
      page: 1,
      size: 10
    })

    const vm = wrapper.vm as unknown as { tableData: EventTypeRecord[]; total: number }
    expect(vm.tableData.length).toBe(2)
    expect(vm.total).toBe(2)
    expect(vm.tableData[0].eventTypeCode).toBe('ORDER_CREATED')
  })

  it('should pass keyword filter when searching', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      keyword: string
      handleSearch: () => void
    }
    vm.keyword = 'ORDER'
    vm.handleSearch()
    await flushPromises()

    expect(mockGetEventTypeList).toHaveBeenLastCalledWith(
      expect.objectContaining({ keyword: 'ORDER' })
    )
  })

  it('should pass publisherCode filter when set', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      publisherCodeFilter: string
      handleSearch: () => void
    }
    vm.publisherCodeFilter = 'order-service'
    vm.handleSearch()
    await flushPromises()

    expect(mockGetEventTypeList).toHaveBeenLastCalledWith(
      expect.objectContaining({ publisherCode: 'order-service' })
    )
  })

  it('should pass status filter when set', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      statusFilter: string
      handleSearch: () => void
    }
    vm.statusFilter = 'ACTIVE'
    vm.handleSearch()
    await flushPromises()

    expect(mockGetEventTypeList).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'ACTIVE' })
    )
  })

  it('should navigate to detail page', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      handleDetail: (id: number) => void
    }
    vm.handleDetail(1)

    expect(mockPush).toHaveBeenCalledWith('/event-types/1')
  })

  it('should open create dialog with defaults', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      dialogVisible: boolean
      isEdit: boolean
      form: { eventTypeCode: string; status: string }
      handleCreate: () => void
    }
    vm.handleCreate()

    expect(vm.dialogVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.form.eventTypeCode).toBe('')
    expect(vm.form.status).toBe('DRAFT')
  })

  it('should open edit dialog with row data', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      dialogVisible: boolean
      isEdit: boolean
      editingId: number | null
      form: { eventTypeCode: string; displayName: string; status: string }
      handleEdit: (row: EventTypeRecord) => void
    }
    vm.handleEdit(eventTypeRecords[0])

    expect(vm.dialogVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.editingId).toBe(1)
    expect(vm.form.eventTypeCode).toBe('ORDER_CREATED')
    expect(vm.form.displayName).toBe('订单创建')
    expect(vm.form.status).toBe('ACTIVE')
  })

  it('should call createEventType on submit in create mode', async () => {
    mockCreateEventType.mockResolvedValue({ data: {} })
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      handleCreate: () => void
      form: { eventTypeCode: string; publisherCode: string; displayName: string; description: string; payloadSchema: string }
      handleSubmit: () => Promise<void>
    }
    vm.handleCreate()
    vm.form.eventTypeCode = 'PAYMENT_DONE'
    vm.form.publisherCode = 'payment-service'
    vm.form.displayName = '支付完成'
    vm.form.description = ''
    vm.form.payloadSchema = ''
    await vm.handleSubmit()
    await flushPromises()

    expect(mockCreateEventType).toHaveBeenCalledWith({
      eventTypeCode: 'PAYMENT_DONE',
      publisherCode: 'payment-service',
      displayName: '支付完成',
      description: '',
      payloadSchema: null
    })
  })

  it('should truncate long schema text', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      truncate: (str: string, len: number) => string
    }
    expect(vm.truncate('short', 60)).toBe('short')
    expect(vm.truncate('a'.repeat(100), 60)).toBe('a'.repeat(60) + '...')
    expect(vm.truncate('', 60)).toBe('')
  })

  it('should reset page on search', async () => {
    const wrapper = mount(EventTypeListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      currentPage: number
      handleSearch: () => void
    }
    vm.currentPage = 5
    vm.handleSearch()
    expect(vm.currentPage).toBe(1)
  })
})
