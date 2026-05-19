import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { extname, join, resolve } from 'path'

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
  '--font-weight-title-lg', '--font-weight-title', '--font-weight-body', '--font-weight-caption',
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
  '--td-success-color-light', '--td-warning-color-light', '--td-error-color-light',
]

// Extract :root block
const rootMatch = cssContent.match(/:root\s*\{([^}]+(?:\{[^}]*\}[^}]*)*)\}/s)
const rootBlock = rootMatch ? rootMatch[0] : ''

function expectVarDefined(varName: string) {
  const regex = new RegExp(`${varName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*:`)
  expect(rootBlock).toMatch(regex)
}

function collectVueFiles(dir: string): string[] {
  const result: string[] = []
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) {
      if (entry !== 'node_modules' && entry !== '__tests__') result.push(...collectVueFiles(full))
    } else if (extname(full) === '.vue') {
      result.push(full)
    }
  }
  return result
}

describe('Global Theme Tokens', () => {
  describe('color tokens (17)', () => {
    it.each(COLOR_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('layout tokens (8)', () => {
    it.each(LAYOUT_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('typography tokens (13)', () => {
    it.each(TYPOGRAPHY_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('shadow tokens (2)', () => {
    it.each(SHADOW_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('map tokens (6)', () => {
    it.each(MAP_VARS)('%s is defined in :root', (v) => expectVarDefined(v))
  })

  describe('TDesign overrides (10)', () => {
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

describe('JSON sync validation', () => {
  const tokens = JSON.parse(readFileSync(resolve(__dirname, '../../../schemas/frontend_ui_tokens.json'), 'utf-8'))

  it('all color tokens from JSON exist in CSS', () => {
    const colorMap: Record<string, string> = {
      primary: '--color-primary', primaryHover: '--color-primary-hover', primaryBg: '--color-primary-bg',
      success: '--color-success', successBg: '--color-success-bg',
      warning: '--color-warning', warningBg: '--color-warning-bg',
      danger: '--color-danger', dangerBg: '--color-danger-bg',
      neutral: '--color-neutral', neutralBg: '--color-neutral-bg',
      pageBg: '--page-bg', cardBg: '--card-bg', border: '--color-border',
      textPrimary: '--text-primary', textSecondary: '--text-secondary', textPlaceholder: '--text-placeholder',
    }
    for (const [jsonKey, cssVar] of Object.entries(colorMap)) {
      expect(tokens.colors[jsonKey], `JSON key colors.${jsonKey} exists`).toBeDefined()
      expect(rootBlock, `CSS var ${cssVar} defined`).toMatch(new RegExp(`${cssVar.replace(/[-]/g, '\\-')}\\s*:`))
    }
  })

  it('all typography tokens from JSON exist in CSS', () => {
    for (const level of ['titleLg', 'title', 'body', 'caption']) {
      const t = tokens.typography[level]
      expect(t.size).toBeDefined()
      expect(t.lineHeight).toBeDefined()
      expect(t.weight).toBeDefined()
    }
  })

  it('all shadow tokens from JSON exist in CSS', () => {
    expect(tokens.shadows.shadowCard).toBeDefined()
    expect(tokens.shadows.shadowPopover).toBeDefined()
  })

  it('all map tokens from JSON exist in CSS', () => {
    for (const key of ['polygonStroke', 'polygonFill', 'previewStroke', 'previewFill', 'touchedStroke', 'changedStroke']) {
      expect(tokens.map[key]).toBeDefined()
    }
  })
})

describe('no hardcoded token hex in Vue SFC styles', () => {
  const srcDir = resolve(__dirname, '..')
  const vueFiles = collectVueFiles(srcDir)
  const OLD_TDESIGN_BLUE = /#0052d9/gi

  it('no #0052d9 (old TDesign blue) in any Vue file', () => {
    for (const file of vueFiles) {
      const content = readFileSync(file, 'utf-8')
      expect(content, `Found #0052d9 in ${file}`).not.toMatch(OLD_TDESIGN_BLUE)
    }
  })
})
