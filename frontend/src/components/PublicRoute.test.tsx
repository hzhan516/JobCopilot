import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import PublicRoute from './PublicRoute'

// Mock useAuth
const mockUseAuth = vi.hoisted(() => ({
  isAuthenticated: false,
}))

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth,
}))

describe('PublicRoute', () => {
  it('renders children when not authenticated', () => {
    mockUseAuth.isAuthenticated = false

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/" element={<div>Home</div>} />
          <Route
            path="/login"
            element={
              <PublicRoute>
                <div>Login Page</div>
              </PublicRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Login Page')).toBeInTheDocument()
  })

  it('redirects to home when authenticated without from state', () => {
    mockUseAuth.isAuthenticated = true

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/" element={<div>Home</div>} />
          <Route
            path="/login"
            element={
              <PublicRoute>
                <div>Login Page</div>
              </PublicRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Home')).toBeInTheDocument()
  })

  it('redirects to saved from path when authenticated', () => {
    mockUseAuth.isAuthenticated = true

    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/login',
            state: { from: { pathname: '/jobs' } },
          },
        ]}
        future={{ v7_startTransition: true }}
      >
        <Routes>
          <Route path="/jobs" element={<div>Jobs</div>} />
          <Route path="/" element={<div>Home</div>} />
          <Route
            path="/login"
            element={
              <PublicRoute>
                <div>Login Page</div>
              </PublicRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Jobs')).toBeInTheDocument()
  })

  it('avoids redirect loop by defaulting to home for /login from path', () => {
    mockUseAuth.isAuthenticated = true

    render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: '/login' } } }]}>
        <Routes>
          <Route path="/" element={<div>Home</div>} />
          <Route
            path="/login"
            element={
              <PublicRoute>
                <div>Login Page</div>
              </PublicRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    // Even with /login in from state, should not loop
    expect(screen.getByText('Home')).toBeInTheDocument()
  })

  it('falls back to home for unsafe external-like from path', () => {
    mockUseAuth.isAuthenticated = true

    render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: '//evil.com' } } }]}>
        <Routes>
          <Route path="/" element={<div>Home</div>} />
          <Route
            path="/login"
            element={
              <PublicRoute>
                <div>Login Page</div>
              </PublicRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Home')).toBeInTheDocument()
  })
})
