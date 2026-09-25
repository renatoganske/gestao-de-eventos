import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Table, type TableColumn } from './Table'

interface Row {
  code: string
  title: string
}

const rows: Row[] = [
  { code: 'EVT-018', title: 'Casamento Maria & João' },
  { code: 'EVT-019', title: 'Aniversário 15 anos' },
]

const columns: TableColumn<Row>[] = [
  { key: 'code', header: 'Código', render: (row) => row.code },
  { key: 'title', header: 'Evento', render: (row) => row.title },
]

describe('Table', () => {
  it('renderiza cabeçalho e uma linha por item', () => {
    render(<Table columns={columns} rows={rows} rowKey={(row) => row.code} />)

    expect(screen.getByRole('columnheader', { name: 'Código' })).toBeInTheDocument()
    expect(screen.getByRole('columnheader', { name: 'Evento' })).toBeInTheDocument()
    expect(screen.getAllByRole('row')).toHaveLength(rows.length + 1)
    expect(screen.getByText('EVT-018')).toBeInTheDocument()
  })

  it('chama onRowClick com a linha correspondente ao clicar', async () => {
    const onRowClick = vi.fn()
    render(<Table columns={columns} rows={rows} rowKey={(row) => row.code} onRowClick={onRowClick} />)

    await userEvent.click(screen.getByText('EVT-018'))

    expect(onRowClick).toHaveBeenCalledExactlyOnceWith(rows[0])
  })

  it('renderiza sem lançar quando não há linhas', () => {
    render(<Table columns={columns} rows={[]} rowKey={(row) => row.code} />)

    expect(screen.getAllByRole('row')).toHaveLength(1)
  })
})
