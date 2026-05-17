import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CaseIdInput from '@/components/CaseIdInput.vue'

describe('CaseIdInput', () => {
  it('valid case_id 启用扫描按钮并提交', async () => {
    const wrapper = mount(CaseIdInput)
    const input = wrapper.find('input')

    await input.setValue('2026010108')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.find('button').attributes('disabled')).toBeUndefined()
    expect(wrapper.emitted('submit')?.[0]).toEqual(['2026010108'])
  })

  it('invalid case_id 禁用按钮并显示格式提示', async () => {
    const wrapper = mount(CaseIdInput)

    await wrapper.find('input').setValue('2026023021')
    await wrapper.find('input').trigger('blur')

    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('格式: YYYYMMDDHH（时次 08 或 20）')
  })
})
