import { Fragment } from 'react';
import { rupees, type SeatRow } from '../api';

export type SeatState = 'free' | 'selected' | 'mine' | 'held' | 'booked';

type Props = {
  layout: SeatRow[];
  basePrice: number;
  stateOf: (seatId: string) => SeatState;
  flashing: Set<string>;
  locked: boolean;
  onToggle: (seatId: string) => void;
};

export const rowPrice = (basePrice: number, row: SeatRow) => Math.round(basePrice * row.priceMultiplier * 100) / 100;

const titleCase = (s: string) => s.charAt(0) + s.slice(1).toLowerCase();

const describe: Record<SeatState, string> = {
  free: 'available',
  selected: 'selected by you',
  mine: 'held for you',
  held: 'held by someone else',
  booked: 'booked',
};

/** Splits a row into blocks with aisles, like a real auditorium: 3 | middle | 3 for rows of 12+. */
function blocks(count: number): number[][] {
  const all = Array.from({ length: count }, (_, i) => i + 1);
  if (count < 12) return [all];
  return [all.slice(0, 3), all.slice(3, count - 3), all.slice(count - 3)];
}

export default function SeatMap({ layout, basePrice, stateOf, flashing, locked, onToggle }: Props) {
  return (
    <div className="seatmap">
      <div className="screen" aria-hidden="true"><span>Screen this way</span></div>

      <div className="rows">
        {layout.map((row, i) => {
          const newCategory = i === 0 || layout[i - 1].category !== row.category;
          return (
            <Fragment key={row.row}>
              {newCategory && (
                <p className="category">
                  {titleCase(row.category)} <span>{rupees(rowPrice(basePrice, row))}</span>
                </p>
              )}
              <div className="row" role="group" aria-label={`Row ${row.row}`}>
                <span className="row-label" aria-hidden="true">{row.row}</span>
                {blocks(row.seats).map((block, b) => (
                  <div className="block" key={b}>
                    {block.map((n) => {
                      const id = `${row.row}${n}`;
                      const state = stateOf(id);
                      const clickable = !locked && (state === 'free' || state === 'selected');
                      return (
                        <button
                          key={id}
                          className={`seat seat-${state}${flashing.has(id) ? ' seat-flash' : ''}`}
                          disabled={!clickable}
                          aria-pressed={state === 'selected'}
                          aria-label={`Row ${row.row} seat ${n}, ${titleCase(row.category)}, ${rupees(rowPrice(basePrice, row))}, ${describe[state]}`}
                          onClick={() => onToggle(id)}
                        >
                          <span aria-hidden="true">{n}</span>
                        </button>
                      );
                    })}
                  </div>
                ))}
                <span className="row-label" aria-hidden="true">{row.row}</span>
              </div>
            </Fragment>
          );
        })}
      </div>

      <ul className="legend" aria-label="Seat colours">
        <li><span className="swatch seat-free" />Available</li>
        <li><span className="swatch seat-selected" />Your pick</li>
        <li><span className="swatch seat-held" />Someone is holding it</li>
        <li><span className="swatch seat-booked" />Booked</li>
      </ul>
    </div>
  );
}
