import type { EventDto } from '../api/events'

/** What the delete confirmation needs to know about the event; the counts say which links go away with it. */
export interface DeletableEvent {
  name: string
  eventDate: string
  customerCount: number
  professionalCount: number
}

export function deletableEvent(event: EventDto): DeletableEvent {
  return {
    name: event.name,
    eventDate: event.eventDate,
    customerCount: event.customers.length,
    professionalCount: event.eventProfessionals.length,
  }
}

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`
}

/** "2026-10-15" -> "15/10/2026"; anything unexpected is shown as received. */
export function formatIsoDate(iso: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso)
  return match ? `${match[3]}/${match[2]}/${match[1]}` : iso
}

/** Says what goes away with the event and what stays: the customers/professionals themselves are kept. */
export function deleteConfirmationMessage(event: DeletableEvent): string {
  const unlinked = [
    event.customerCount > 0 ? plural(event.customerCount, 'cliente', 'clientes') : null,
    event.professionalCount > 0 ? plural(event.professionalCount, 'profissional', 'profissionais') : null,
  ].filter((part): part is string => part !== null)
  const base = `Excluir o evento "${event.name}" (${formatIsoDate(event.eventDate)})? Esta ação não pode ser desfeita.`
  if (unlinked.length === 0) {
    return base
  }
  return `${base} Os vínculos com ${unlinked.join(' e ')} serão removidos; os cadastros em si não são apagados.`
}
