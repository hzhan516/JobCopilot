import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import ProtectedRoute from './ProtectedRoute'

// Mock useAuth
const mockUseAuth = vi.hoisted(() => ({
  isAuthenticated: false,
}))

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockUseAuth,
}))

function LocationDisplay() {
  const location = useLocation()
  return (
    <div>
      <div data-testid="location">{location.pathname}</div>
      <div data-testid="location-state">{location.state?.from?.pathname || 'no-state'}</div>
    </div>
  )
}

describe('ProtectedRoute', () => {
  it('redirects to login when not authenticated', () => {
    mockUseAuth.isAuthenticated = false

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <div>Dashboard</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Login Page')).toBeInTheDocument()
  })

  it('renders children when authenticated', () => {
    mockUseAuth.isAuthenticated = true

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <div>Dashboard</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })

  it('preserves original path in location state for redirect after login', () => {
    mockUseAuth.isAuthenticated = false

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/login" element={<LocationDisplay />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <div>Dashboard</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    // The route redirects to /login and stores the original path in state
    expect(screen.getByTestId('location')).toHaveTextContent('/login')

    // Verify location.state.from.pathname contains original path
    // We access the actual location via a custom test helper
    const locationData = screen.getByTestId('location-state')
    if (locationData) {
      expect(locationData).toHaveTextContent('/dashboard')
    }
  })
})
