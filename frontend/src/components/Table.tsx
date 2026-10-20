import { useState, type ReactNode } from 'react'
import { compareSortValues, type SortDirection, type SortValue } from '../utils/sort'

export interface TableColumn<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  /** When present the column is sortable: the header becomes a button that sorts by this value. */
  sortValue?: (row: T) => SortValue
}

export interface TableSort {
  key: string
  direction: SortDirection
  /** Key of another sortable column, ascending, used to break ties (e.g. date, then code). */
  thenBy?: string
}

export interface TableProps<T> {
  columns: TableColumn<T>[]
  rows: T[]
  rowKey: (row: T) => string
  onRowClick?: (row: T) => void
  defaultSort?: TableSort
  /** Controlled sort: when given, the table shows exactly this sort and `defaultSort` is ignored. */
  sort?: TableSort
  /** Called with the next sort when a header is clicked (in controlled and uncontrolled mode). */
  onSortChange?: (sort: TableSort) => void
}

export function Table<T>({ columns, rows, rowKey, onRowClick, defaultSort, sort: controlledSort, onSortChange }: TableProps<T>) {
  const [internalSort, setInternalSort] = useState<TableSort | undefined>(defaultSort)
  const isControlled = controlledSort !== undefined
  const sort = isControlled ? controlledSort : internalSort

  const sortValue = sort ? columns.find((column) => column.key === sort.key)?.sortValue : undefined
  const tieValue = sort?.thenBy ? columns.find((column) => column.key === sort.thenBy)?.sortValue : undefined

  // Sorting happens on a copy, in memory: no new API call, and the stable sort keeps ties in server order.
  const sortedRows =
    sort && sortValue
      ? [...rows].sort((a, b) => {
          const primary = compareSortValues(sortValue(a), sortValue(b), sort.direction)
          if (primary !== 0 || !tieValue) {
            return primary
          }
          return compareSortValues(tieValue(a), tieValue(b), 'asc')
        })
      : rows

  function toggleSort(key: string) {
    const next: TableSort = {
      key,
      direction: sort?.key === key && sort.direction === 'asc' ? 'desc' : 'asc',
      thenBy: sort?.thenBy,
    }
    if (!isControlled) {
      setInternalSort(next)
    }
    onSortChange?.(next)
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          {columns.map((column) => {
            if (!column.sortValue) {
              return <th key={column.key}>{column.header}</th>
            }
            const active = sort?.key === column.key
            return (
              <th key={column.key} aria-sort={active ? (sort.direction === 'asc' ? 'ascending' : 'descending') : 'none'}>
                <button type="button" className="sort-button" onClick={() => toggleSort(column.key)}>
                  {column.header}
                  {active && (
                    <span className="sort-indicator" aria-hidden="true">
                      {sort.direction === 'asc' ? '▲' : '▼'}
                    </span>
                  )}
                </button>
              </th>
            )
          })}
        </tr>
      </thead>
      <tbody>
        {sortedRows.map((row) => (
          <tr
            key={rowKey(row)}
            data-clickable={onRowClick ? 'true' : undefined}
            onClick={onRowClick ? () => onRowClick(row) : undefined}
          >
            {columns.map((column) => (
              <td key={column.key}>{column.render(row)}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
