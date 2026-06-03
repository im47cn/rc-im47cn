import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import CredentialForm from '../CredentialForm.vue'

describe('CredentialForm', () => {
  describe('create mode (default)', () => {
    it('should add a KV pair via addCredential', async () => {
      const wrapper = mount(CredentialForm)

      // Initially no credential rows
      expect(wrapper.findAll('.credential-row').length).toBe(0)

      // Click add button
      await wrapper.find('.credential-form > button').trigger('click')
      expect(wrapper.findAll('.credential-row').length).toBe(1)

      // Add another
      await wrapper.find('.credential-form > button').trigger('click')
      expect(wrapper.findAll('.credential-row').length).toBe(2)
    })

    it('should remove a KV pair', async () => {
      const wrapper = mount(CredentialForm)

      // Add two pairs
      await wrapper.find('.credential-form > button').trigger('click')
      await wrapper.find('.credential-form > button').trigger('click')
      expect(wrapper.findAll('.credential-row').length).toBe(2)

      // Remove first one (click the delete button in the first row)
      const deleteBtn = wrapper.findAll('.credential-row')[0].findAll('button').pop()!
      await deleteBtn.trigger('click')
      expect(wrapper.findAll('.credential-row').length).toBe(1)
    })

    it('getSubmitData should return empty object when no credentials added', () => {
      const wrapper = mount(CredentialForm)
      const result = (wrapper.vm as InstanceType<typeof CredentialForm>).getSubmitData()
      expect(result).toEqual({})
    })

    it('getSubmitData should return entered key-value pairs', async () => {
      const wrapper = mount(CredentialForm)

      // Add a credential
      await wrapper.find('.credential-form > button').trigger('click')

      // Manually set credential data via the component's internal state
      const vm = wrapper.vm as unknown as { credentials: Array<{ key: string; value: string; keepOriginal: boolean; isExisting: boolean }> }
      vm.credentials[0].key = 'apiKey'
      vm.credentials[0].value = 'secret123'

      const result = (wrapper.vm as InstanceType<typeof CredentialForm>).getSubmitData()
      expect(result).toEqual({ apiKey: 'secret123' })
    })

    it('getSubmitData should skip items with empty keys', async () => {
      const wrapper = mount(CredentialForm)
      await wrapper.find('.credential-form > button').trigger('click')

      const vm = wrapper.vm as unknown as { credentials: Array<{ key: string; value: string; keepOriginal: boolean; isExisting: boolean }> }
      vm.credentials[0].key = '   '
      vm.credentials[0].value = 'somevalue'

      const result = (wrapper.vm as InstanceType<typeof CredentialForm>).getSubmitData()
      expect(result).toEqual({})
    })
  })

  describe('edit mode', () => {
    let wrapper: ReturnType<typeof mount>

    beforeEach(() => {
      wrapper = mount(CredentialForm, {
        props: {
          editMode: true,
          existingKeys: ['accessKeyId', 'accessKeySecret']
        }
      })
    })

    it('should show masked values (******) for existing credentials', () => {
      const rows = wrapper.findAll('.credential-row')
      expect(rows.length).toBe(2)

      const vm = wrapper.vm as unknown as { credentials: Array<{ key: string; value: string; keepOriginal: boolean; isExisting: boolean }> }
      expect(vm.credentials[0].key).toBe('accessKeyId')
      expect(vm.credentials[0].value).toBe('******')
      expect(vm.credentials[0].keepOriginal).toBe(true)
      expect(vm.credentials[0].isExisting).toBe(true)

      expect(vm.credentials[1].key).toBe('accessKeySecret')
      expect(vm.credentials[1].value).toBe('******')
    })

    it('getSubmitData should filter out keepOriginal items (returns null when no changes)', () => {
      const result = (wrapper.vm as InstanceType<typeof CredentialForm>).getSubmitData()
      // Edit mode with no changes returns null
      expect(result).toBeNull()
    })

    it('should toggle between keep original and reenter', () => {
      const vm = wrapper.vm as unknown as { credentials: Array<{ key: string; value: string; keepOriginal: boolean; isExisting: boolean }> }

      // Initially keepOriginal is true
      expect(vm.credentials[0].keepOriginal).toBe(true)
      expect(vm.credentials[0].value).toBe('******')

      // Toggle to reenter
      // Find the toggle button in the first row (it's the button before delete for existing items)
      const firstRow = wrapper.findAll('.credential-row')[0]
      const buttons = firstRow.findAll('button')
      // First button is the toggle (keep/reenter), second is delete
      buttons[0].trigger('click')

      expect(vm.credentials[0].keepOriginal).toBe(false)
      expect(vm.credentials[0].value).toBe('')

      // Toggle back
      buttons[0].trigger('click')

      expect(vm.credentials[0].keepOriginal).toBe(true)
      expect(vm.credentials[0].value).toBe('******')
    })

    it('getSubmitData should return changed items when reenter is chosen', () => {
      const vm = wrapper.vm as unknown as { credentials: Array<{ key: string; value: string; keepOriginal: boolean; isExisting: boolean }> }

      // Toggle first item to reenter and set new value
      vm.credentials[0].keepOriginal = false
      vm.credentials[0].value = 'newAccessKey'

      const result = (wrapper.vm as InstanceType<typeof CredentialForm>).getSubmitData()
      expect(result).toEqual({ accessKeyId: 'newAccessKey' })
    })
  })
})
