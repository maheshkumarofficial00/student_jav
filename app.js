/**
 * Smart Faculty and Resource Booking System – university booking (vanilla JS, Fetch API).
 * All dynamic text is inserted with textContent via h(), never innerHTML, to avoid XSS.
 */
(() => {
  'use strict';

  const cfg = window.APP_CONFIG;
  const S = window.Slots;
  const $ = (sel, root = document) => root.querySelector(sel);

  // ===================================================================
  // Tiny DOM helper
  // ===================================================================
  function h(tag, props, ...kids) {
    const el = document.createElement(tag);
    let value;
    for (const [k, v] of Object.entries(props || {})) {
      if (v === null || v === undefined || v === false) continue;
      if (k === 'class') el.className = v;
      else if (k === 'value') value = v;
      else if (/^on[A-Z]/.test(k)) el.addEventListener(k.slice(2).toLowerCase(), v);
      else if (v === true) el.setAttribute(k, '');
      else el.setAttribute(k, v);
    }
    for (const kid of kids.flat(Infinity)) {
      if (kid === null || kid === undefined || kid === false) continue;
      el.append(kid.nodeType ? kid : document.createTextNode(String(kid)));
    }
    if (value !== undefined) el.value = value;
    return el;
  }

  let idCounter = 0;
  function field(label, control, hint) {
    const id = 'f' + (++idCounter);
    control.id = id;
    return h('div', { class: 'field' }, h('label', { for: id }, label), control, hint && h('p', { class: 'hint' }, hint));
  }
  const select = (opts, value, onChange) =>
    h('select', { value: value ?? '', onChange: (e) => onChange(e.target.value) }, opts.map(([v, l]) => h('option', { value: v }, l)));

  // ---- Building / floor / room helpers (the room lists come from the API with `block` and `floor`) ----
  const BUILDINGS = ['SR Block', 'V Block', 'CV Raman Block'];
  const floorsOf = (rooms, block) => [...new Set(rooms.filter((r) => r.block === block && r.floor != null).map((r) => Number(r.floor)))].sort((a, b) => a - b);
  const roomsIn = (rooms, block, floor) => rooms.filter((r) => (!block || r.block === block) && (!floor || String(r.floor) === String(floor)));
  function setOptions(sel, opts, value) {
    sel.replaceChildren(...opts.map(([v, l]) => h('option', { value: v }, l)));
    sel.value = value ?? '';
  }

  // ===================================================================
  // Constants and formatting
  // ===================================================================
  const ROLE_LABEL = { STUDENT: 'Student', FACULTY: 'Faculty', ADMIN: 'Administrator' };
  const ROOM_TYPES = { CLASSROOM: 'Classroom', MEETING_ROOM: 'Meeting room', SEMINAR_HALL: 'Seminar hall' };
  const STATUS = {
    PENDING: ['Awaiting faculty', 'warn'],
    CONFIRMED: ['Confirmed', 'ok'],
    DECLINED: ['Declined', 'bad'],
    CANCELLED: ['Cancelled', 'muted']
  };
  const DAY_NAMES = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  const WEEK_ORDER = [1, 2, 3, 4, 5, 6, 0];

  const NAV = {
    STUDENT: [['find', 'Find a time'], ['faculty', 'Faculty'], ['rooms', 'Rooms & resources'], ['mine', 'My bookings']],
    FACULTY: [['requests', 'Appointments'], ['schedule', 'My schedule'], ['rooms', 'Rooms & resources'], ['mine', 'My reservations']],
    ADMIN: [['overview', 'Overview'], ['users', 'Users & faculty'], ['rooms-admin', 'Rooms'], ['resources-admin', 'Resources'],
      ['schedules', 'Schedules'], ['bookings', 'All bookings']]
  };

  const state = { user: null };
  const range = (item) => `${item.start}–${item.end}`;
  const when = (item) => `${S.fmtDay(item.date)}, ${range(item)}`;
  const badge = (status) => { const [label, tone] = STATUS[status] || [status, '']; return h('span', { class: 'badge ' + tone }, label); };

  // ===================================================================
  // UI primitives: toast, modal, confirm, states
  // ===================================================================
  function toast(message, type = 'info') {
    const t = h('div', { class: 'toast ' + (type === 'error' ? 'error' : ''), role: type === 'error' ? 'alert' : 'status' }, message);
    $('#toasts').append(t);
    setTimeout(() => t.remove(), type === 'error' ? 7000 : 4000);
  }

  function openModal(title, content, { wide = false } = {}) {
    const dlg = $('#modal');
    dlg.className = wide ? 'wide' : '';
    dlg.replaceChildren(
      h('div', { class: 'modal-head' },
        h('h2', { id: 'modal-title' }, title),
        h('button', { class: 'icon-btn', type: 'button', 'aria-label': 'Close', onClick: () => dlg.close() }, '×')),
      h('div', { class: 'modal-body' }, content));
    dlg.showModal();
    return dlg;
  }

  function confirmDialog(text, confirmLabel, danger = true) {
    return new Promise((resolve) => {
      let accepted = false;
      const dlg = openModal('Please confirm', h('div', {},
        h('p', {}, text),
        h('div', { class: 'actions' },
          h('button', { class: 'btn secondary', type: 'button', onClick: () => dlg.close() }, 'Keep it'),
          h('button', { class: 'btn ' + (danger ? 'danger' : ''), type: 'button', onClick: () => { accepted = true; dlg.close(); } }, confirmLabel))));
      dlg.addEventListener('close', () => resolve(accepted), { once: true });
    });
  }

  const loadingEl = (text = 'Loading…') => h('p', { class: 'loading', role: 'status' }, text);
  const emptyState = (title, text, action) => h('div', { class: 'empty' }, h('h3', {}, title), text && h('p', {}, text), action);
  const errorBox = (e) => h('div', { class: 'panel' }, h('div', { class: 'form-error', role: 'alert' }, e.message || 'Something went wrong.'));
  const pageHead = (title, sub) => h('header', { class: 'page-head' }, h('h1', {}, title), sub && h('p', {}, sub));

  async function run(button, fn) {
    button.disabled = true;
    try { return await fn(); } finally { button.disabled = false; }
  }

  // ===================================================================
  // Lanes – shows several calendars on one time axis, with the overlap highlighted
  // ===================================================================
  function lanes({ rows, selected, dayStart = cfg.DAY_START, dayEnd = cfg.DAY_END, legend = true }) {
    const span = dayEnd - dayStart;
    const pos = (m) => Math.min(100, Math.max(0, ((m - dayStart) / span) * 100));
    const seg = (cls, item, title) => {
      const s = pos(S.toMin(item.start));
      const e = pos(S.toMin(item.end));
      if (e <= s) return null;
      return h('span', { class: 'seg ' + cls, style: `left:${s}%;width:${e - s}%`, title });
    };

    const ticks = [];
    for (let m = Math.ceil(dayStart / 120) * 120; m <= dayEnd; m += 120) {
      ticks.push(h('span', { style: `left:${pos(m)}%` }, S.toTime(m)));
    }

    const kinds = new Set();
    const rowEls = rows.map((row) => {
      kinds.add(row.kind);
      const track = h('div', { class: 'lane-track' },
        row.busy.map((b) => seg('busy', b, `${b.label || 'Busy'} ${range(b)}`)),
        row.free.map((f) => seg('free ' + row.kind, f, `Free ${range(f)}`)),
        selected && h('span', {
          class: 'sel',
          style: `left:${pos(S.toMin(selected.start))}%;width:${pos(S.toMin(selected.end)) - pos(S.toMin(selected.start))}%`
        }));
      return h('div', { class: 'lane ' + row.kind }, h('span', { class: 'lane-label', title: row.label }, row.label), track);
    });

    const names = { faculty: 'Faculty free', room: 'Room free', both: 'Both free' };
    return h('div', {},
      h('div', { class: 'lanes' },
        rowEls,
        h('div', { class: 'axis', 'aria-hidden': 'true' }, h('span'), h('div', { class: 'axis-track' }, ticks))),
      legend && h('div', { class: 'legend' },
        [...kinds].map((k) => h('span', {}, h('i', { style: `background:var(--${k === 'both' ? 'overlap' : k})` }), names[k] || 'Free')),
        h('span', {}, h('i', { style: 'background:repeating-linear-gradient(135deg,#B8C3D9 0 3px,#DCE3F0 3px 6px)' }), 'Busy')));
  }

  // ===================================================================
  // Routing and shell
  // ===================================================================
  const VIEWS = {};
  const parseHash = () => {
    const raw = location.hash.replace(/^#\/?/, '');
    const [name, qs] = raw.split('?');
    return { name: name || '', params: Object.fromEntries(new URLSearchParams(qs || '')) };
  };
  function navigate(hash) {
    if (location.hash === hash) route(); else location.hash = hash;
  }

  async function route() {
    if (!state.user) { renderLogin(); return; }
    const { name, params } = parseHash();
    const allowed = NAV[state.user.role].map((n) => n[0]);
    const target = allowed.includes(name) ? name : allowed[0];
    if (target !== name) history.replaceState(null, '', '#/' + target);

    if (!$('.shell')) renderShell();
    document.querySelectorAll('.nav a').forEach((a) => {
      if (a.dataset.route === target) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    });
    const label = NAV[state.user.role].find((n) => n[0] === target)[1];
    document.title = `${label} – ${cfg.BRAND}`;

    const host = $('#view');
    host.replaceChildren(loadingEl());
    try {
      const el = await VIEWS[target](params);
      if (parseHash().name === target || name === '') host.replaceChildren(el);
    } catch (e) {
      host.replaceChildren(errorBox(e));
    }
  }

  function renderShell() {
    const user = state.user;
    $('#app').replaceChildren(h('div', { class: 'shell' },
      h('aside', { class: 'side' },
        h('div', { class: 'brand' }, cfg.BRAND),
        h('nav', { class: 'nav', 'aria-label': 'Main' },
          NAV[user.role].map(([id, label]) => h('a', { href: '#/' + id, 'data-route': id }, label))),
        h('div', { class: 'me' },
          h('strong', {}, user.name),
          h('span', {}, ROLE_LABEL[user.role]),
          h('button', { class: 'link-btn', type: 'button', onClick: logout }, 'Sign out'))),
      h('main', { class: 'main', id: 'main', tabindex: '-1' },
        cfg.USE_MOCK && h('div', { class: 'demo-banner' },
          'Demo mode: sample data stored in this browser only. ',
          h('button', { class: 'link-btn', type: 'button', onClick: () => { MockServer.reset(); toast('Demo data reset.'); route(); } }, 'Reset demo data')),
        h('div', { id: 'view' }))));
  }

  function logout() {
    Api.setToken(null);
    state.user = null;
    history.replaceState(null, '', location.pathname + location.search);
    renderLogin();
  }

  // ===================================================================
  // Login
  // ===================================================================
  function startSession(res) {
    Api.setToken(res.token);
    state.user = res.user;
    history.replaceState(null, '', location.pathname + location.search + '#/');
    route();
  }

  function renderLogin(mode = 'login') {
    const isRegister = mode === 'register';

    const fullName = h('input', { type: 'text', autocomplete: 'name', required: true, maxlength: 80 });
    const email = h('input', { type: 'email', autocomplete: 'username', placeholder: 'yourname@srmap.edu.in', required: true });
    const password = h('input', { type: 'password', autocomplete: isRegister ? 'new-password' : 'current-password', minlength: 8, required: true });
    const confirmPw = h('input', { type: 'password', autocomplete: 'new-password', required: true });
    const errBox = h('div', { class: 'form-error', role: 'alert', hidden: true });
    const submit = h('button', { class: 'btn', type: 'submit' }, isRegister ? 'Create account' : 'Sign in');

    const form = h('form', {
      class: 'form',
      onSubmit: async (e) => {
        e.preventDefault();
        errBox.hidden = true;

        const fail = (message) => {
          errBox.textContent = message;
          errBox.hidden = false;
        };

        const emailValue = email.value.trim().toLowerCase();
        const passwordValue = password.value;

        if (!/^[a-zA-Z0-9._%+-]+@srmap\.edu\.in$/.test(emailValue)) {
          return fail('Enter a valid university email ending in @srmap.edu.in.');
        }
        if (passwordValue.length < 8) return fail('Password must contain at least 8 characters.');
        if (!/[A-Z]/.test(passwordValue)) return fail('Password must contain an uppercase letter.');
        if (!/[a-z]/.test(passwordValue)) return fail('Password must contain a lowercase letter.');
        if (!/[0-9]/.test(passwordValue)) return fail('Password must contain a number from 0 to 9.');
        if (!/[^A-Za-z0-9]/.test(passwordValue)) return fail('Password must contain a special character.');

        if (isRegister) {
          if (fullName.value.trim().length < 2) return fail('Enter your full name.');
          if (passwordValue !== confirmPw.value) return fail('The two passwords do not match.');
        }

        await run(submit, async () => {
          try {
            const res = isRegister
              ? await Api.post('/auth/register', { name: fullName.value.trim(), email: emailValue, password: passwordValue })
              : await Api.post('/auth/login', { email: emailValue, password: passwordValue });
            startSession(res);
          } catch (err) {
            fail(err.message);
          }
        });
      }
    },
      isRegister
        ? [
            field('Full name', fullName),
            field('University email', email, 'Use your @srmap.edu.in email.'),
            field('Password', password, 'Minimum 8 characters, uppercase, lowercase, number and special character.'),
            field('Confirm password', confirmPw),
            errBox,
            submit
          ]
        : [
            field('University email', email, 'Use your @srmap.edu.in email.'),
            field('Password', password, 'Must include uppercase, lowercase, number and special character.'),
            errBox,
            submit
          ]
    );

    const switchLink = h('p', { class: 'hint' },
      isRegister ? 'Already have an account? ' : 'New student? ',
      h('button', { class: 'link-btn', type: 'button', onClick: () => renderLogin(isRegister ? 'login' : 'register') },
        isRegister ? 'Sign in' : 'Create an account'));

    const demo = cfg.USE_MOCK && !isRegister && h('div', { class: 'field' },
      h('span', { class: 'label' }, 'Demo accounts (password: Campus@123)'),
      h('div', { class: 'demo-accounts' },
        [
          ['Student', 'student1@srmap.edu.in'],
          ['Faculty', 'faculty1@srmap.edu.in'],
          ['Admin', 'admin@srmap.edu.in']
        ].map(([label, mail]) =>
          h('button', {
            class: 'chip', type: 'button',
            onClick: () => { email.value = mail; password.value = 'Campus@123'; password.focus(); }
          }, label))));

    const sample = lanes({
      rows: [
        {
          label: 'Dr. Kumar', kind: 'faculty',
          free: [{ start: '09:00', end: '13:00' }, { start: '14:00', end: '17:00' }],
          busy: [{ start: '11:00', end: '12:00', label: 'Meeting' }]
        },
        {
          label: 'Room M1', kind: 'room',
          free: [{ start: '08:00', end: '10:30' }, { start: '12:00', end: '20:00' }],
          busy: [{ start: '10:30', end: '12:00', label: 'Reserved' }]
        },
        {
          label: 'Both free', kind: 'both',
          free: [{ start: '09:00', end: '10:30' }, { start: '14:00', end: '17:00' }],
          busy: []
        }
      ],
      selected: { start: '14:00', end: '15:00' }
    });

    $('#app').replaceChildren(
      h('div', { class: 'login' },
        h('section', { class: 'login-art' },
          h('div', { class: 'brand' }, cfg.APP_NAME),
          h('h1', {}, 'Meet when the professor and the room are both free.'),
          h('p', {}, 'Choose a faculty member and the system compares their calendar with every suitable room, then shows only the times that work for both. Book appointments, reserve rooms and share academic resources in one place.'),
          h('div', { 'aria-hidden': 'true' }, sample)),
        h('section', { class: 'login-form', id: 'main' },
          h('h2', {}, isRegister ? 'Create a student account' : 'Sign in'),
          form,
          switchLink,
          demo)));

    document.title = (isRegister ? 'Create account' : 'Sign in') + ' – ' + cfg.BRAND;
  }

  // ===================================================================
  // Reusable: availability dialog (faculty / room / resource)
  // ===================================================================
  async function openAvailability({ kind, entity }) {
    const path = { faculty: 'faculty', room: 'rooms', resource: 'resources' }[kind];
    const today = S.todayStr();
    const st = { date: today, duration: 30, start: null, free: [], busy: [], extras: new Set() };

    const laneHost = h('div', { 'aria-live': 'polite' });
    const chipHost = h('div', { class: 'chips' });
    const purpose = h('textarea', { maxlength: 300, placeholder: kind === 'faculty' ? 'What would you like to discuss?' : 'What is the reservation for?' });
    const errBox = h('div', { class: 'form-error', role: 'alert', hidden: true });
    const submit = h('button', { class: 'btn', type: 'button', disabled: true }, kind === 'faculty' ? 'Request appointment' : 'Reserve');

    const dateInput = h('input', {
      type: 'date', min: today, value: st.date,
      onChange: (e) => { st.date = e.target.value || today; st.start = null; load(); }
    });
    const durationSel = select([[15, '15 minutes'], [30, '30 minutes'], [45, '45 minutes'], [60, '1 hour'], [90, '1 hour 30 minutes'], [120, '2 hours']], st.duration,
      (v) => { st.duration = Number(v); st.start = null; draw(); });

    let extrasBlock = null;
    if (kind === 'room') {
      try {
        const resources = await Api.get('/resources');
        if (resources.length) {
          extrasBlock = h('div', { class: 'field' },
            h('span', { class: 'label' }, 'Add shared resources (optional)'),
            h('div', { class: 'res-list' }, resources.map((r) =>
              h('label', { class: 'check' },
                h('input', { type: 'checkbox', onChange: (e) => { if (e.target.checked) st.extras.add(r.id); else st.extras.delete(r.id); } }),
                r.name))),
            h('p', { class: 'hint' }, 'Resource availability is checked when you reserve.'));
        }
      } catch (_) { /* resources are optional */ }
    }

    function draw() {
      const selected = st.start ? { start: st.start, end: S.addMinutes(st.start, st.duration) } : null;
      const laneKind = kind === 'faculty' ? 'faculty' : 'room';
      laneHost.replaceChildren(lanes({ rows: [{ label: entity.name, kind: laneKind, free: st.free, busy: st.busy }], selected }));

      const starts = st.free.flatMap((w) => S.startTimes(w, st.duration, cfg.SLOT_STEP));
      chipHost.replaceChildren(...(starts.length
        ? starts.map((t) => h('button', {
          class: 'chip', type: 'button', 'aria-pressed': String(st.start === t),
          'aria-label': `${t} to ${S.addMinutes(t, st.duration)}`,
          onClick: () => { st.start = t; draw(); }
        }, t))
        : [h('p', { class: 'hint' }, st.free.length ? 'No free gap is long enough. Try a shorter duration.' : 'Nothing is free on this day. Try another date.')]));
      submit.disabled = !st.start;
    }

    async function load() {
      laneHost.replaceChildren(loadingEl());
      chipHost.replaceChildren();
      try {
        const a = await Api.get(`/${path}/${entity.id}/availability?date=${st.date}`);
        st.free = a.free; st.busy = a.busy;
        draw();
      } catch (e) {
        laneHost.replaceChildren(h('div', { class: 'form-error', role: 'alert' }, e.message));
      }
    }

    submit.addEventListener('click', () => run(submit, async () => {
      errBox.hidden = true;
      const end = S.addMinutes(st.start, st.duration);
      const text = purpose.value.trim();
      if (text.length < 3) { errBox.textContent = 'Add a short purpose so the other person knows what this is for.'; errBox.hidden = false; return; }
      try {
        if (kind === 'faculty') {
          await Api.post('/appointments', { facultyId: entity.id, roomId: null, date: st.date, start: st.start, end, purpose: text });
          toast(`Request sent to ${entity.name}. You'll see it under My bookings.`);
        } else {
          await Api.post('/bookings', {
            roomId: kind === 'room' ? entity.id : null,
            resourceIds: kind === 'resource' ? [entity.id] : [...st.extras],
            date: st.date, start: st.start, end, purpose: text
          });
          toast(`${entity.name} reserved for ${when({ date: st.date, start: st.start, end })}.`);
        }
        $('#modal').close();
        navigate('#/mine');
      } catch (e) {
        errBox.textContent = e.message; errBox.hidden = false;
        if (e.status === 409) { st.start = null; load(); }
      }
    }));

    const intro = kind === 'faculty'
      ? h('p', { class: 'hint' }, `${entity.title || ''} ${entity.department ? '· ' + entity.department : ''} ${entity.office ? '· Office ' + entity.office : ''}`.trim())
      : kind === 'room'
        ? h('p', { class: 'hint' }, `${entity.building} · ${ROOM_TYPES[entity.type] || entity.type} · seats ${entity.capacity}`)
        : h('p', { class: 'hint' }, `${entity.category || ''} ${entity.description ? '· ' + entity.description : ''}`.trim());

    const body = h('div', { class: 'form' },
      intro,
      h('div', { class: 'form-grid' }, field('Date', dateInput), field('Length', durationSel)),
      laneHost,
      h('div', { class: 'field' }, h('span', { class: 'label' }, 'Start time'), chipHost),
      extrasBlock,
      field('Purpose', purpose),
      errBox,
      h('div', { class: 'actions' },
        kind === 'faculty' && h('a', { class: 'btn secondary', href: '#/find?faculty=' + entity.id, onClick: () => $('#modal').close() }, 'Also need a room? Find a time'),
        h('button', { class: 'btn secondary', type: 'button', onClick: () => $('#modal').close() }, 'Close'),
        submit));

    openModal(kind === 'faculty' ? `Book ${entity.name}` : `Reserve ${entity.name}`, body, { wide: true });
    load();
  }

  // ===================================================================
  // Student: Find a time (combined faculty + room availability)
  // ===================================================================
  VIEWS.find = async (params) => {
    const [faculty, rooms] = await Promise.all([Api.get('/faculty'), Api.get('/rooms')]);
    const today = S.todayStr();
    const st = {
      facultyId: params.faculty || '', needRoom: true, block: '', floor: '', roomId: '', roomType: '', minCapacity: '',
      dateFrom: today, dateTo: S.addDays(today, 6), duration: 60, result: null, pick: null
    };
    let pickToken = 0;

    const resultsHost = h('div', { 'aria-live': 'polite' });
    const detailHost = h('aside', { class: 'detail' });

    const roomFilters = h('div', { class: 'find-form wide', style: 'padding:0' });
    const roomBlock = h('div', { class: 'wide find-form', style: 'padding:0' });

    const facultySel = select([['', 'Choose a faculty member'], ...faculty.map((f) => [f.id, `${f.name} (${f.department})`])], st.facultyId, (v) => { st.facultyId = v; });
    const needRoom = h('input', {
      type: 'checkbox', checked: true,
      onChange: (e) => { st.needRoom = e.target.checked; roomBlock.hidden = !st.needRoom; }
    });
    const buildingSel = select([['', 'Any building'], ...BUILDINGS.map((b) => [b, b])], '', (v) => { st.block = v; st.floor = ''; st.roomId = ''; refreshRooms(); });
    const floorSel = select([['', 'Select a building first']], '', (v) => { st.floor = v; st.roomId = ''; refreshRooms(); });
    const roomSel = select([['', 'Any suitable room']], '', (v) => { st.roomId = v; roomFilters.hidden = !!v; });
    function refreshRooms() {
      setOptions(floorSel, st.block
        ? [['', 'All floors'], ...floorsOf(rooms, st.block).map((f) => [f, 'Floor ' + f])]
        : [['', 'Select a building first']], st.floor);
      floorSel.disabled = !st.block;
      setOptions(roomSel, [['', 'Any suitable room'], ...roomsIn(rooms, st.block, st.floor).map((r) => [r.id, `${r.name} (seats ${r.capacity})`])], st.roomId);
      roomFilters.hidden = !!st.roomId;
    }
    const typeSel = select([['', 'Any type'], ...Object.entries(ROOM_TYPES)], '', (v) => { st.roomType = v; });
    const capInput = h('input', { type: 'number', min: 1, max: 500, placeholder: 'e.g. 4', onInput: (e) => { st.minCapacity = e.target.value; } });
    const fromInput = h('input', { type: 'date', min: today, value: st.dateFrom, onChange: (e) => { st.dateFrom = e.target.value; } });
    const toInput = h('input', { type: 'date', min: today, value: st.dateTo, onChange: (e) => { st.dateTo = e.target.value; } });
    const durSel = select([[15, '15 minutes'], [30, '30 minutes'], [45, '45 minutes'], [60, '1 hour'], [90, '1 hour 30 minutes'], [120, '2 hours']], st.duration, (v) => { st.duration = Number(v); });
    const searchBtn = h('button', { class: 'btn', type: 'submit' }, 'Find common hours');

    roomFilters.replaceChildren(field('Room type', typeSel), field('Minimum seats', capInput));
    roomBlock.replaceChildren(
      field('Select Building', buildingSel),
      field('Select Floor', floorSel, 'Pick a floor to search every room on it. Without one, only the first few rooms are checked each day.'),
      field('Room', roomSel),
      roomFilters);
    refreshRooms();

    const form = h('form', {
      class: 'panel',
      onSubmit: (e) => { e.preventDefault(); run(searchBtn, search); }
    },
      h('div', { class: 'find-form' },
        h('div', { class: 'wide' }, field('Faculty member', facultySel)),
        field('From', fromInput), field('To', toInput), field('Meeting length', durSel),
        h('div', { class: 'wide' }, h('label', { class: 'check' }, needRoom, 'I need a room for this meeting')),
        roomBlock,
        h('div', { class: 'wide' }, searchBtn)));

    async function search() {
      if (!st.facultyId) { toast('Choose a faculty member first.', 'error'); return; }
      if (!st.dateFrom || !st.dateTo || st.dateTo < st.dateFrom) { toast('The end date must not be before the start date.', 'error'); return; }
      if (S.daysBetween(st.dateFrom, st.dateTo) >= cfg.MAX_SEARCH_DAYS) { toast(`Search up to ${cfg.MAX_SEARCH_DAYS} days at a time.`, 'error'); return; }
      st.pick = null; renderDetail();
      resultsHost.replaceChildren(loadingEl('Comparing calendars…'));
      try {
        st.result = await Api.post('/availability/combined', {
          facultyId: Number(st.facultyId), needRoom: st.needRoom,
          roomId: st.needRoom && st.roomId ? Number(st.roomId) : null,
          block: st.needRoom && st.block ? st.block : null,
          floor: st.needRoom && st.floor ? Number(st.floor) : null,
          roomType: st.needRoom && !st.roomId && st.roomType ? st.roomType : null,
          minCapacity: st.needRoom && !st.roomId && st.minCapacity ? Number(st.minCapacity) : null,
          dateFrom: st.dateFrom, dateTo: st.dateTo, durationMinutes: st.duration
        });
        renderResults();
      } catch (e) {
        resultsHost.replaceChildren(errorBox(e));
      }
    }

    const keyOf = (p) => `${p.date}|${p.roomId}|${p.start}`;

    function renderResults() {
      const { windows, durationMinutes } = st.result;
      if (!windows.length) {
        resultsHost.replaceChildren(h('div', { class: 'panel' }, emptyState(
          'No common hours in this range',
          st.needRoom
            ? 'The faculty member and a matching room are never free together. Try more days, a shorter meeting, or fewer room requirements.'
            : 'The faculty member has no free gap that long. Try more days or a shorter meeting.')));
        return;
      }
      const byDate = new Map();
      windows.forEach((w) => { if (!byDate.has(w.date)) byDate.set(w.date, new Map()); const g = byDate.get(w.date); const k = w.roomId ?? 'none'; if (!g.has(k)) g.set(k, []); g.get(k).push(w); });

      resultsHost.replaceChildren(h('div', { class: 'panel' },
        h('h2', {}, `${byDate.size} ${byDate.size === 1 ? 'day has' : 'days have'} common hours`),
        h('p', { class: 'hint', style: 'margin:6px 0 14px' }, `Start times for a ${durationMinutes}-minute meeting. Choose one to see both calendars.`),
        [...byDate].map(([date, groups]) => h('section', { class: 'day' },
          h('h3', {}, S.fmtDay(date)),
          [...groups].map(([, list]) => h('div', { class: 'slot-row' },
            list[0].roomName && h('span', { class: 'room' }, `${list[0].roomName} · seats ${list[0].capacity}`),
            h('div', { class: 'chips' }, list.flatMap((w) => S.startTimes(w, durationMinutes, cfg.SLOT_STEP).map((t) => {
              const p = { date, roomId: w.roomId, roomName: w.roomName, start: t, end: S.addMinutes(t, durationMinutes) };
              return h('button', {
                class: 'chip', type: 'button', 'data-key': keyOf(p), 'aria-pressed': 'false',
                'aria-label': `${S.fmtDay(date)} ${t} to ${p.end}${w.roomName ? ', ' + w.roomName : ''}`,
                onClick: () => pick(p)
              }, `${t}–${p.end}`);
            })))))))));
    }

    async function pick(p) {
      st.pick = p;
      document.querySelectorAll('.chip[data-key]').forEach((c) => c.setAttribute('aria-pressed', String(c.dataset.key === keyOf(p))));
      renderDetail();
    }

    async function renderDetail() {
      const p = st.pick;
      if (!p) {
        detailHost.replaceChildren(h('div', { class: 'panel' }, h('h2', {}, 'Both calendars'), h('p', { class: 'hint', style: 'margin-top:6px' }, 'Choose a start time to compare the faculty member’s day with the room’s.')));
        return;
      }
      const token = ++pickToken;
      const fac = faculty.find((f) => String(f.id) === String(st.facultyId));
      const laneHost = h('div', {}, loadingEl('Loading both calendars…'));
      const purpose = h('textarea', { maxlength: 300, placeholder: 'What would you like to discuss?' });
      const errBox = h('div', { class: 'form-error', role: 'alert', hidden: true });
      const confirm = h('button', { class: 'btn', type: 'button' }, 'Request appointment');

      confirm.addEventListener('click', () => run(confirm, async () => {
        errBox.hidden = true;
        if (purpose.value.trim().length < 3) { errBox.textContent = 'Add a short purpose so the faculty member knows what to expect.'; errBox.hidden = false; return; }
        try {
          await Api.post('/appointments', { facultyId: Number(st.facultyId), roomId: p.roomId, date: p.date, start: p.start, end: p.end, purpose: purpose.value.trim() });
          toast(`Request sent to ${fac.name} for ${when(p)}.`);
          navigate('#/mine');
        } catch (e) {
          errBox.textContent = e.message; errBox.hidden = false;
          if (e.status === 409) { toast('Someone booked that time first. Showing fresh results.', 'error'); st.pick = null; await search(); }
        }
      }));

      detailHost.replaceChildren(h('div', { class: 'panel' },
        h('h2', {}, 'Both calendars'),
        h('p', { class: 'summary', style: 'margin-top:8px' }, `${fac.name}, ${when(p)}${p.roomName ? ', ' + p.roomName : ''}`),
        laneHost,
        field('Purpose', purpose),
        errBox,
        h('p', { class: 'hint', style: 'margin-top:10px' }, p.roomName ? 'The room is held for you while the faculty member confirms.' : 'The faculty member will confirm your request.'),
        h('div', { class: 'actions' }, confirm)));

      try {
        const [f, r] = await Promise.all([
          Api.get(`/faculty/${st.facultyId}/availability?date=${p.date}`),
          p.roomId ? Api.get(`/rooms/${p.roomId}/availability?date=${p.date}`) : null
        ]);
        if (token !== pickToken) return;
        const rows = [{ label: 'Faculty', kind: 'faculty', free: f.free, busy: f.busy }];
        if (r) {
          rows.push({ label: p.roomName, kind: 'room', free: r.free, busy: r.busy });
          rows.push({ label: 'Both free', kind: 'both', free: S.intersect(f.free, r.free), busy: [] });
        }
        laneHost.replaceChildren(lanes({ rows, selected: p }));
      } catch (e) {
        if (token === pickToken) laneHost.replaceChildren(h('div', { class: 'form-error', role: 'alert' }, e.message));
      }
    }

    renderDetail();
    const root = h('div', {},
      pageHead('Find a time', 'Choose who you want to meet. We compare their calendar with the rooms you could use and list only the times when both are free.'),
      form,
      h('div', { class: 'find-grid' }, resultsHost, detailHost));
    if (st.facultyId) run(searchBtn, search);
    return root;
  };

  // ===================================================================
  // Faculty directory (student)  -- REPLACE the whole existing VIEWS.faculty block with this one
  // ===================================================================
  VIEWS.faculty = async () => {
    const list = await Api.get('/faculty');
    // who: 'current' (default) | 'dept:<name>' | 'all'.   subject: '' = any subject.
    const st = { q: '', who: 'current', subject: '' };
    const grid = h('div', { class: 'cards' });
    const countEl = h('p', { class: 'hint', role: 'status', style: 'margin-bottom:12px' });

    const hasCurrent = list.some((f) => f.current);
    const departments = [...new Set(list.map((f) => f.department).filter(Boolean))].sort();
    const subjects = [...new Set(list.flatMap((f) => f.subjects || []))].sort();

    const inGroup = (f) => {
      if (st.who === 'all') return true;
      // Choosing a subject searches everyone, otherwise the five current faculty would hide the new ones.
      if (st.who === 'current') return st.subject ? true : (!hasCurrent || !!f.current);
      if (st.who.startsWith('dept:')) return f.department === st.who.slice(5);
      return String(f.id) === st.who;
    };

    function draw() {
      const q = st.q.toLowerCase();
      const rows = list.filter((f) => inGroup(f)
        && (!st.subject || (f.subjects || []).includes(st.subject))
        && (!q || `${f.name} ${f.email} ${f.department} ${f.title} ${(f.subjects || []).join(' ')}`.toLowerCase().includes(q)));
      countEl.textContent = rows.length === list.length
        ? `${list.length} faculty members`
        : `Showing ${rows.length} of ${list.length} faculty members`;
      grid.replaceChildren(...(rows.length ? rows.map((f) => h('article', { class: 'card' },
        h('h3', {}, f.name),
        f.email && h('p', { class: 'meta' }, f.email),
        h('p', { class: 'meta' }, [f.title, f.department].filter(Boolean).join(' · ')),
        f.office && h('p', { class: 'meta' }, 'Office ' + f.office),
        (f.subjects || []).length > 0 && h('div', { class: 'tags' }, f.subjects.map((x) => h('span', { class: 'tag' }, x))),
        h('div', { class: 'row-actions' },
          h('button', { class: 'btn small', type: 'button', onClick: () => openAvailability({ kind: 'faculty', entity: f }) }, 'See availability'),
          h('a', { class: 'btn small secondary', href: '#/find?faculty=' + f.id }, 'Find a time with a room'))))
        : [emptyState('No one matches', 'Try a different name, subject or department.')]));
    }
    draw();
    return h('div', {},
      pageHead('Faculty', 'Check when someone is free and request a meeting.'),
      h('div', { class: 'toolbar' },
        field('Search', h('input', { type: 'text', placeholder: 'Name, email, department or subject', onInput: (e) => { st.q = e.target.value; draw(); } })),
        field('Subject', select([['', 'Any subject'], ...subjects.map((s) => [s, s])], st.subject, (v) => { st.subject = v; draw(); })),
        field('Faculty member', select([
          ['current', 'Current faculty'],
          ...departments.map((d) => ['dept:' + d, d]),
          ['all', 'All departments']
        ], st.who, (v) => { st.who = v; draw(); }))),
      countEl, grid);
  };

  // ===================================================================
  // Rooms & resources (student, faculty)
  // ===================================================================
  VIEWS.rooms = async () => {
    const [rooms, resources] = await Promise.all([Api.get('/rooms'), Api.get('/resources')]);
    const st = { q: '', type: '', cap: '', block: '', floor: '' };
    const roomGrid = h('div', { class: 'cards' });
    const buildingSel = select([['', 'All buildings'], ...BUILDINGS.map((b) => [b, b])], '', (v) => { st.block = v; st.floor = ''; refreshFloors(); draw(); });
    const floorSel = select([['', 'Select a building first']], '', (v) => { st.floor = v; draw(); });
    function refreshFloors() {
      setOptions(floorSel, st.block
        ? [['', 'All floors'], ...floorsOf(rooms, st.block).map((f) => [f, 'Floor ' + f])]
        : [['', 'Select a building first']], st.floor);
      floorSel.disabled = !st.block;
    }
    refreshFloors();

    function draw() {
      const q = st.q.toLowerCase();
      const rows = rooms.filter((r) => (!st.type || r.type === st.type) && (!st.cap || r.capacity >= Number(st.cap))
        && (!st.block || r.block === st.block) && (!st.floor || String(r.floor) === st.floor)
        && (!q || `${r.name} ${r.building} ${(r.features || []).join(' ')}`.toLowerCase().includes(q)));
      roomGrid.replaceChildren(...(rows.length ? rows.map((r) => h('article', { class: 'card' },
        h('h3', {}, r.name),
        h('p', { class: 'meta' }, `${r.building} · ${ROOM_TYPES[r.type] || r.type} · seats ${r.capacity}`),
        h('div', { class: 'tags' }, (r.features || []).map((f) => h('span', { class: 'tag' }, f))),
        h('div', { class: 'row-actions' }, h('button', { class: 'btn small', type: 'button', onClick: () => openAvailability({ kind: 'room', entity: r }) }, 'Check and reserve'))))
        : [emptyState('No rooms match', 'Try fewer filters.')]));
    }
    draw();

    return h('div', {},
      pageHead('Rooms & resources', 'See when a room or shared equipment is free, then reserve it.'),
      h('div', { class: 'toolbar' },
        field('Search', h('input', { type: 'text', placeholder: 'Name, building or feature', onInput: (e) => { st.q = e.target.value; draw(); } })),
        field('Select Building', buildingSel),
        field('Select Floor', floorSel),
        field('Type', select([['', 'Any type'], ...Object.entries(ROOM_TYPES)], '', (v) => { st.type = v; draw(); })),
        field('Minimum seats', h('input', { type: 'number', min: 1, placeholder: 'Any', onInput: (e) => { st.cap = e.target.value; draw(); } }))),
      roomGrid,
      h('h2', { class: 'section-title' }, 'Shared resources'),
      resources.length
        ? h('div', { class: 'cards' }, resources.map((r) => h('article', { class: 'card' },
          h('h3', {}, r.name),
          h('p', { class: 'meta' }, r.category),
          r.description && h('p', {}, r.description),
          h('div', { class: 'row-actions' }, h('button', { class: 'btn small', type: 'button', onClick: () => openAvailability({ kind: 'resource', entity: r }) }, 'Check and reserve')))))
        : emptyState('No shared resources yet'));
  };

  // ===================================================================
  // My bookings (student: appointments + reservations; faculty: reservations)
  // ===================================================================
  VIEWS.mine = async () => {
    const isStudent = state.user.role === 'STUDENT';
    const [appts, bookings] = await Promise.all([isStudent ? Api.get('/appointments/mine') : [], Api.get('/bookings/mine')]);
    const canCancel = (x) => (x.status === 'PENDING' || x.status === 'CONFIRMED') && S.isFuture(x.date, x.start);

    const apptItem = (a) => h('div', { class: 'item' },
      h('div', {},
        h('div', { class: 'when' }, when(a)),
        h('div', {}, `Meeting with ${a.facultyName}`),
        h('div', { class: 'sub' }, [a.roomName ? 'Room ' + a.roomName : 'No room', a.purpose].join(' · '))),
      h('div', { class: 'row-actions' }, badge(a.status),
        canCancel(a) && cancelButton('Cancel this appointment?', () => Api.patch(`/appointments/${a.id}/cancel`))));

    const bookingItem = (b) => h('div', { class: 'item' },
      h('div', {},
        h('div', { class: 'when' }, when(b)),
        h('div', {}, [b.roomName, ...b.resourceNames].filter(Boolean).join(' + ')),
        h('div', { class: 'sub' }, b.purpose)),
      h('div', { class: 'row-actions' }, badge(b.status),
        canCancel(b) && cancelButton('Cancel this reservation?', () => Api.patch(`/bookings/${b.id}/cancel`))));

    function cancelButton(text, call) {
      return h('button', {
        class: 'btn small danger-outline', type: 'button',
        onClick: async (e) => {
          if (!(await confirmDialog(text, 'Cancel it'))) return;
          await run(e.target, async () => {
            try { await call(); toast('Cancelled.'); route(); } catch (err) { toast(err.message, 'error'); }
          });
        }
      }, 'Cancel');
    }

    const section = (title, rows, render, emptyTitle, emptyText, cta) => h('section', {},
      h('h2', { class: 'section-title' }, title),
      rows.length ? h('div', { class: 'list' }, rows.map(render)) : h('div', { class: 'panel' }, emptyState(emptyTitle, emptyText, cta)));

    return h('div', {},
      pageHead(isStudent ? 'My bookings' : 'My reservations', isStudent ? 'Appointments with faculty and rooms or equipment you reserved.' : 'Rooms and equipment you reserved.'),
      isStudent && section('Appointments', appts, apptItem, 'No appointments yet', 'Find a time when a faculty member and a room are both free.', h('a', { class: 'btn', href: '#/find' }, 'Find a time')),
      section('Room and resource reservations', bookings, bookingItem, 'No reservations yet', 'Reserve a room or shared equipment.', h('a', { class: 'btn', href: '#/rooms' }, 'Browse rooms')));
  };

  // ===================================================================
  // Faculty: appointment requests
  // ===================================================================
  VIEWS.requests = async () => {
    const list = await Api.get('/appointments/incoming');
    const pending = list.filter((a) => a.status === 'PENDING' && S.isFuture(a.date, a.start));
    const upcoming = list.filter((a) => a.status === 'CONFIRMED' && S.isFuture(a.date, a.start));
    const rest = list.filter((a) => !pending.includes(a) && !upcoming.includes(a)).reverse();

    async function respond(a, status, button) {
      await run(button, async () => {
        try { await Api.patch(`/appointments/${a.id}/status`, { status }); toast(status === 'CONFIRMED' ? 'Appointment confirmed.' : 'Request declined.'); route(); }
        catch (e) { toast(e.message, 'error'); }
      });
    }

    const item = (a, actions) => h('div', { class: 'item' },
      h('div', {},
        h('div', { class: 'when' }, when(a)),
        h('div', {}, a.studentName),
        h('div', { class: 'sub' }, [a.roomName ? 'Room ' + a.roomName : 'No room', a.purpose].join(' · '))),
      h('div', { class: 'row-actions' }, badge(a.status), actions));

    const section = (title, rows, render, emptyText) => h('section', {},
      h('h2', { class: 'section-title' }, title),
      rows.length ? h('div', { class: 'list' }, rows.map(render)) : h('div', { class: 'panel' }, emptyState(emptyText)));

    return h('div', {},
      pageHead('Appointments', 'Answer student requests and see what is coming up.'),
      section('Waiting for your answer', pending, (a) => item(a, [
        h('button', { class: 'btn small', type: 'button', onClick: (e) => respond(a, 'CONFIRMED', e.target) }, 'Confirm'),
        h('button', { class: 'btn small danger-outline', type: 'button', onClick: (e) => respond(a, 'DECLINED', e.target) }, 'Decline')]), 'No requests are waiting.'),
      section('Upcoming', upcoming, (a) => item(a, h('button', {
        class: 'btn small danger-outline', type: 'button',
        onClick: async (e) => {
          if (!(await confirmDialog(`Cancel the appointment with ${a.studentName}?`, 'Cancel it'))) return;
          await run(e.target, async () => { try { await Api.patch(`/appointments/${a.id}/cancel`); toast('Cancelled.'); route(); } catch (err) { toast(err.message, 'error'); } });
        }
      }, 'Cancel')), 'Nothing confirmed yet.'),
      rest.length ? section('Earlier and closed', rest, (a) => item(a), '') : null);
  };

  // ===================================================================
  // Schedule editor (faculty: own; admin: choose faculty)
  // ===================================================================
  async function scheduleEditor(fid) {
    const sch = await Api.get(`/faculty/${fid}/schedule`);
    const wh = {};
    for (let d = 0; d < 7; d++) wh[d] = (sch.workingHours[d] || []).map((x) => ({ ...x }));
    let blocks = sch.blocks.slice().sort((a, b) => (a.date + a.start).localeCompare(b.date + b.start));

    const daysHost = h('div', { class: 'week' });
    function drawDays() {
      daysHost.replaceChildren(...WEEK_ORDER.map((d) => h('div', { class: 'week-row' },
        h('strong', {}, DAY_NAMES[d]),
        h('div', {},
          wh[d].length ? wh[d].map((r, i) => h('div', { class: 'range' },
            h('input', { type: 'time', value: r.start, 'aria-label': `${DAY_NAMES[d]} start`, onChange: (e) => { r.start = e.target.value; } }),
            h('span', {}, 'to'),
            h('input', { type: 'time', value: r.end, 'aria-label': `${DAY_NAMES[d]} end`, onChange: (e) => { r.end = e.target.value; } }),
            h('button', { class: 'btn small secondary', type: 'button', onClick: () => { wh[d].splice(i, 1); drawDays(); } }, 'Remove')))
            : h('p', { class: 'off' }, 'Not available'),
          h('button', {
            class: 'btn small secondary', type: 'button',
            onClick: () => { const last = wh[d][wh[d].length - 1]; wh[d].push(last ? { start: last.end, end: S.addMinutes(last.end, 60) } : { start: '09:00', end: '17:00' }); drawDays(); }
          }, 'Add hours')))));
    }
    drawDays();

    const saveBtn = h('button', { class: 'btn', type: 'button' }, 'Save weekly hours');
    saveBtn.addEventListener('click', () => run(saveBtn, async () => {
      for (const d of WEEK_ORDER) {
        const list = wh[d].slice().sort((a, b) => a.start.localeCompare(b.start));
        for (let i = 0; i < list.length; i++) {
          if (!list[i].start || !list[i].end || list[i].end <= list[i].start) { toast(`${DAY_NAMES[d]}: each range must end after it starts.`, 'error'); return; }
          if (i && list[i].start < list[i - 1].end) { toast(`${DAY_NAMES[d]}: hours must not overlap.`, 'error'); return; }
        }
      }
      try { await Api.put(`/faculty/${fid}/schedule/working-hours`, { workingHours: wh }); toast('Weekly hours saved.'); }
      catch (e) { toast(e.message, 'error'); }
    }));

    // blocked time
    const blockHost = h('div', { class: 'list' });
    function drawBlocks() {
      blockHost.replaceChildren(...(blocks.length ? blocks.map((b) => h('div', { class: 'item' },
        h('div', {}, h('div', { class: 'when' }, when(b)), h('div', { class: 'sub' }, b.reason || 'Blocked')),
        h('button', {
          class: 'btn small danger-outline', type: 'button',
          onClick: async (e) => { await run(e.target, async () => { try { await Api.del(`/faculty/${fid}/schedule/blocks/${b.id}`); blocks = blocks.filter((x) => x.id !== b.id); drawBlocks(); toast('Time unblocked.'); } catch (err) { toast(err.message, 'error'); } }); }
        }, 'Unblock'))) : [h('p', { class: 'hint' }, 'No blocked time. Block a day for a conference, leave or exam duty.')]));
    }
    drawBlocks();

    const bDate = h('input', { type: 'date', min: S.todayStr(), value: S.todayStr() });
    const bStart = h('input', { type: 'time', value: '09:00' });
    const bEnd = h('input', { type: 'time', value: '17:00' });
    const bReason = h('input', { type: 'text', maxlength: 80, placeholder: 'e.g. Conference' });
    const bBtn = h('button', { class: 'btn', type: 'submit' }, 'Block this time');
    const bForm = h('form', {
      class: 'form',
      onSubmit: (e) => {
        e.preventDefault();
        run(bBtn, async () => {
          if (bEnd.value <= bStart.value) { toast('The end time must be after the start time.', 'error'); return; }
          try {
            const created = await Api.post(`/faculty/${fid}/schedule/blocks`, { date: bDate.value, start: bStart.value, end: bEnd.value, reason: bReason.value });
            blocks.push(created); blocks.sort((a, b) => (a.date + a.start).localeCompare(b.date + b.start)); drawBlocks();
            bReason.value = ''; toast('Time blocked.');
          } catch (err) { toast(err.message, 'error'); }
        });
      }
    }, h('div', { class: 'form-grid' }, field('Date', bDate), field('From', bStart), field('To', bEnd), field('Reason (optional)', bReason)), h('div', { class: 'actions', style: 'margin-top:0' }, bBtn));

    return h('div', {},
      h('div', { class: 'panel' }, h('h2', {}, 'Weekly working hours'),
        h('p', { class: 'hint', style: 'margin:6px 0 10px' }, 'Students can only book you inside these hours.'),
        daysHost, h('div', { class: 'actions' }, saveBtn)),
      h('div', { class: 'panel' }, h('h2', {}, 'Blocked time'), bForm, h('div', { style: 'margin-top:16px' }, blockHost)));
  }

  VIEWS.schedule = async () => h('div', {}, pageHead('My schedule', 'Set when you are available and block out time you are not.'), await scheduleEditor(state.user.id));

  VIEWS.schedules = async () => {
    const faculty = await Api.get('/faculty');
    if (!faculty.length) return h('div', {}, pageHead('Schedules'), emptyState('No faculty yet', 'Add a faculty user first.'));
    const host = h('div', {}, loadingEl());
    async function show(id) {
      host.replaceChildren(loadingEl());
      try { host.replaceChildren(await scheduleEditor(id)); } catch (e) { host.replaceChildren(errorBox(e)); }
    }
    show(faculty[0].id);
    return h('div', {},
      pageHead('Schedules', 'Edit working hours and blocked time for any faculty member.'),
      h('div', { class: 'toolbar' }, field('Faculty member', select(faculty.map((f) => [f.id, `${f.name} (${f.department})`]), faculty[0].id, show))),
      host);
  };

  // ===================================================================
  // Admin: overview, CRUD, all bookings
  // ===================================================================
  VIEWS.overview = async () => {
    const s = await Api.get('/admin/stats');
    const stat = (label, value) => h('div', { class: 'stat' }, h('div', { class: 'value' }, value), h('div', { class: 'label' }, label));
    return h('div', {},
      pageHead('Overview', 'What is happening across the university right now.'),
      h('div', { class: 'stats' },
        stat('Active users', s.activeUsers), stat('Faculty', s.facultyCount), stat('Students', s.studentCount),
        stat('Rooms', s.rooms), stat('Shared resources', s.resources),
        stat('Requests awaiting faculty', s.pendingAppointments),
        stat('Upcoming appointments', s.upcomingAppointments), stat('Upcoming reservations', s.upcomingReservations)),
      h('div', { class: 'actions', style: 'justify-content:flex-start' },
        h('a', { class: 'btn', href: '#/users' }, 'Manage users'),
        h('a', { class: 'btn secondary', href: '#/bookings' }, 'Review bookings')));
  };

  const activeBadge = (row) => h('span', { class: 'badge ' + (row.active ? 'ok' : '') }, row.active ? 'Active' : 'Inactive');

  const CRUD = {
    users: {
      title: 'Users & faculty', sub: 'Create accounts and set roles. Faculty accounts also carry department and office details.', noun: 'user', path: '/admin/users',
      columns: [['Name', (r) => r.name], ['Email', (r) => r.email], ['Role', (r) => ROLE_LABEL[r.role]], ['Department', (r) => r.department || ''], ['Status', activeBadge]],
      defaults: () => ({ role: 'STUDENT', active: true }),
      fields: [
        { key: 'name', label: 'Full name', type: 'text', required: true },
        { key: 'email', label: 'Email', type: 'email', required: true },
        { key: 'role', label: 'Role', type: 'select', options: Object.entries(ROLE_LABEL), required: true },
        { key: 'password', label: 'Password', type: 'password', hint: 'At least 8 characters. When editing, leave blank to keep the current password.', requiredOnCreate: true },
        { key: 'department', label: 'Department', type: 'text', showIf: (d) => d.role === 'FACULTY' },
        { key: 'title', label: 'Title', type: 'text', showIf: (d) => d.role === 'FACULTY' },
        { key: 'office', label: 'Office', type: 'text', showIf: (d) => d.role === 'FACULTY' },
        { key: 'subjects', label: 'Subjects taught', type: 'text', hint: 'Separate with commas', showIf: (d) => d.role === 'FACULTY', toForm: (v) => (v || []).join(', '), fromForm: (v) => String(v || '').split(',').map((x) => x.trim()).filter(Boolean) },
        { key: 'active', label: 'Account is active', type: 'checkbox' }
      ]
    },
    rooms: {
      title: 'Rooms', sub: 'Classrooms, meeting rooms and halls students and faculty can reserve.', noun: 'room', path: '/admin/rooms',
      columns: [['Name', (r) => r.name], ['Building', (r) => r.building], ['Type', (r) => ROOM_TYPES[r.type] || r.type], ['Seats', (r) => r.capacity], ['Features', (r) => (r.features || []).join(', ')], ['Status', activeBadge]],
      defaults: () => ({ type: 'CLASSROOM', active: true, features: [] }),
      fields: [
        { key: 'name', label: 'Room name', type: 'text', required: true },
        { key: 'building', label: 'Building', type: 'text' },
        { key: 'type', label: 'Type', type: 'select', options: Object.entries(ROOM_TYPES), required: true },
        { key: 'capacity', label: 'Seats', type: 'number', required: true },
        { key: 'features', label: 'Features', type: 'text', hint: 'Separate with commas, e.g. projector, whiteboard', toForm: (v) => (v || []).join(', '), fromForm: (v) => String(v || '').split(',').map((x) => x.trim()).filter(Boolean) },
        { key: 'active', label: 'Bookable', type: 'checkbox' }
      ]
    },
    resources: {
      title: 'Resources', sub: 'Shared academic equipment such as projectors, laptop carts and lab tools.', noun: 'resource', path: '/admin/resources',
      columns: [['Name', (r) => r.name], ['Category', (r) => r.category], ['Description', (r) => r.description], ['Status', activeBadge]],
      defaults: () => ({ active: true }),
      fields: [
        { key: 'name', label: 'Resource name', type: 'text', required: true },
        { key: 'category', label: 'Category', type: 'text' },
        { key: 'description', label: 'Description', type: 'textarea' },
        { key: 'active', label: 'Bookable', type: 'checkbox' }
      ]
    }
  };

  function crudView(conf) {
    return async () => {
      let rows = await Api.get(conf.path);
      let q = '';
      const tableHost = h('div', { class: 'table-wrap' });

      function draw() {
        const needle = q.toLowerCase();
        const shown = rows.filter((r) => !needle || JSON.stringify(Object.values(r)).toLowerCase().includes(needle));
        tableHost.replaceChildren(shown.length
          ? h('table', {},
            h('thead', {}, h('tr', {}, conf.columns.map(([label]) => h('th', { scope: 'col' }, label)), h('th', { scope: 'col' }, h('span', { class: 'sr' }, 'Actions')))),
            h('tbody', {}, shown.map((r) => h('tr', {},
              conf.columns.map(([, get]) => h('td', {}, get(r))),
              h('td', {}, h('div', { class: 'row-actions' }, h('button', { class: 'btn small secondary', type: 'button', onClick: () => openForm(r) }, 'Edit')))))))
          : emptyState('Nothing found', rows.length ? 'Try a different search.' : `Add the first ${conf.noun}.`));
      }

      function openForm(row) {
        const creating = !row;
        const data = creating ? conf.defaults() : { ...row };
        conf.fields.forEach((f) => { if (f.toForm) data[f.key] = f.toForm(data[f.key]); });
        const fieldsHost = h('div', { class: 'form-grid' });
        const errBox = h('div', { class: 'form-error', role: 'alert', hidden: true });
        const saveBtn = h('button', { class: 'btn', type: 'submit' }, creating ? `Add ${conf.noun}` : 'Save changes');

        function control(f) {
          const v = data[f.key];
          if (f.type === 'checkbox') return h('label', { class: 'check' }, h('input', { type: 'checkbox', checked: !!v, onChange: (e) => { data[f.key] = e.target.checked; } }), f.label);
          let el;
          if (f.type === 'select') el = select(f.options, v ?? '', (val) => { data[f.key] = val; drawFields(); });
          else if (f.type === 'textarea') el = h('textarea', { value: v ?? '', onInput: (e) => { data[f.key] = e.target.value; } });
          else el = h('input', { type: f.type, value: v ?? '', autocomplete: f.type === 'password' ? 'new-password' : 'off', onInput: (e) => { data[f.key] = f.type === 'number' ? (e.target.value === '' ? '' : Number(e.target.value)) : e.target.value; } });
          return field(f.label + (f.required || (creating && f.requiredOnCreate) ? ' *' : ''), el, f.hint);
        }
        function drawFields() {
          fieldsHost.replaceChildren(...conf.fields.filter((f) => !f.showIf || f.showIf(data)).map(control));
        }
        drawFields();

        const form = h('form', {
          class: 'form', novalidate: true,
          onSubmit: (e) => {
            e.preventDefault();
            errBox.hidden = true;
            run(saveBtn, async () => {
              const payload = {};
              for (const f of conf.fields) {
                if (f.showIf && !f.showIf(data)) continue;
                const val = f.fromForm ? f.fromForm(data[f.key]) : data[f.key];
                const missing = val === undefined || val === null || val === '';
                if (missing && (f.required || (creating && f.requiredOnCreate))) { errBox.textContent = `${f.label} is required.`; errBox.hidden = false; return; }
                if (f.key === 'password' && missing) continue;
                payload[f.key] = f.type === 'checkbox' ? !!val : val;
              }
              try {
                if (creating) await Api.post(conf.path, payload); else await Api.put(`${conf.path}/${row.id}`, payload);
                $('#modal').close();
                toast(creating ? `${conf.noun[0].toUpperCase() + conf.noun.slice(1)} added.` : 'Changes saved.');
                rows = await Api.get(conf.path); draw();
              } catch (err) { errBox.textContent = err.message; errBox.hidden = false; }
            });
          }
        }, fieldsHost, errBox, h('div', { class: 'actions' }, h('button', { class: 'btn secondary', type: 'button', onClick: () => $('#modal').close() }, 'Cancel'), saveBtn));

        openModal(creating ? `Add ${conf.noun}` : `Edit ${row.name}`, form);
      }

      draw();
      return h('div', {},
        pageHead(conf.title, conf.sub),
        h('div', { class: 'toolbar' },
          field('Search', h('input', { type: 'text', onInput: (e) => { q = e.target.value; draw(); } })),
          h('span', { class: 'spacer' }),
          h('button', { class: 'btn', type: 'button', onClick: () => openForm(null) }, `Add ${conf.noun}`)),
        tableHost);
    };
  }
  VIEWS.users = crudView(CRUD.users);
  VIEWS['rooms-admin'] = crudView(CRUD.rooms);
  VIEWS['resources-admin'] = crudView(CRUD.resources);

  VIEWS.bookings = async () => {
    const [appts, bookings] = await Promise.all([Api.get('/admin/appointments'), Api.get('/admin/bookings')]);
    const st = { tab: 'appointments', status: '' };
    const host = h('div', { class: 'table-wrap' });
    const tabs = h('div', { class: 'chips', role: 'group', 'aria-label': 'Booking type' });

    async function cancel(kind, row, button) {
      if (!(await confirmDialog(`Cancel ${when(row)}?`, 'Cancel it'))) return;
      await run(button, async () => {
        try { await Api.patch(`/${kind}/${row.id}/cancel`); toast('Cancelled.'); route(); } catch (e) { toast(e.message, 'error'); }
      });
    }

    function draw() {
      tabs.replaceChildren(...[['appointments', `Appointments (${appts.length})`], ['reservations', `Reservations (${bookings.length})`]].map(([id, label]) =>
        h('button', { class: 'chip', type: 'button', 'aria-pressed': String(st.tab === id), onClick: () => { st.tab = id; draw(); } }, label)));
      const isAppt = st.tab === 'appointments';
      const rows = (isAppt ? appts : bookings).filter((r) => !st.status || r.status === st.status);
      const open = (r) => (r.status === 'PENDING' || r.status === 'CONFIRMED') && S.isFuture(r.date, r.start);
      host.replaceChildren(rows.length ? h('table', {},
        h('thead', {}, h('tr', {}, (isAppt ? ['When', 'Student', 'Faculty', 'Room', 'Purpose', 'Status', ''] : ['When', 'Booked by', 'Room and resources', 'Purpose', 'Status', '']).map((t) => h('th', { scope: 'col' }, t)))),
        h('tbody', {}, rows.map((r) => h('tr', {},
          h('td', {}, when(r)),
          isAppt ? [h('td', {}, r.studentName), h('td', {}, r.facultyName), h('td', {}, r.roomName || '—')]
            : [h('td', {}, r.userName), h('td', {}, [r.roomName, ...r.resourceNames].filter(Boolean).join(' + '))],
          h('td', {}, r.purpose),
          h('td', {}, badge(r.status)),
          h('td', {}, open(r) && h('button', { class: 'btn small danger-outline', type: 'button', onClick: (e) => cancel(isAppt ? 'appointments' : 'bookings', r, e.target) }, 'Cancel'))))))
        : emptyState('Nothing here', 'No bookings match this filter.'));
    }
    draw();

    return h('div', {},
      pageHead('All bookings', 'Review and cancel appointments and reservations across the university.'),
      h('div', { class: 'toolbar' }, tabs, h('span', { class: 'spacer' }),
        field('Status', select([['', 'Any status'], ...Object.entries(STATUS).map(([k, v]) => [k, v[0]])], '', (v) => { st.status = v; draw(); }))),
      host);
  };

  // ===================================================================
  // Boot
  // ===================================================================
  async function boot() {
    Api.onUnauthorized(() => { if (state.user) { toast('Your session ended. Please sign in again.', 'error'); logout(); } });
    $('#modal').addEventListener('click', (e) => { if (e.target === $('#modal')) $('#modal').close(); });
    window.addEventListener('hashchange', route);

    if (Api.hasToken()) {
      try { state.user = await Api.get('/auth/me'); } catch (_) { Api.setToken(null); }
    }
    route();
  }
  boot();
})();