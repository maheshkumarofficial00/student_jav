/**
 * Time-interval helpers shared by the UI and the demo server.
 *
 * Intervals are { start: 'HH:mm', end: 'HH:mm' } on a single day.
 * Overlap rule: A overlaps B when A.start < B.end && B.start < A.end,
 * so back-to-back bookings (one ends at 10:00, the next starts at 10:00) are allowed.
 *
 * NOTE: these are used for display only. The backend is the source of truth
 * and re-validates every booking inside a transaction.
 */
(function () {
  const pad = (n) => String(n).padStart(2, '0');
  const toMin = (t) => { const [h, m] = t.split(':').map(Number); return h * 60 + m; };
  const toTime = (m) => pad(Math.floor(m / 60)) + ':' + pad(m % 60);

  const numeric = (list) =>
    list.map((i) => ({ start: toMin(i.start), end: toMin(i.end) }))
      .filter((i) => i.end > i.start)
      .sort((a, b) => a.start - b.start);

  const stringify = (list) => list.map((i) => ({ start: toTime(i.start), end: toTime(i.end) }));

  function mergeNumeric(list) {
    const out = [];
    for (const i of list) {
      const last = out[out.length - 1];
      if (last && i.start <= last.end) last.end = Math.max(last.end, i.end);
      else out.push({ ...i });
    }
    return out;
  }

  const merge = (list) => stringify(mergeNumeric(numeric(list)));

  function intersect(a, b) {
    const A = mergeNumeric(numeric(a));
    const B = mergeNumeric(numeric(b));
    const out = [];
    for (const x of A) {
      for (const y of B) {
        const s = Math.max(x.start, y.start);
        const e = Math.min(x.end, y.end);
        if (s < e) out.push({ start: s, end: e });
      }
    }
    return stringify(mergeNumeric(out.sort((p, q) => p.start - q.start)));
  }

  function subtract(windows, busy) {
    let out = mergeNumeric(numeric(windows));
    for (const b of mergeNumeric(numeric(busy))) {
      const next = [];
      for (const w of out) {
        if (b.end <= w.start || b.start >= w.end) { next.push(w); continue; }
        if (b.start > w.start) next.push({ start: w.start, end: b.start });
        if (b.end < w.end) next.push({ start: b.end, end: w.end });
      }
      out = next;
    }
    return stringify(out);
  }

  const overlaps = (a, b) => toMin(a.start) < toMin(b.end) && toMin(b.start) < toMin(a.end);

  /** Start times inside a free window that leave room for `duration` minutes. */
  function startTimes(win, duration, step = 30) {
    const s = toMin(win.start);
    const e = toMin(win.end);
    const out = [];
    for (let t = s; t + duration <= e; t = t % step === 0 ? t + step : Math.ceil(t / step) * step) {
      out.push(toTime(t));
    }
    return out;
  }

  const addMinutes = (time, mins) => toTime(toMin(time) + mins);

  // ---- dates (local, YYYY-MM-DD) ----
  const dstr = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const todayStr = () => dstr(new Date());
  const parseDate = (s) => new Date(s + 'T00:00:00');
  const addDays = (s, n) => { const d = parseDate(s); d.setDate(d.getDate() + n); return dstr(d); };
  const daysBetween = (a, b) => Math.round((parseDate(b) - parseDate(a)) / 86400000);
  const fmtDay = (s) => parseDate(s).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
  const isFuture = (date, start) => new Date(`${date}T${start}:00`) > new Date();

  window.Slots = {
    toMin, toTime, merge, intersect, subtract, overlaps, startTimes, addMinutes,
    dstr, todayStr, parseDate, addDays, daysBetween, fmtDay, isFuture
  };
})();