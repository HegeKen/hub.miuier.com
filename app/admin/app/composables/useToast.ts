export interface ToastItem {
  id: number
  type: 'success' | 'error' | 'info'
  message: string
}

const toasts = ref<ToastItem[]>([])
let seed = 0

export function useToast() {
  const push = (type: ToastItem['type'], message: string) => {
    const id = ++seed
    toasts.value.push({ id, type, message })
    setTimeout(() => dismiss(id), 4200)
  }

  const dismiss = (id: number) => {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  return { toasts, push, dismiss }
}
