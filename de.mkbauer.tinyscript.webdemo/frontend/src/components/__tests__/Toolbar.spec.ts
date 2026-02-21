import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Toolbar from '../Toolbar.vue'

describe('Toolbar.vue', () => {
  it('renders all 10 example menu items', () => {
    const wrapper = mount(Toolbar, { props: { executing: false } })
    const items = wrapper.findAll('.dropdown-item')
    expect(items).toHaveLength(10)
  })

  it('clicking a menu item emits load-script with the correct filename', async () => {
    const wrapper = mount(Toolbar, { props: { executing: false } })
    const firstItem = wrapper.findAll('.dropdown-item')[0]
    await firstItem.trigger('click')
    const emitted = wrapper.emitted('load-script')
    expect(emitted).toBeTruthy()
    expect(emitted![0]).toEqual(['helloworld.ts'])
  })

  it('clicking the execute button emits execute', async () => {
    const wrapper = mount(Toolbar, { props: { executing: false } })
    await wrapper.find('#execute').trigger('click')
    expect(wrapper.emitted('execute')).toBeTruthy()
  })

  it('execute button is disabled when executing prop is true', () => {
    const wrapper = mount(Toolbar, { props: { executing: true } })
    const btn = wrapper.find<HTMLButtonElement>('#execute')
    expect(btn.element.disabled).toBe(true)
  })
})
