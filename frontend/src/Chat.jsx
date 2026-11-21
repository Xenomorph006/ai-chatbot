import { useEffect, useRef, useState } from 'react'
import * as api from './api'

export default function Chat({ user, onSignOut }) {
  const [chats, setChats] = useState([])
  const [activeId, setActiveId] = useState(null)
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const bottomRef = useRef(null)

  useEffect(() => {
    api.listChats()
      .then((list) => {
        setChats(list)
        if (list.length) setActiveId(list[0].id)
      })
      .catch(() => setError('Could not load chats.'))
  }, [])

  useEffect(() => {
    if (!activeId) {
      setMessages([])
      return
    }
    let cancelled = false
    api.getMessages(activeId)
      .then((m) => { if (!cancelled) setMessages(m) })
      .catch(() => setError('Could not load messages.'))
    return () => { cancelled = true }
  }, [activeId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, sending])

  async function send() {
    const text = input.trim()
    if (!text || sending) return
    setError('')
    setSending(true)
    setInput('')
    setMessages((m) => [...m, { id: `tmp-${Date.now()}`, role: 'user', content: text }])

    let chatId = activeId
    try {
      if (!chatId) chatId = (await api.createChat()).id
      const reply = await api.sendMessage(chatId, text)
      if (activeId) setMessages((m) => [...m, reply])
      else setActiveId(chatId)
    } catch {
      setError('Something went wrong. Please try again.')
      if (chatId === activeId) api.getMessages(chatId).then(setMessages)
      else if (chatId) setActiveId(chatId)
    } finally {
      setSending(false)
      api.listChats().then(setChats).catch(() => {})
    }
  }

  async function remove(id) {
    if (!window.confirm('Delete this chat?')) return
    try {
      await api.deleteChat(id)
      setChats((c) => c.filter((x) => x.id !== id))
      if (id === activeId) setActiveId(null)
    } catch {
      setError('Could not delete chat.')
    }
  }

  function onKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      send()
    }
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <button className="new-chat" onClick={() => setActiveId(null)} disabled={sending}>
          + New chat
        </button>
        <ul className="chat-list">
          {chats.map((c) => (
            <li key={c.id} className={c.id === activeId ? 'active' : ''}>
              <button className="chat-title" onClick={() => setActiveId(c.id)} disabled={sending}>
                {c.title}
              </button>
              <button
                className="chat-delete"
                onClick={() => remove(c.id)}
                disabled={sending}
                aria-label="Delete chat"
              >
                ×
              </button>
            </li>
          ))}
        </ul>
        <div className="account">
          <span className="email">{user.email ?? 'Guest'}</span>
          <button onClick={onSignOut}>Sign out</button>
        </div>
      </aside>

      <main className="main">
        <div className="messages">
          {messages.length === 0 && !sending && (
            <p className="empty">How can I help you today?</p>
          )}
          {messages.map((m) => (
            <div key={m.id} className={`msg ${m.role}`}>
              <div className="bubble">{m.content}</div>
            </div>
          ))}
          {sending && (
            <div className="msg assistant">
              <div className="bubble typing">Thinking…</div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {error && <div className="error">{error}</div>}

        <div className="composer">
          <textarea
            rows={1}
            value={input}
            placeholder="Message…"
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
          />
          <button onClick={send} disabled={sending || !input.trim()}>
            Send
          </button>
        </div>
      </main>
    </div>
  )
}