import { render, screen } from '@testing-library/react'
import { FormField } from './FormField'

describe('FormField', () => {
  it('associa o label ao campo via htmlFor/id', () => {
    render(
      <FormField label="Cliente" htmlFor="f-customer">
        <input id="f-customer" />
      </FormField>,
    )

    expect(screen.getByLabelText('Cliente')).toBeInTheDocument()
  })

  it('mostra a dica quando não há erro', () => {
    render(
      <FormField label="Cliente" htmlFor="f-customer" hint="Não achou? Cadastre sem sair desta tela.">
        <input id="f-customer" />
      </FormField>,
    )

    expect(screen.getByText('Não achou? Cadastre sem sair desta tela.')).toBeInTheDocument()
  })

  it('mostra o erro como alerta no lugar da dica', () => {
    render(
      <FormField label="Cliente" htmlFor="f-customer" hint="dica" error="Campo obrigatório">
        <input id="f-customer" />
      </FormField>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Campo obrigatório')
    expect(screen.queryByText('dica')).not.toBeInTheDocument()
  })
})
