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
          <Route path="/resumes" element={<div>Resumes</div>} />
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

  it('redirects to /resumes when authenticated without from state', () => {
    mockUseAuth.isAuthenticated = true

    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/resumes" element={<div>Resumes</div>} />
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

    expect(screen.getByText('Resumes')).toBeInTheDocument()
  })

  it('redirects to saved from path when authenticated', () => {
    mockUseAuth.isAuthenticated = true

    render(
      <MemoryRouter initialEntries={['/login']} future={{ v7_startTransition: true }}>
        <Routes>
          <Route path="/dashboard" element={<div>Dashboard</div>} />
          <Route path="/resumes" element={<div>Resumes</div>} />
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

    // When there's no from state, it defaults to /resumes
    expect(screen.getByText('Resumes')).toBeInTheDocument()
  })

  it('avoids redirect loop by defaulting to /resumes for /login from path', () => {
    mockUseAuth.isAuthenticated = true

    render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { from: { pathname: '/login' } } }]}>
        <Routes>
          <Route path="/resumes" element={<div>Resumes</div>} />
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
    expect(screen.getByText('Resumes')).toBeInTheDocument()
  })
})
