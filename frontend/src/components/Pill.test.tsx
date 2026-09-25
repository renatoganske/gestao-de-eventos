import { render, screen } from '@testing-library/react'
import { Pill } from './Pill'

describe('Pill', () => {
  it.each([
    ['pending', 'pill-pending'],
    ['delivered', 'pill-delivered'],
    ['archived', 'pill-archived'],
    ['critical', 'pill-critical'],
  ] as const)('aplica a classe correspondente ao status %s', (status, expectedClass) => {
    render(<Pill status={status}>Rótulo</Pill>)

    expect(screen.getByText('Rótulo')).toHaveClass('pill', expectedClass)
  })
})
