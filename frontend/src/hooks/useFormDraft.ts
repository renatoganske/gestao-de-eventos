import { useState } from 'react'

const DRAFT_PREFIX = 'gde_draft:'

function readDraft<T>(key: string, initialValue: T): T {
  try {
    const raw = sessionStorage.getItem(DRAFT_PREFIX + key)
    return raw ? (JSON.parse(raw) as T) : initialValue
  } catch {
    return initialValue
  }
}

/** Descarta o rascunho de `key` sem montar o formulário (ex.: ao clicar em "Novo ..." numa listagem). */
export function discardFormDraft(key: string): void {
  try {
    sessionStorage.removeItem(DRAFT_PREFIX + key)
  } catch {
    // sessionStorage indisponível -- não há rascunho a descartar
  }
}

/**
 * Persiste o estado de um formulário em sessionStorage a cada mudança, sobrevivendo
 * a um redirect por sessão expirada (GDE-32) sem depender de nenhum wiring extra:
 * o token e o rascunho vivem em chaves separadas, então limpar o token (logout/401)
 * não apaga o rascunho, e a próxima montagem do formulário o restaura automaticamente.
 */
export function useFormDraft<T>(key: string, initialValue: T): [T, (value: T) => void, () => void] {
  const [value, setValueState] = useState<T>(() => readDraft(key, initialValue))

  function setValue(next: T) {
    setValueState(next)
    try {
      sessionStorage.setItem(DRAFT_PREFIX + key, JSON.stringify(next))
    } catch {
      // sessionStorage indisponível (ex.: modo privado) -- rascunho vira best-effort em memória
    }
  }

  function clearDraft() {
    sessionStorage.removeItem(DRAFT_PREFIX + key)
    setValueState(initialValue)
  }

  return [value, setValue, clearDraft]
}
