import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, rupees, showDate, showTime, type Booking, type ShowDetail } from '../api';

const STATUS: Record<Booking['status'], string> = {
  HELD: 'Held, not paid',
  PAYMENT_PENDING: 'Paying',
  CONFIRMED: 'Booked',
  CANCELLED: 'Cancelled',
  FAILED: 'Payment declined',
  EXPIRED: 'Hold ran out',
};

export default function BookingsPage() {
  const [bookings, setBookings] = useState<Booking[] | null>(null);
  const [shows, setShows] = useState<Record<string, ShowDetail>>({});
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState('');

  const load = () =>
    api<Booking[]>('/api/bookings')
      .then(async (list) => {
        setBookings(list);
        const ids = [...new Set(list.map((b) => b.showId))];
        const details = await Promise.all(ids.map((id) => api<ShowDetail>(`/api/catalog/shows/${id}`).catch(() => null)));
        setShows(Object.fromEntries(details.filter((d): d is ShowDetail => !!d).map((d) => [d.id, d])));
      })
      .catch((e: Error) => setError(e.message));

  useEffect(() => {
    void load();
  }, []);

  async function cancel(b: Booking) {
    setBusyId(b.id);
    setError('');
    try {
      await api(`/api/bookings/${b.id}`, { method: 'DELETE' });
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not cancel the booking.');
    } finally {
      setBusyId('');
    }
  }

  return (
    <div className="page">
      <div className="page-head">
        <h1>My bookings</h1>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {!bookings && !error && <p className="page-note">Loading your bookings</p>}
      {bookings?.length === 0 && (
        <p className="page-note">
          You haven’t booked anything yet. <Link to="/">Find a show</Link>
        </p>
      )}

      <ul className="bookings">
        {bookings?.map((b) => {
          const show = shows[b.showId];
          const canCancel = b.status === 'CONFIRMED' || b.status === 'HELD';
          return (
            <li key={b.id} className={`booking status-${b.status.toLowerCase()}`}>
              <div>
                <Link to={`/shows/${b.showId}`} className="booking-title">{show?.title ?? 'Show'}</Link>
                {show && <p className="muted">{showDate(show.startsAt)}, {showTime(show.startsAt)}, {show.venueName}</p>}
              </div>
              <p className="booking-seats">{b.seats.join(', ')}</p>
              <p className="booking-amount">{rupees(b.amount)}</p>
              <p className="badge">{STATUS[b.status]}</p>
              <div className="booking-action">
                {canCancel && (
                  <button className="secondary" disabled={busyId === b.id} onClick={() => void cancel(b)}>
                    {b.status === 'CONFIRMED' ? 'Cancel and refund' : 'Release seats'}
                  </button>
                )}
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
