import { describe, it, expect, vi, beforeEach } from 'vitest'

// vi.mock factories are hoisted - cannot reference top-level variables
// Use vi.hoisted to define mocks that vi.mock factories can reference
const { mockPush, mockClearAuth, mockWarning } = vi.hoisted(() => ({
  mockPush: vi.fn(),
  mockClearAuth: vi.fn(),
  mockWarning: vi.fn()
}))

// Mock router
vi.mock('../../router', () => ({
  default: {
    push: mockPush,
    currentRoute: { value: { fullPath: '/suppliers' } }
  }
}))

// Mock auth store
vi.mock('../../stores/auth', () => ({
  useAuthStore: () => ({
    clearAuth: mockClearAuth,
    isAuthenticated: true,
    username: 'test'
  })
}))

// Mock element-plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: mockWarning,
    info: vi.fn()
  }
}))

// Import after mocks
import request from '../request'

describe('request (Axios instance)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should have baseURL configured from env or empty string', () => {
    expect(request.defaults.baseURL).toBe('')
  })

  it('should have timeout set to 15000ms', () => {
    expect(request.defaults.timeout).toBe(15000)
  })

  it('should have withCredentials enabled', () => {
    expect(request.defaults.withCredentials).toBe(true)
  })

  it('should have Content-Type set to application/json', () => {
    expect(request.defaults.headers['Content-Type']).toBe('application/json')
  })

  it('should have a response interceptor registered', () => {
    const interceptors = request.interceptors.response as unknown as {
      handlers: Array<{ fulfilled: unknown; rejected: unknown }>
    }
    expect(interceptors.handlers.length).toBeGreaterThan(0)
  })

  it('should redirect to login on 401 response', async () => {
    const error = {
      response: { status: 401 }
    }

    const interceptors = request.interceptors.response as unknown as {
      handlers: Array<{ fulfilled: unknown; rejected: (error: unknown) => Promise<unknown> }>
    }
    const rejectionHandler = interceptors.handlers[0].rejected

    await rejectionHandler(error).catch(() => {})

    expect(mockClearAuth).toHaveBeenCalled()
    expect(mockWarning).toHaveBeenCalledWith('登录已过期，请重新登录')
    expect(mockPush).toHaveBeenCalledWith({
      name: 'Login',
      query: { redirect: '/suppliers' }
    })
  })

  it('should not redirect on non-401 errors', async () => {
    const error = {
      response: { status: 500 }
    }

    const interceptors = request.interceptors.response as unknown as {
      handlers: Array<{ fulfilled: unknown; rejected: (error: unknown) => Promise<unknown> }>
    }
    const rejectionHandler = interceptors.handlers[0].rejected

    await rejectionHandler(error).catch(() => {})

    expect(mockClearAuth).not.toHaveBeenCalled()
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('should reject the promise on any error', async () => {
    const error = {
      response: { status: 403 }
    }

    const interceptors = request.interceptors.response as unknown as {
      handlers: Array<{ fulfilled: unknown; rejected: (error: unknown) => Promise<unknown> }>
    }
    const rejectionHandler = interceptors.handlers[0].rejected

    await expect(rejectionHandler(error)).rejects.toEqual(error)
  })
})
