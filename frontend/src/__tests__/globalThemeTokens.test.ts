import { describe, it, expect } from 'vitest'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const cssContent = readFileSync(resolve(__dirname, '../style.css'), 'utf-8')

const COLOR_VARS = [
  '--color-primary', '--color-primary-hover', '--color-primary-bg',
  '--color-success', '--color-success-bg',
  '--color-warning', '--color-warning-bg',
  '--color-danger', '--color-danger-bg',
  '--color-neutral', '--color-neutral-bg',
  '--page-bg', '--card-bg', '--color-border',
  '--text-primary', '--text-secondary', '--text-placeholder',
]

const LAYOUT_VARS = [
  '--top-nav-height', '--left-sidebar-width', '--right-sidebar-width',
  '--bottom-status-height', '--page-gap', '--card-padding',
  '--radius-card', '--radius-control',
]

const TYPOGRAPHY_VARS = [
  '--font-family',
  '--font-size-title-lg', '--font-size-title', '--font-size-body', '--font-size-caption',
  '--line-height-title-lg', '--line-height-title', '--line-height-body', '--line-height-caption',
]

const SHADOW_VARS = ['--shadow-card', '--shadow-popover']

const MAP_VARS = [
  '--map-polygon-stroke', '--map-polygon-fill',
  '--map-preview-stroke', '--map-preview-fill',
  '--map-touched-stroke', '--map-changed-stroke',
]

const TDESIGN_VARS = [
  '--td-brand-color', '--td-brand-color-hover', '--td-brand-color-active',
  '--td-brand-color-light', '--td-warning-color', '--td-error-color', '--td-success-color',
]

function expectVarDefined(varName: string) {
  const regex = new RegExp(`${varName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*:`)
  expect(cssContent).toMatch(regex)
}

describe('Global Theme Tokens', () => {
  describe('color tokens (17)', () => {
    it.each(COLOR_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('layout tokens (8)', () => {
    it.each(LAYOUT_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('typography tokens (9)', () => {
    it.each(TYPOGRAPHY_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('shadow tokens (2)', () => {
    it.each(SHADOW_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('map tokens (6)', () => {
    it.each(MAP_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('TDesign overrides (7)', () => {
    it.each(TDESIGN_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  it('no hardcoded #0052d9 (old TDesign blue) outside :root', () => {
    const rootEnd = cssContent.indexOf('}')
    const afterRoot = cssContent.slice(rootEnd + 1)
    expect(afterRoot).not.toContain('#0052d9')
  })

  it('no hardcoded #0052D9 (case variant) outside :root', () => {
    const rootEnd = cssContent.indexOf('}')
    const afterRoot = cssContent.slice(rootEnd + 1)
    expect(afterRoot.toLowerCase()).not.toContain('#0052d9')
  })

  it('color token values match frontend_ui_tokens.json', () => {
    expect(cssContent).toContain('--color-primary: #1664FF')
    expect(cssContent).toContain('--page-bg: #F5F7FA')
    expect(cssContent).toContain('--text-primary: #1D2129')
    expect(cssContent).toContain('--color-danger: #E34D59')
    expect(cssContent).toContain('--color-success: #00A870')
  })

  it('layout token values are correct', () => {
    expect(cssContent).toContain('--top-nav-height: 56px')
    expect(cssContent).toContain('--left-sidebar-width: 260px')
    expect(cssContent).toContain('--right-sidebar-width: 340px')
    expect(cssContent).toContain('--bottom-status-height: 36px')
    expect(cssContent).toContain('--radius-card: 8px')
  })
})
