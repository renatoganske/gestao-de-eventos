import { render, screen, within } from '@testing-library/react'
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

  describe('ordenação', () => {
    interface Item {
      id: string
      name: string | null
      qty: number | null
      group: string
    }

    const items: Item[] = [
      { id: '1', name: 'Bruno', qty: 10, group: 'b' },
      { id: '2', name: null, qty: 2, group: 'a' },
      { id: '3', name: 'ágata', qty: null, group: 'a' },
      { id: '4', name: 'Álvaro', qty: 2, group: 'b' },
    ]

    const sortable: TableColumn<Item>[] = [
      { key: 'name', header: 'Nome', render: (row) => row.name ?? '—', sortValue: (row) => row.name },
      { key: 'qty', header: 'Qtd', render: (row) => String(row.qty ?? '—'), sortValue: (row) => row.qty },
      { key: 'group', header: 'Grupo', render: (row) => row.group, sortValue: (row) => row.group },
      { key: 'plain', header: 'Sem ordem', render: () => 'x' },
    ]

    function ids() {
      return screen
        .getAllByRole('row')
        .slice(1)
        .map((row) => within(row).getAllByRole('cell')[0].textContent)
    }

    function renderItems(props: Partial<Parameters<typeof Table<Item>>[0]> = {}) {
      render(<Table columns={sortable} rows={items} rowKey={(row) => row.id} {...props} />)
    }

    it('só colunas com sortValue têm cabeçalho clicável', () => {
      renderItems()

      expect(screen.getByRole('button', { name: 'Nome' })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Sem ordem' })).not.toBeInTheDocument()
      expect(screen.getByRole('columnheader', { name: 'Nome' })).toHaveAttribute('aria-sort', 'none')
    })

    it('ordena asc no primeiro clique, inverte no segundo e mostra indicador e aria-sort', async () => {
      renderItems()

      await userEvent.click(screen.getByRole('button', { name: 'Nome' }))
      expect(ids()).toEqual(['ágata', 'Álvaro', 'Bruno', '—'])
      expect(screen.getByRole('columnheader', { name: /Nome/ })).toHaveAttribute('aria-sort', 'ascending')
      expect(screen.getByRole('columnheader', { name: /Nome/ })).toHaveTextContent('▲')

      await userEvent.click(screen.getByRole('button', { name: /Nome/ }))
      expect(ids()).toEqual(['Bruno', 'Álvaro', 'ágata', '—'])
      expect(screen.getByRole('columnheader', { name: /Nome/ })).toHaveAttribute('aria-sort', 'descending')
      expect(screen.getByRole('columnheader', { name: /Nome/ })).toHaveTextContent('▼')
    })

    it('trocar de coluna volta a ordenar crescente e limpa o indicador da anterior', async () => {
      renderItems({ defaultSort: { key: 'name', direction: 'desc' } })

      await userEvent.click(screen.getByRole('button', { name: /Qtd/ }))

      expect(screen.getByRole('columnheader', { name: /Qtd/ })).toHaveAttribute('aria-sort', 'ascending')
      expect(screen.getByRole('columnheader', { name: /Nome/ })).toHaveAttribute('aria-sort', 'none')
    })

    it('compara números numericamente e deixa nulos no fim nas duas direções', async () => {
      render(
        <Table
          columns={[{ key: 'qty', header: 'Qtd', render: (row: Item) => String(row.qty ?? '—'), sortValue: (row: Item) => row.qty }]}
          rows={items}
          rowKey={(row) => row.id}
        />,
      )

      await userEvent.click(screen.getByRole('button', { name: 'Qtd' }))
      expect(ids()).toEqual(['2', '2', '10', '—'])

      await userEvent.click(screen.getByRole('button', { name: /Qtd/ }))
      expect(ids()).toEqual(['10', '2', '2', '—'])
    })

    it('é estável: empates mantêm a ordem original, também ao inverter', async () => {
      render(
        <Table
          columns={[
            { key: 'group', header: 'Grupo', render: (row: Item) => `${row.group}${row.id}`, sortValue: (row: Item) => row.group },
          ]}
          rows={items}
          rowKey={(row) => row.id}
        />,
      )

      await userEvent.click(screen.getByRole('button', { name: 'Grupo' }))
      expect(ids()).toEqual(['a2', 'a3', 'b1', 'b4'])

      await userEvent.click(screen.getByRole('button', { name: /Grupo/ }))
      expect(ids()).toEqual(['b1', 'b4', 'a2', 'a3'])
    })

    it('defaultSort ordena já na primeira renderização; thenBy desempata sempre crescente', () => {
      renderItems({ defaultSort: { key: 'qty', direction: 'desc', thenBy: 'name' } })

      expect(ids()).toEqual(['Bruno', 'Álvaro', '—', 'ágata'])
      expect(screen.getByRole('columnheader', { name: /Qtd/ })).toHaveAttribute('aria-sort', 'descending')
    })

    it('sem defaultSort mantém a ordem recebida', () => {
      renderItems()

      expect(ids()).toEqual(['Bruno', '—', 'ágata', 'Álvaro'])
    })

    it('clicar no cabeçalho não dispara onRowClick', async () => {
      const onRowClick = vi.fn()
      renderItems({ onRowClick })

      await userEvent.click(screen.getByRole('button', { name: 'Nome' }))

      expect(onRowClick).not.toHaveBeenCalled()
    })
  })
})
