import { auth } from './firebase'

async function request(path, options = {}) {
  const user = auth.currentUser
  if (!user) throw new Error('Not signed in')
  const token = await user.getIdToken()
  const res = await fetch(`/api${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  })
  if (!res.ok) throw new Error(`Request failed (${res.status})`)
  return res.status === 204 ? null : res.json()
}

export const listChats = () => request('/chats')
export const createChat = () => request('/chats', { method: 'POST' })
export const getMessages = (id) => request(`/chats/${id}/messages`)
export const sendMessage = (id, content) =>
  request(`/chats/${id}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
export const deleteChat = (id) => request(`/chats/${id}`, { method: 'DELETE' })