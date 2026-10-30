import type { ReactNode } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './auth';
import LoginPage from './pages/LoginPage';
import ShowsPage from './pages/ShowsPage';
import ShowPage from './pages/ShowPage';
import BookingsPage from './pages/BookingsPage';

function RequireAuth({ children }: { children: ReactNode }) {
  const { me, loading } = useAuth();
  const location = useLocation();
  if (loading) return <p className="page-note">Loading your account</p>;
  if (!me) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <>{children}</>;
}

export default function App() {
  const { me, logout } = useAuth();

  return (
    <>
      <a className="skip" href="#main">Skip to content</a>
      <header className="topbar">
        <Link to="/" className="wordmark">HoldMySeat</Link>
        {me && (
          <nav className="topnav" aria-label="Main">
            <NavLink to="/" end>Shows</NavLink>
            <NavLink to="/bookings">My bookings</NavLink>
            <span className="who" title={me.email}>{me.fullName}</span>
            <button className="link-button" onClick={() => void logout()}>Sign out</button>
          </nav>
        )}
      </header>
      <main id="main">
        <Routes>
          <Route path="/login" element={me ? <Navigate to="/" replace /> : <LoginPage />} />
          <Route path="/" element={<RequireAuth><ShowsPage /></RequireAuth>} />
          <Route path="/shows/:id" element={<RequireAuth><ShowPage /></RequireAuth>} />
          <Route path="/bookings" element={<RequireAuth><BookingsPage /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
