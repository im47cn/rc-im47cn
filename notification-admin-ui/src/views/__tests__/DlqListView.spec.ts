import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Mock vue-router
vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/dlq' }),
  useRouter: () => ({ push: vi.fn() })
}))

// Mock API
const mockGetDlqList = vi.fn()
const mockRetryDlq = vi.fn()
const mockBatchRetryDlq = vi.fn()
const mockIgnoreDlq = vi.fn()
vi.mock('../../api/dlq', () => ({
  getDlqList: (...args: unknown[]) => mockGetDlqList(...args),
  retryDlq: (...args: unknown[]) => mockRetryDlq(...args),
  batchRetryDlq: (...args: unknown[]) => mockBatchRetryDlq(...args),
  ignoreDlq: (...args: unknown[]) => mockIgnoreDlq(...args)
}))

// Mock auth API (used by AppLayout)
vi.mock('../../api/auth', () => ({
  logout: vi.fn()
}))

// Mock AppLayout
vi.mock('../../components/AppLayout.vue', () => ({
  default: { template: '<div><slot /></div>' }
}))

import DlqListView from '../DlqListView.vue'

interface DlqRecord {
  id: number
  bizSign: string
  traceId: string
  supplierCode: string
  errorMsg: string
  retryCount: number
  dlqStatus: number
  createTime: string
}

const dlqRecords: DlqRecord[] = [
  {
    id: 1,
    bizSign: 'BIZ001',
    traceId: 'TRACE001',
    supplierCode: 'ALI_YUN',
    errorMsg: 'Connection timeout',
    retryCount: 3,
    dlqStatus: 0,
    createTime: '2026-01-01 00:00:00'
  },
  {
    id: 2,
    bizSign: 'BIZ002',
    traceId: 'TRACE002',
    supplierCode: 'TENCENT',
    errorMsg: 'Server error',
    retryCount: 1,
    dlqStatus: 1,
    createTime: '2026-01-02 00:00:00'
  }
]

describe('DlqListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockGetDlqList.mockResolvedValue({
      data: { records: dlqRecords, total: 2 }
    })
  })

  it('should fetch DLQ records on mount', async () => {
    const wrapper = mount(DlqListView)
    await flushPromises()

    expect(mockGetDlqList).toHaveBeenCalledWith({
      page: 1,
      size: 10
    })

    // Verify internal state
    const vm = wrapper.vm as unknown as { tableData: DlqRecord[]; total: number }
    expect(vm.tableData.length).toBe(2)
    expect(vm.total).toBe(2)
    expect(vm.tableData[0].bizSign).toBe('BIZ001')
    expect(vm.tableData[1].supplierCode).toBe('TENCENT')
  })

  it('should render filter bar and batch retry button', async () => {
    const wrapper = mount(DlqListView)
    await flushPromises()

    const html = wrapper.html()
    // Filter bar elements are rendered (stubbed but present)
    expect(html).toContain('批量重试 (0)')
  })

  it('should have batch retry showing 0 selected when no rows selected', async () => {
    const wrapper = mount(DlqListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as { selectedRows: DlqRecord[] }
    expect(vm.selectedRows.length).toBe(0)

    const html = wrapper.html()
    expect(html).toContain('批量重试 (0)')
  })

  it('should update selectedRows when handleSelectionChange is called', async () => {
    const wrapper = mount(DlqListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      selectedRows: DlqRecord[]
      handleSelectionChange: (rows: DlqRecord[]) => void
    }
    vm.handleSelectionChange(dlqRecords)

    await wrapper.vm.$nextTick()

    expect(vm.selectedRows.length).toBe(2)
    const html = wrapper.html()
    expect(html).toContain('批量重试 (2)')
  })

  it('should reset to page 1 when search is triggered', async () => {
    const wrapper = mount(DlqListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      currentPage: number
      handleSearch: () => void
    }
    vm.currentPage = 3
    vm.handleSearch()

    expect(vm.currentPage).toBe(1)
    expect(mockGetDlqList).toHaveBeenCalledTimes(2) // once on mount, once on search
  })

  it('should pass supplierCode filter when set', async () => {
    const wrapper = mount(DlqListView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      supplierCode: string
      handleSearch: () => void
    }
    vm.supplierCode = 'ALI_YUN'
    vm.handleSearch()
    await flushPromises()

    expect(mockGetDlqList).toHaveBeenLastCalledWith(
      expect.objectContaining({ supplierCode: 'ALI_YUN' })
    )
  })
})
