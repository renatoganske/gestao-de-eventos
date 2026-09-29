import css from './components.css?raw'

// jsdom nao aplica o layout real, entao a regressao do GDE-40 (dropdown do
// Autocomplete cortado pelo overflow:hidden do .panel) e travada na regra CSS.
function ruleBody(selector: string): string | undefined {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return new RegExp(`(?:^|\\n)${escaped}\\s*\\{([^}]*)\\}`).exec(css)?.[1]
}

describe('overflow do painel (GDE-40)', () => {
  it('mantem o recorte padrao nos paineis comuns', () => {
    expect(ruleBody('.panel')).toMatch(/overflow:\s*hidden/)
  })

  it('deixa o overflow visivel em paineis que contem um Autocomplete', () => {
    expect(ruleBody('.panel:has(.autocomplete)')).toMatch(/overflow:\s*visible/)
  })
})
