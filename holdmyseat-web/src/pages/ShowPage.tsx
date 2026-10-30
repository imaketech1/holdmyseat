import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, ApiError, rupees, showDate, showTime, type Availability, type Booking, type ShowDetail } from '../api';
import SeatMap, { rowPrice, type SeatState } from '../components/SeatMap';
import { useSeatEvents, type SeatDelta } from '../useSeatEvents';

const MAX_SEATS = 10;
const ACTIVE = ['HELD', 'PAYMENT_PENDING'];
const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

function useCountdown(until: string | undefined) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    if (!until) return;
    setNow(Date.now());
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, [until]);
  return until ? Math.max(0, Math.floor((new Date(until).getTime() - now) / 1000)) : null;
}

const clock = (s: number) => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
const seatList = (seats: string[]) => [...seats].sort((a, b) => a.localeCompare(b, undefined, { numeric: true })).join(', ');

type FeedItem = { key: number; text: string; at: Date };

export default function ShowPage() {
  const { id } = useParams();
  const [show, setShow] = useState<ShowDetail | null>(null);
  const [held, setHeld] = useState<Set<string>>(new Set());
  const [booked, setBooked] = useState<Set<string>>(new Set());
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [booking, setBooking] = useState<Booking | null>(null);
  const [flashing, setFlashing] = useState<Set<string>>(new Set());
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  const bookingRef = useRef(booking);
  bookingRef.current = booking;
  const selectedRef = useRef(selected);
  selectedRef.current = selected;
  // Live events for these seats can arrive before the REST response.
  const pendingRef = useRef<Set<string>>(new Set());

  const loadAvailability = useCallback(async () => {
    const a = await api<Availability>(`/api/bookings/shows/${id}/availability`);
    setHeld(new Set(a.held));
    setBooked(new Set(a.booked));
  }, [id]);

  useEffect(() => {
    setError('');
    api<ShowDetail>(`/api/catalog/shows/${id}`).then(setShow).catch((e: Error) => setError(e.message));
    loadAvailability().catch((e: Error) => setError(e.message));
    // Resume an existing hold on this show (e.g. after reload).
    api<Booking[]>('/api/bookings')
      .then((list) => setBooking(list.find((b) => b.showId === id && ACTIVE.includes(b.status) && new Date(b.expiresAt) > new Date()) ?? null))
      .catch(() => {});
  }, [id, loadAvailability]);

  const onDelta = useCallback((d: SeatDelta) => {
    setHeld((prev) => {
      const next = new Set(prev);
      d.seatIds.forEach((s) => (d.state === 'HELD' ? next.add(s) : next.delete(s)));
      return next;
    });
    setBooked((prev) => {
      const next = new Set(prev);
      d.seatIds.forEach((s) => (d.state === 'BOOKED' ? next.add(s) : d.state === 'AVAILABLE' && next.delete(s)));
      return next;
    });

    const mine = new Set([...(bookingRef.current?.seats ?? []), ...pendingRef.current]);
    const others = d.seatIds.filter((s) => !mine.has(s));
    if (others.length === 0) return;

    setFlashing((prev) => new Set([...prev, ...others]));
    setTimeout(() => setFlashing((prev) => new Set([...prev].filter((s) => !others.includes(s)))), 1600);

    const text = {
      HELD: `${seatList(others)} just held by someone`,
      BOOKED: `${seatList(others)} booked`,
      AVAILABLE: `${seatList(others)} back on sale`,
    }[d.state];
    setFeed((prev) => [{ key: Date.now() + Math.random(), text, at: new Date() }, ...prev].slice(0, 6));

    if (d.state !== 'AVAILABLE') {
      const lost = others.filter((s) => selectedRef.current.has(s));
      if (lost.length) {
        setSelected((prev) => new Set([...prev].filter((s) => !lost.includes(s))));
        setNotice(`${seatList(lost)} was just taken by someone else, so it’s been removed from your pick.`);
      }
    }
  }, []);

  const live = useSeatEvents(id, onDelta, () => void loadAvailability().catch(() => {}));

  const remaining = useCountdown(booking && ACTIVE.includes(booking.status) ? booking.expiresAt : undefined);

  useEffect(() => {
    if (remaining === 0 && booking?.status === 'HELD') {
      setBooking({ ...booking, status: 'EXPIRED' });
      void loadAvailability().catch(() => {});
    }
  }, [remaining, booking, loadAvailability]);

  const activeBooking = booking && (ACTIVE.includes(booking.status) || booking.status === 'CONFIRMED') ? booking : null;
  const mySeats = new Set(activeBooking?.seats ?? []);

  const stateOf = (seat: string): SeatState => {
    if (mySeats.has(seat)) return 'mine';
    if (booked.has(seat)) return 'booked';
    if (held.has(seat)) return 'held';
    if (selected.has(seat)) return 'selected';
    return 'free';
  };

  function toggle(seat: string) {
    setNotice('');
    setError('');
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(seat)) next.delete(seat);
      else if (next.size >= MAX_SEATS) setNotice(`You can hold up to ${MAX_SEATS} seats at a time.`);
      else next.add(seat);
      return next;
    });
  }

  async function hold() {
    setBusy(true);
    setError('');
    setNotice('');
    pendingRef.current = new Set(selected);
    try {
      const b = await api<Booking>('/api/holds', { method: 'POST', body: { showId: id, seatIds: [...selected] } });
      bookingRef.current = b;
      setBooking(b);
      setSelected(new Set());
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        setError('Someone grabbed one of those seats a moment before you. The map is up to date now, so pick again.');
        setSelected(new Set());
        void loadAvailability().catch(() => {});
      } else setError(e instanceof Error ? e.message : 'Could not hold the seats.');
    } finally {
      pendingRef.current = new Set();
      setBusy(false);
    }
  }

  async function pay() {
    if (!booking) return;
    setBusy(true);
    setError('');
    try {
      let b = await api<Booking>(`/api/bookings/${booking.id}/checkout`, { method: 'POST' });
      setBooking(b);
      // Payment is async via Kafka; poll until the saga settles.
      for (let i = 0; i < 20 && b.status === 'PAYMENT_PENDING'; i++) {
        await sleep(1000);
        b = await api<Booking>(`/api/bookings/${booking.id}`);
        setBooking(b);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Payment could not start.');
    } finally {
      setBusy(false);
    }
  }

  async function release() {
    if (!booking) return;
    setBusy(true);
    try {
      await api(`/api/bookings/${booking.id}`, { method: 'DELETE' });
      setBooking(null);
      setNotice('Seats released. Anyone can book them now.');
      await loadAvailability();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not release the seats.');
    } finally {
      setBusy(false);
    }
  }

  if (!show) {
    return <div className="page">{error ? <p className="error" role="alert">{error}</p> : <p className="page-note">Loading the seat map</p>}</div>;
  }

  const priceOf = (seat: string) => {
    const row = show.layout.find((r) => seat.startsWith(r.row) && /^\d+$/.test(seat.slice(r.row.length)));
    return row ? rowPrice(show.basePrice, row) : 0;
  };
  const pickTotal = [...selected].reduce((sum, s) => sum + priceOf(s), 0);

  return (
    <div className="page show-page">
      <div className="show-head">
        <Link to="/" className="back">All shows</Link>
        <h1>{show.title}</h1>
        <p>
          {showDate(show.startsAt)}, {showTime(show.startsAt)}. {show.venueName}, {show.screenName}. {show.language}.
        </p>
      </div>

      <div className="show-layout">
        <section className="map-area" aria-label="Seat map">
          <p className={`live ${live ? 'is-live' : ''}`} role="status">
            <span className="live-dot" aria-hidden="true" />
            {live ? 'Live: seats update as others book' : 'Connecting to live updates'}
          </p>
          <SeatMap
            layout={show.layout}
            basePrice={show.basePrice}
            stateOf={stateOf}
            flashing={flashing}
            locked={!!activeBooking}
            onToggle={toggle}
          />
        </section>

        <aside className="ticket" aria-label="Your seats">
          {!activeBooking && booking?.status !== 'FAILED' && booking?.status !== 'EXPIRED' && (
            <>
              <h2>Your seats</h2>
              {selected.size === 0 ? (
                <p className="muted">Tap seats on the map to pick them. You can hold up to {MAX_SEATS} at once.</p>
              ) : (
                <>
                  <p className="ticket-seats">{seatList([...selected])}</p>
                  <p className="ticket-total"><span>Total</span>{rupees(pickTotal)}</p>
                </>
              )}
              <button className="primary" disabled={busy || selected.size === 0} onClick={() => void hold()}>
                {busy ? 'Holding seats' : selected.size <= 1 ? 'Hold seat' : `Hold ${selected.size} seats`}
              </button>
            </>
          )}

          {booking?.status === 'HELD' && remaining !== null && (
            <>
              <h2>Held for you</h2>
              <p className="countdown" aria-live="off"><strong>{clock(remaining)}</strong> left to pay</p>
              <p className="ticket-seats">{seatList(booking.seats)}</p>
              <p className="ticket-total"><span>Total</span>{rupees(booking.amount)}</p>
              <button className="primary" disabled={busy} onClick={() => void pay()}>Pay {rupees(booking.amount)}</button>
              <button className="secondary" disabled={busy} onClick={() => void release()}>Release seats</button>
            </>
          )}

          {booking?.status === 'PAYMENT_PENDING' && (
            <>
              <h2>Processing payment</h2>
              <p className="muted">Your seats stay held while the payment goes through. This takes a few seconds.</p>
              <p className="ticket-seats">{seatList(booking.seats)}</p>
              <p className="ticket-total"><span>Total</span>{rupees(booking.amount)}</p>
            </>
          )}

          {booking?.status === 'CONFIRMED' && (
            <>
              <h2>You’re booked</h2>
              <p className="ticket-seats">{seatList(booking.seats)}</p>
              <p className="ticket-total"><span>Paid</span>{rupees(booking.amount)}</p>
              <p className="muted">A confirmation is on its way to you.</p>
              <Link className="primary" to="/bookings">View my bookings</Link>
              <button className="secondary" onClick={() => setBooking(null)}>Book more seats</button>
            </>
          )}

          {booking?.status === 'FAILED' && (
            <>
              <h2>Payment declined</h2>
              <p className="muted">The card was declined, so {seatList(booking.seats)} went back on sale. Nothing was charged.</p>
              <button className="primary" onClick={() => setBooking(null)}>Pick seats again</button>
            </>
          )}

          {booking?.status === 'EXPIRED' && (
            <>
              <h2>Hold ran out</h2>
              <p className="muted">The seven minutes passed before payment, so {seatList(booking.seats)} went back on sale.</p>
              <button className="primary" onClick={() => setBooking(null)}>Pick seats again</button>
            </>
          )}

          {notice && <p className="notice" role="status">{notice}</p>}
          {error && <p className="error" role="alert">{error}</p>}

          {feed.length > 0 && (
            <div className="feed">
              <h3>Happening now</h3>
              <ul aria-live="polite">
                {feed.map((f) => (
                  <li key={f.key}>
                    {f.text}
                    <time>{f.at.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit', second: '2-digit' })}</time>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
