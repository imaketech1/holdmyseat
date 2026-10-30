import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api, rupees, showDate, showTime, type ShowSummary } from '../api';

export default function ShowsPage() {
  const [city, setCity] = useState('Hyderabad');
  const [query, setQuery] = useState('Hyderabad');
  const [shows, setShows] = useState<ShowSummary[] | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setShows(null);
    setError('');
    api<ShowSummary[]>(`/api/catalog/shows?city=${encodeURIComponent(query)}`)
      .then(setShows)
      .catch((e: Error) => setError(e.message));
  }, [query]);

  function search(e: FormEvent) {
    e.preventDefault();
    if (city.trim()) setQuery(city.trim());
  }

  const byDay = new Map<string, ShowSummary[]>();
  shows?.forEach((s) => {
    const day = showDate(s.startsAt);
    byDay.set(day, [...(byDay.get(day) ?? []), s]);
  });

  return (
    <div className="page">
      <div className="page-head">
        <h1>Showing in {query}</h1>
        <form className="city-form" onSubmit={search}>
          <label htmlFor="city">City</label>
          <input id="city" value={city} onChange={(e) => setCity(e.target.value)} />
          <button className="secondary">Change city</button>
        </form>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {!shows && !error && <p className="page-note">Loading showtimes</p>}
      {shows?.length === 0 && (
        <p className="page-note">
          {query.toLowerCase() === 'hyderabad'
            ? 'No upcoming shows here yet. An admin can add one, or restart catalog-service with a fresh database to get the demo show back.'
            : `No upcoming shows in ${query}. Try Hyderabad, where the demo show is playing.`}
        </p>
      )}

      {[...byDay.entries()].map(([day, list]) => (
        <section key={day} className="day">
          <h2>{day}</h2>
          <ul className="board">
            {list.map((s) => (
              <li key={s.id}>
                <Link to={`/shows/${s.id}`} className="board-row">
                  <span className="board-time">{showTime(s.startsAt)}</span>
                  <span className="board-title">
                    <strong>{s.title}</strong>
                    <span>{s.language}, {s.venueName}</span>
                  </span>
                  <span className="board-price">From {rupees(s.basePrice)}</span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
