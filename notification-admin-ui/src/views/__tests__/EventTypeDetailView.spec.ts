import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' }, path: '/event-types/1' }),
  useRouter: () => ({ push: mockPush })
}))

// Mock APIs
const mockGetEventType = vi.fn()
const mockGetFieldFingerprints = vi.fn()
const mockGetChangeRecords = vi.fn()
const mockConfirmChangeRecord = vi.fn()
const mockDismissChangeRecord = vi.fn()

vi.mock('../../api/eventType', () => ({
  getEventType: (...args: unknown[]) => mockGetEventType(...args)
}))
vi.mock('../../api/fieldFingerprint', () => ({
  getFieldFingerprints: (...args: unknown[]) => mockGetFieldFingerprints(...args)
}))
vi.mock('../../api/changeRecord', () => ({
  getChangeRecords: (...args: unknown[]) => mockGetChangeRecords(...args),
  confirmChangeRecord: (...args: unknown[]) => mockConfirmChangeRecord(...args),
  dismissChangeRecord: (...args: unknown[]) => mockDismissChangeRecord(...args)
}))

// Mock AppLayout and auth
vi.mock('../../components/AppLayout.vue', () => ({
  default: { template: '<div><slot /></div>' }
}))
vi.mock('../../api/auth', () => ({
  logout: vi.fn()
}))

import EventTypeDetailView from '../EventTypeDetailView.vue'

const eventTypeInfo = {
  id: 1,
  eventTypeCode: 'ORDER_CREATED',
  publisherCode: 'order-service',
  displayName: '订单创建',
  description: '订单创建事件',
  payloadSchema: '{"properties":{"orderId":{"type":"string"}}}',
  status: 'ACTIVE',
  version: 2
}

const fingerprints = [
  {
    id: 1,
    eventTypeCode: 'ORDER_CREATED',
    fieldPath: 'payload.orderId',
    observedType: 'STRING',
    firstSeenAt: '2026-01-01 00:00:00',
    lastSeenAt: '2026-01-05 12:00:00',
    sampleCount: 150,
    status: 'ACTIVE'
  },
  {
    id: 2,
    eventTypeCode: 'ORDER_CREATED',
    fieldPath: 'payload.amount',
    observedType: 'NUMBER',
    firstSeenAt: '2026-01-01 00:00:00',
    lastSeenAt: '2026-01-05 12:00:00',
    sampleCount: 148,
    status: 'ACTIVE'
  }
]

const changeRecords = [
  {
    id: 1,
    eventTypeCode: 'ORDER_CREATED',
    changeType: 'FIELD_ADDED',
    fieldPath: 'payload.address',
    oldValue: null,
    newValue: 'OBJECT',
    detectionSource: 'RUNTIME_INFERRED',
    confidence: 'MEDIUM',
    status: 'PENDING_REVIEW',
    affectedSubscriptions: '["ALIYUN","WECHAT"]',
    createdAt: '2026-01-03 10:00:00'
  },
  {
    id: 2,
    eventTypeCode: 'ORDER_CREATED',
    changeType: 'FIELD_REMOVED',
    fieldPath: 'payload.oldField',
    oldValue: 'STRING',
    newValue: null,
    detectionSource: 'SCHEMA_DIFF',
    confidence: 'HIGH',
    status: 'CONFIRMED',
    affectedSubscriptions: null,
    createdAt: '2026-01-04 10:00:00'
  }
]

describe('EventTypeDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    mockGetEventType.mockResolvedValue({ data: eventTypeInfo })
    mockGetFieldFingerprints.mockResolvedValue({ data: fingerprints })
    mockGetChangeRecords.mockResolvedValue({ data: { records: changeRecords } })
  })

  it('should fetch event type info on mount', async () => {
    const wrapper = mount(EventTypeDetailView)
    await flushPromises()

    expect(mockGetEventType).toHaveBeenCalledWith(1)

    const vm = wrapper.vm as unknown as { eventTypeInfo: typeof eventTypeInfo | null }
    expect(vm.eventTypeInfo).not.toBeNull()
    expect(vm.eventTypeInfo!.eventTypeCode).toBe('ORDER_CREATED')
    expect(vm.eventTypeInfo!.displayName).toBe('订单创建')
  })

  it('should fetch fingerprints after event type loads', async () => {
    mount(EventTypeDetailView)
    await flushPromises()

    expect(mockGetFieldFingerprints).toHaveBeenCalledWith({
      eventTypeCode: 'ORDER_CREATED'
    })
  })

  it('should fetch change records after event type loads', async () => {
    mount(EventTypeDetailView)
    await flushPromises()

    expect(mockGetChangeRecords).toHaveBeenCalledWith({
      eventTypeCode: 'ORDER_CREATED'
    })
  })

  it('should store fingerprints in state', async () => {
    const wrapper = mount(EventTypeDetailView)
    await flushPromises()

    const vm = wrapper.vm as unknown as { fingerprints: typeof fingerprints }
    expect(vm.fingerprints.length).toBe(2)
    expect(vm.fingerprints[0].fieldPath).toBe('payload.orderId')
    expect(vm.fingerprints[1].observedType).toBe('NUMBER')
  })

  it('should store change records in state', async () => {
    const wrapper = mount(EventTypeDetailView)
    await flushPromises()

    const vm = wrapper.vm as unknown as { changeRecords: typeof changeRecords }
    expect(vm.changeRecords.length).toBe(2)
    expect(vm.changeRecords[0].changeType).toBe('FIELD_ADDED')
    expect(vm.changeRecords[1].status).toBe('CONFIRMED')
  })

  it('should parse affectedSubscriptions JSON', async () => {
    const wrapper = mount(EventTypeDetailView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      parseAffected: (json: string) => string[]
    }
    expect(vm.parseAffected('["ALIYUN","WECHAT"]')).toEqual(['ALIYUN', 'WECHAT'])
    expect(vm.parseAffected('')).toEqual([])
    expect(vm.parseAffected('invalid')).toEqual([])
  })

  it('should call confirmChangeRecord API', async () => {
    mockConfirmChangeRecord.mockResolvedValue({ data: {} })
    const wrapper = mount(EventTypeDetailView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      handleConfirm: (record: typeof changeRecords[0]) => Promise<void>
    }
    await vm.handleConfirm(changeRecords[0])
    await flushPromises()

    expect(mockConfirmChangeRecord).toHaveBeenCalledWith(1)
  })

  it('should call dismissChangeRecord API', async () => {
    mockDismissChangeRecord.mockResolvedValue({ data: {} })
    const wrapper = mount(EventTypeDetailView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      handleDismiss: (record: typeof changeRecords[0]) => Promise<void>
    }
    await vm.handleDismiss(changeRecords[0])
    await flushPromises()

    expect(mockDismissChangeRecord).toHaveBeenCalledWith(1)
  })

  it('should navigate back to event-types list', async () => {
    const wrapper = mount(EventTypeDetailView)
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      goBack: () => void
    }
    vm.goBack()

    expect(mockPush).toHaveBeenCalledWith('/event-types')
  })

  it('should default to fields tab', async () => {
    const wrapper = mount(EventTypeDetailView)
    await flushPromises()

    const vm = wrapper.vm as unknown as { activeTab: string }
    expect(vm.activeTab).toBe('fields')
  })
})
