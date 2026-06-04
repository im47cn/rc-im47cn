import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Mock vue-router
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/publishers' }),
  useRouter: () => ({ push: vi.fn() })
}))

// Mock API
const mockGetPublisherList = vi.fn()
const mockCreatePublisher = vi.fn()
const mockUpdatePublisher = vi.fn()
const mockRotateApiKey = vi.fn()
vi.mock('../../api/publisher', () => ({
  getPublisherList: (...args: unknown[]) => mockGetPublisherList(...args),
  createPublisher: (...args: unknown[]) => mockCreatePublisher(...args),
  updatePublisher: (...args: unknown[]) => mockUpdatePublisher(...args),
  rotateApiKey: (...args: unknown[]) => mockRotateApiKey(...args)
}))

// Mock AppLayout and auth
vi.mock('../../components/AppLayout.vue', () => ({
  default: { template: '<div><slot /></div>' }
}))
vi.mock('../../api/auth', () => ({
  logout: vi.fn()
}))

import PublisherListView from '../PublisherListView.vue'

interface PublisherRecord {
  id: number
  publisherCode: string
  publisherName: string
  apiKey: string
  status: number
  contactInfo: string
  createTime: string
  updateTime: string
}

const publisherRecords: PublisherRecord[] = [
  {
    id: 1,
    publisherCode: 'order-service',
    publisherName: 'Order Service',
    apiKey: 'pk_abc123def456ghi789',
    status: 1,
    contactInfo: 'dev@example.com',
    createTime: '2026-01-01 00:00:00',
    updateTime: '2026-01-01 00:00:00'
  },
  {
    id: 2,
    publisherCode: 'user-service',
    publisherName: 'User Service',
    apiKey: 'pk_xyz789abc123def456',
    status: 0,
    contactInfo: '',
    createTime: '2026-01-02 00:00:00',
    updateTime: '2026-01-02 00:00:00'
  }
]

describe('PublisherListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockGetPublisherList.mockResolvedValue({
      data: { records: publisherRecords, total: 2 }
    })
  })

  it('should fetch publishers on mount', async () => {
    const wrapper = mount(PublisherListView)
    await flushPromises()

    expect(mockGetPublisherList).toHaveBeenCalledWith({
      page: 1,
      size: 10
    })

    const vm = wrapper.vm as unknown as { tableData: PublisherRecord[]; total: number }
    expect(vm.tableData.length).toBe(2)
    expect(vm.total).toBe(2)
    expect(vm.tableData[0].publisherCode).toBe('order-service')
  })

  it('should pass keyword filter when searching', async () => {
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      keyword: string
      handleSearch: () => void
    }
    vm.keyword = 'order'
    vm.handleSearch()
    await flushPromises()

    expect(mockGetPublisherList).toHaveBeenLastCalledWith(
      expect.objectContaining({ keyword: 'order' })
    )
  })

  it('should pass status filter when set', async () => {
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      statusFilter: number | ''
      handleSearch: () => void
    }
    vm.statusFilter = 1
    vm.handleSearch()
    await flushPromises()

    expect(mockGetPublisherList).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 1 })
    )
  })

  it('should reset to page 1 when searching', async () => {
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      currentPage: number
      handleSearch: () => void
    }
    vm.currentPage = 3
    vm.handleSearch()

    expect(vm.currentPage).toBe(1)
  })

  it('should mask API key correctly', async () => {
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      maskApiKey: (key: string) => string
    }
    expect(vm.maskApiKey('pk_abc123def456ghi789')).toBe('pk_abc123def...')
    expect(vm.maskApiKey('')).toBe('')
    expect(vm.maskApiKey('short')).toBe('short')
  })

  it('should open create dialog with empty form', async () => {
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      dialogVisible: boolean
      isEdit: boolean
      form: { publisherCode: string; publisherName: string }
      handleCreate: () => void
    }
    vm.handleCreate()

    expect(vm.dialogVisible).toBe(true)
    expect(vm.isEdit).toBe(false)
    expect(vm.form.publisherCode).toBe('')
    expect(vm.form.publisherName).toBe('')
  })

  it('should open edit dialog with row data', async () => {
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      dialogVisible: boolean
      isEdit: boolean
      editingId: number | null
      form: { publisherCode: string; publisherName: string; contactInfo: string }
      handleEdit: (row: PublisherRecord) => void
    }
    vm.handleEdit(publisherRecords[0])

    expect(vm.dialogVisible).toBe(true)
    expect(vm.isEdit).toBe(true)
    expect(vm.editingId).toBe(1)
    expect(vm.form.publisherCode).toBe('order-service')
    expect(vm.form.publisherName).toBe('Order Service')
  })

  it('should call createPublisher on submit in create mode', async () => {
    mockCreatePublisher.mockResolvedValue({ data: { apiKey: 'pk_new123' } })
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      isEdit: boolean
      form: { publisherCode: string; publisherName: string; contactInfo: string }
      handleCreate: () => void
      handleSubmit: () => Promise<void>
    }
    vm.handleCreate()
    vm.form.publisherCode = 'new-service'
    vm.form.publisherName = 'New Service'
    await vm.handleSubmit()
    await flushPromises()

    expect(mockCreatePublisher).toHaveBeenCalledWith({
      publisherCode: 'new-service',
      publisherName: 'New Service',
      contactInfo: ''
    })
  })

  it('should call updatePublisher on submit in edit mode', async () => {
    mockUpdatePublisher.mockResolvedValue({ data: {} })
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      handleEdit: (row: PublisherRecord) => void
      handleSubmit: () => Promise<void>
    }
    vm.handleEdit(publisherRecords[0])
    await vm.handleSubmit()
    await flushPromises()

    expect(mockUpdatePublisher).toHaveBeenCalledWith(1, {
      publisherName: 'Order Service',
      contactInfo: 'dev@example.com'
    })
  })

  it('should handle page size change', async () => {
    const wrapper = mount(PublisherListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      currentPage: number
      pageSize: number
      handleSizeChange: (size: number) => void
    }
    vm.currentPage = 3
    vm.handleSizeChange(20)

    expect(vm.pageSize).toBe(20)
    expect(vm.currentPage).toBe(1)
    expect(mockGetPublisherList).toHaveBeenLastCalledWith(
      expect.objectContaining({ size: 20, page: 1 })
    )
  })
})
