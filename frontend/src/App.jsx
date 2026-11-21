import { useEffect, useState } from 'react'
import { onAuthStateChanged, signInAnonymously, signInWithPopup, signOut } from 'firebase/auth'
import { auth, googleProvider } from './firebase'
import Chat from './Chat'
import './App.css'

export default function App() {
  const [user, setUser] = useState(undefined) // undefined = still checking

  useEffect(() => onAuthStateChanged(auth, setUser), [])

  if (user === undefined) return <div className="center muted">Loading…</div>
  if (!user) return <Login />
  return <Chat user={user} onSignOut={() => signOut(auth)} />
}

function Login() {
  const [error, setError] = useState('')

  async function run(signIn) {
    setError('')
    try {
      await signIn()
    } catch (e) {
      console.error('LOGIN ERROR:', e.code, e.message)
      if (e.code !== 'auth/popup-closed-by-user') setError(`Sign-in failed (${e.code})`)
    }
  }

  return (
    <div className="center">
      <h1>AI Chat</h1>
      <button className="google" onClick={() => run(() => signInWithPopup(auth, googleProvider))}>
        Continue with Google
      </button>
      <button className="guest" onClick={() => run(() => signInAnonymously(auth))}>
        Continue as guest
      </button>
      {error && <div className="error">{error}</div>}
    </div>
  )
}