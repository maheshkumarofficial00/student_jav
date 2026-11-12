/**
 * DEMO ONLY – a browser-side stand-in for the Spring Boot API so the UI can be previewed
 * without a backend. It implements the same REST contract (see README.md) and stores data in
 * localStorage. Set USE_MOCK=false in config.js for the real system; this file can then be deleted.
 */
const MockServer = (() => {
  // The key is now STABLE. When the data structure changes, migrate() upgrades the saved data in
  // place (users, bookings and appointments are kept). The old keys are only read, never deleted.
  const KEY = 'ch_mock_db';
  const LEGACY_KEYS = ['ch_mock_db_v5', 'ch_mock_db_v4'];
  const DB_VERSION = 7;   // 7 = adds the 15 extra faculty (see NEW_FACULTY)
  const S = window.Slots;
  const ACTIVE = ['PENDING', 'CONFIRMED'];
const SUBJECTS = [
  'Discrete Mathematics',
  'Design and Analysis of Algorithms',
  'Digital Electronics',
  'Advanced Java Programming',
  'Foundations of Electrical and Electronics Engineering',
  'Aptitude for Engineers '
];  
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const clone = (x) => JSON.parse(JSON.stringify(x));
  const err = (status, message) => new ApiError(message, status);

  function weekdays(ranges) {
    const wh = { 0: [], 1: [], 2: [], 3: [], 4: [], 5: [], 6: [] };
    for (let d = 1; d <= 5; d++) wh[d] = ranges.map((r) => ({ ...r }));
    return wh;
  }


  // ---------- college blocks (297 rooms are generated, not typed by hand) ----------
  // A room number is: prefix + floor + two-digit room number.
  //   floor 2, room 5 -> S205      floor 10, room 1 -> C1001      floor 10, room 15 -> C1015
  // `capacity` is a PLACEHOLDER for every room in a block - change it to the real seat count.
  // `idBase` gives every generated room a stable id (idBase + 1, idBase + 2, ...) so bookings keep pointing
  // at the same room after a migration.
  const CAMPUS_BLOCKS = [
    { name: 'SR Block',       prefix: 'S', startFloor: 2, endFloor: 7,  roomsPerFloor: 15, capacity: 60, idBase: 10000 }, //  90 rooms: S201-S215 ... S701-S715
    { name: 'V Block',        prefix: 'V', startFloor: 1, endFloor: 6,  roomsPerFloor: 12, capacity: 40, idBase: 20000 }, //  72 rooms: V101-V112 ... V601-V612
    { name: 'CV Raman Block', prefix: 'C', startFloor: 2, endFloor: 10, roomsPerFloor: 15, capacity: 50, idBase: 30000 }  // 135 rooms: C201-C215 ... C1001-C1015
  ];

  /** Rooms of one block. Room objects use the same fields as the rest of the project, plus block and floor. */
  function generateRooms(b) {
    const rooms = [];
    let id = b.idBase;
    for (let floor = b.startFloor; floor <= b.endFloor; floor++) {
      for (let n = 1; n <= b.roomsPerFloor; n++) {
        rooms.push({
          id: ++id,
          name: `${b.prefix}${floor}${String(n).padStart(2, '0')}`,
          block: b.name,
          floor,
          building: `${b.name}, Floor ${floor}`,
          type: 'CLASSROOM',
          capacity: b.capacity,
          features: ['projector', 'whiteboard'],
          active: true
        });
      }
    }
    return rooms;
  }
  const generateCampusRooms = () => CAMPUS_BLOCKS.flatMap(generateRooms);

  // ---------- 15 extra faculty (one subject each) ----------
  // Added to a fresh database by seed() and to an already-saved one by migrate(), matched by email,
  // so running it again never creates duplicates and existing users, bookings and appointments stay untouched.
  // department = the subject, the same convention the existing faculty use. Ids 12-26 are fixed.
  const NEW_FACULTY = [
    ['Dr. Arun Kumar',     'arun.kumar@srmap.edu.in',     'Discrete Mathematics',                                  'B-301'],
    ['Dr. Priya Sharma',   'priya.sharma@srmap.edu.in',   'Design and Analysis of Algorithms',                     'B-302'],
    ['Dr. Ravi Teja',      'ravi.teja@srmap.edu.in',      'Digital Electronics',                                   'C-201'],
    ['Dr. Sneha Reddy',    'sneha.reddy@srmap.edu.in',    'Advanced Java Programming',                             'D-110'],
    ['Dr. Kiran Kumar',    'kiran.kumar@srmap.edu.in',    'Foundations of Electrical and Electronics Engineering', 'C-210'],
    ['Dr. Divya Lakshmi',  'divya.lakshmi@srmap.edu.in',  'Aptitude for Engineers',                                'A-120'],
    ['Dr. Sandeep Rao',    'sandeep.rao@srmap.edu.in',    'Discrete Mathematics',                                  'B-303'],
    ['Dr. Ananya Das',     'ananya.das@srmap.edu.in',     'Design and Analysis of Algorithms',                     'B-304'],
    ['Dr. Naveen Kumar',   'naveen.kumar@srmap.edu.in',   'Digital Electronics',                                   'C-202'],
    ['Dr. Pooja Singh',    'pooja.singh@srmap.edu.in',    'Advanced Java Programming',                             'D-111'],
    ['Dr. Rahul Varma',    'rahul.varma@srmap.edu.in',    'Foundations of Electrical and Electronics Engineering', 'C-211'],
    ['Dr. Keerthi Reddy',  'keerthi.reddy@srmap.edu.in',  'Aptitude for Engineers',                                'A-121'],
    ['Dr. Abhishek Rao',   'abhishek.rao@srmap.edu.in',   'Discrete Mathematics',                                  'B-305'],
    ['Dr. Swathi Priya',   'swathi.priya@srmap.edu.in',   'Design and Analysis of Algorithms',                     'B-306'],
    ['Dr. Manoj Kumar',    'manoj.kumar@srmap.edu.in',    'Digital Electronics',                                   'C-203']
  ];
  const NEW_FACULTY_FIRST_ID = 12;
  const NEW_FACULTY_HOURS = [
    [{ start: '09:00', end: '13:00' }, { start: '14:00', end: '17:00' }],
    [{ start: '10:00', end: '16:00' }],
    [{ start: '09:30', end: '12:30' }, { start: '13:30', end: '17:30' }],
    [{ start: '11:00', end: '18:00' }],
    [{ start: '09:00', end: '15:00' }]
  ];

  function ensureNewFaculty(d) {
    NEW_FACULTY.forEach(([name, email, subject, office], i) => {
      if (d.users.some((u) => String(u.email).toLowerCase() === email)) return;
      const wanted = NEW_FACULTY_FIRST_ID + i;
      const id = d.users.some((u) => u.id === wanted) ? ++d.seq : wanted;
      d.users.push({
        id, name, email, role: 'FACULTY', active: true, password: 'Campus@123',
        department: subject, office, title: 'Faculty',
        subjects: [subject],
        workingHours: weekdays(NEW_FACULTY_HOURS[i % NEW_FACULTY_HOURS.length]),
        blocks: []
      });
    });
  }

  const CAMPUS_BUILDING = /^(SR Block|V Block|C Block|CV Raman Block)/;   // C Block = the earlier name of CV Raman Block
  const isCampusRoom = (r) => !!r.block || CAMPUS_BUILDING.test(r.building || '');
  const inBlock = (room, block) => !block || (room.block || 'OTHER') === block;     // 'OTHER' = original sample buildings
  const inFloor = (room, floor) => !floor || String(room.floor) === String(floor);
  // With hundreds of rooms, an "any room" search shows at most this many rooms per day
  // (unless a floor or a specific room is chosen).
  const MAX_ROOMS_PER_DAY = 6;

  function seed() {
    const t = S.todayStr();
    const day = (n) => S.addDays(t, n);
    const full = [{ start: '09:00', end: '13:00' }, { start: '14:00', end: '17:00' }];
    const db = {
      version: DB_VERSION,
      seq: 100000, // new ids start above every generated room id
      
// IDs: admin = 1, faculty = 2 to 9 (8 faculty), students = 10 and 11.
users: [
  {
    id: 1,
    name: 'Meera Rao',
    email: 'admin@srmap.edu.in',
    role: 'ADMIN',
    active: true,
    password: 'Campus@123'
  },
  {
    id: 2,
    name: 'Dr. Jayasree Subramanian',
    email: 'faculty1@srmap.edu.in',
    role: 'FACULTY',
    active: true,
    password: 'Campus@123',
    department: 'Discrete Mathematics',
    office: 'B-210',
    title: 'Faculty',
    subjects: [],
    workingHours: weekdays(full),
    blocks: []
  },
  {
    id: 3,
    name: 'Dr.Sugyan Kumar Mishra',
    email: 'faculty2@srmap.edu.in',
    role: 'FACULTY',
    active: true,
    password: 'Campus@123',
    department: 'Design and Analysis of Algorithms',
    office: 'C-105',
    title: 'Faculty',
    subjects: [],
    workingHours: weekdays([
      { start: '10:00', end: '16:00' }
    ]),
    blocks: []
  },
  {
    id: 4,
    name: 'Dr. Pudi Dhilleswararao',
    email: 'faculty3@srmap.edu.in',
    role: 'FACULTY',
    active: true,
    password: 'Campus@123',
    department: 'Digital Electronics',
    office: 'A-312',
    title: 'Faculty',
    subjects: [],
    workingHours: weekdays([
      { start: '09:30', end: '12:30' },
      { start: '13:30', end: '17:30' }
    ]),
    blocks: []
  },
  {
    id: 5,
    name: 'Dr. Aditya Kumar Sahu',
    email: 'faculty4@srmap.edu.in',
    role: 'FACULTY',
    active: true,
    password: 'Campus@123',
    department: 'Advance Java Programming',
    office: 'D-020',
    title: 'Faculty',
    subjects: [],
    workingHours: weekdays([
      { start: '11:00', end: '18:00' }
    ]),
    blocks: []
  },
  {
    id: 6,
    name: 'Dr. Bibaswan Bose',
    email: 'faculty5@srmap.edu.in',
    role: 'FACULTY',
    active: true,
    password: 'Campus@123',
    department: 'Foundations of Electrical and Electronics Engineering',
    office: '',
    title: 'Faculty',
    subjects: [],
    workingHours: weekdays(full),
    blocks: []
  },
  // ADDED: faculty 6, 7 and 8. These names are made up for the demo - replace them with your real professors if needed.
  {
    id: 7,
    name: 'Dr. Venkata Ramana Rao',
    email: 'faculty6@srmap.edu.in',
    role: 'FACULTY',
    active: true,
    password: 'Campus@123',
    department: 'Foundations of Electrical and Electronics Engineering',
    office: 'B-215',
    title: 'Faculty',
    subjects: [],
    workingHours: weekdays([
      { start: '09:00', end: '15:00' }
    ]),
    blocks: []
  },
  {
    id: 8,
    name: 'Dr. Sneha Priyadarshini',
    email: 'faculty7@srmap.edu.in',
    role: 'FACULTY',
    active: true,
    password: 'Campus@123',
    department: 'Digital Electronics',
    office: 'C-110',
    title: 'Faculty',
    subjects: [],
    workingHours: weekdays([
      { start: '10:00', end: '17:00' }
    ]),
    blocks: []
  },
  {
    id: 9,
    name: 'Dr. Kiran Kumar Reddy',
    email: 'faculty8@srmap.edu.in',
    role: 'FACULTY',
    active: true,
    password: 'Campus@123',
    department: 'Dicrete Mathematics',
    office: 'A-315',
    title: 'Faculty',
    subjects: [],
    workingHours: weekdays([
      { start: '09:30', end: '16:30' }
    ]),
    blocks: []
  },
  // CHANGED: student ids 7 and 8 became 10 and 11 so they do not clash with the new faculty ids.
  {
    id: 10,
    name: 'Priya Sharma',
    email: 'student1@srmap.edu.in',
    role: 'STUDENT',
    active: true,
    password: 'Campus@123'
  },
  {
    id: 11,
    name: 'Karthik Reddy',
    email: 'student2@srmap.edu.in',
    role: 'STUDENT',
    active: true,
    password: 'Campus@123'
  }
],
      rooms: [
        { id: 1, name: 'A-101', building: 'Academic Block A', type: 'CLASSROOM', capacity: 60, features: ['projector', 'whiteboard'], active: true },
        { id: 2, name: 'A-204', building: 'Academic Block A', type: 'CLASSROOM', capacity: 30, features: ['projector'], active: true },
        { id: 3, name: 'Meeting Room M1', building: 'Library', type: 'MEETING_ROOM', capacity: 8, features: ['whiteboard', 'video conferencing'], active: true },
        { id: 4, name: 'Meeting Room M2', building: 'Library', type: 'MEETING_ROOM', capacity: 12, features: ['projector'], active: true },
        { id: 5, name: 'Seminar Hall S1', building: 'Main Building', type: 'SEMINAR_HALL', capacity: 150, features: ['projector', 'sound system'], active: true },
        ...generateCampusRooms()
      ],
      resources: [
        { id: 1, name: 'Portable projector', category: 'AV equipment', description: 'HDMI and VGA, carry case included.', active: true },
        { id: 2, name: 'Laptop cart (20 units)', category: 'Computing', description: 'Charging cart with 20 laptops.', active: true },
        { id: 3, name: 'Video conferencing kit', category: 'AV equipment', description: 'Camera, speakerphone and stand.', active: true },
        { id: 4, name: '3D printer', category: 'Lab equipment', description: 'Fabrication lab, trained users only.', active: true }
      ],
      // CHANGED: studentId values now point to the students (10 = Priya, 11 = Karthik).
      // The old third appointment pointed at id 6, which is a faculty member, not a student.
      appointments: [
        { id: 1, studentId: 10, facultyId: 2, roomId: 3, date: day(1), start: '10:00', end: '11:00', purpose: 'Project review', status: 'CONFIRMED' },
        { id: 2, studentId: 10, facultyId: 2, roomId: null, date: day(1), start: '14:00', end: '14:30', purpose: 'Reference letter', status: 'CONFIRMED' },
        { id: 3, studentId: 11, facultyId: 3, roomId: 4, date: day(2), start: '11:00', end: '12:00', purpose: 'Thesis outline', status: 'PENDING' }
      ],
      bookings: [
        { id: 1, userId: 3, roomId: 3, resourceIds: [], date: day(1), start: '11:00', end: '12:30', purpose: 'Lab group meeting', status: 'CONFIRMED' },
        { id: 2, userId: 4, roomId: 4, resourceIds: [1], date: day(1), start: '13:00', end: '16:00', purpose: 'Workshop prep', status: 'CONFIRMED' }
      ]
    };
    ensureNewFaculty(db);
    db.users.find((u) => u.id === 2).blocks.push({ id: 1, date: day(2), start: '09:00', end: '12:00', reason: 'Conference' });
    return db;
  }

  function readJson(key) {
    try {
      const raw = localStorage.getItem(key);
      const v = raw ? JSON.parse(raw) : null;
      return v && Array.isArray(v.users) && Array.isArray(v.rooms) ? v : null;
    } catch (_) { return null; }
  }

  /**
   * Makes sure the 297 college rooms exist exactly once. Safe to run any number of times:
   *  - rooms that are not part of the three blocks (the sample rooms, rooms an admin added) are kept as they are;
   *  - a block room that already exists keeps its admin edits (type, capacity, features, active) and gets the stable id;
   *  - appointments and bookings that pointed at an old room id are re-pointed to the same room by room number;
   *  - users, appointments, bookings and everything else are not touched.
   */
  function ensureCampusRooms(d) {
    const fresh = generateCampusRooms();
    const freshNames = new Set(fresh.map((r) => r.name));
    const oldByName = new Map();
    const kept = [];
    for (const r of d.rooms) {
      if (isCampusRoom(r) && freshNames.has(r.name)) oldByName.set(r.name, r);
      else kept.push(r);
    }
    const idMap = new Map();
    const campus = fresh.map((r) => {
      const old = oldByName.get(r.name);
      if (!old) return r;
      idMap.set(old.id, r.id);
      return { ...r, type: old.type || r.type, capacity: old.capacity ?? r.capacity, features: old.features || r.features, active: old.active !== false };
    });
    d.rooms = [...kept, ...campus];
    for (const list of [d.appointments, d.bookings]) {
      for (const item of list || []) if (item.roomId != null && idMap.has(item.roomId)) item.roomId = idMap.get(item.roomId);
    }
  }

  function migrate(d) {
    ensureCampusRooms(d);
    d.version = DB_VERSION;
    d.seq = Math.max(d.seq || 0, 100000);
    ensureNewFaculty(d);
    return d;
  }

  function load() {
    const current = readJson(KEY);
    if (current) return migrate(current);
    for (const key of LEGACY_KEYS) {            // data saved by the previous versions of this file
      const old = readJson(key);
      if (old) return migrate(old);
    }
    return seed();
  }
  let db = load();
  const save = () => { try { localStorage.setItem(KEY, JSON.stringify(db)); } catch (_) { /* ignore */ } };
  save(); // store the (possibly migrated) data under the current key
  const nextId = () => ++db.seq;
  const byId = (list, id) => list.find((x) => x.id === Number(id));

  // ---------- availability engine ----------
  function entity(kind, id) {
    return kind === 'faculty' ? byId(db.users.filter((u) => u.role === 'FACULTY'), id)
      : kind === 'room' ? byId(db.rooms, id) : byId(db.resources, id);
  }

  function openWindows(kind, ent, date) {
    if (date < S.todayStr()) return [];
    const dow = S.parseDate(date).getDay();
    let w;
    if (kind === 'faculty') w = (ent.workingHours[dow] || []).map((x) => ({ ...x }));
    else if (dow === 0) w = [];
    else w = [kind === 'room' ? { start: '08:00', end: '20:00' } : { start: '09:00', end: '18:00' }];
    if (date === S.todayStr()) {
      const n = new Date();
      const m = Math.min(Math.ceil((n.getHours() * 60 + n.getMinutes()) / 15) * 15, 1439);
      w = S.subtract(w, [{ start: '00:00', end: S.toTime(m) }]);
    }
    return w;
  }

  function busyFor(kind, id, date) {
    const out = [];
    const appt = (a, label) => out.push({ start: a.start, end: a.end, label });
    if (kind === 'faculty') {
      db.appointments.filter((a) => a.facultyId === id && a.date === date && ACTIVE.includes(a.status)).forEach((a) => appt(a, 'Appointment'));
      (byId(db.users, id).blocks || []).filter((b) => b.date === date).forEach((b) => out.push({ start: b.start, end: b.end, label: b.reason || 'Blocked' }));
    } else if (kind === 'room') {
      db.appointments.filter((a) => a.roomId === id && a.date === date && ACTIVE.includes(a.status)).forEach((a) => appt(a, 'Faculty meeting'));
      db.bookings.filter((b) => b.roomId === id && b.date === date && b.status === 'CONFIRMED').forEach((b) => appt(b, 'Reserved'));
    } else {
      db.bookings.filter((b) => b.resourceIds.includes(id) && b.date === date && b.status === 'CONFIRMED').forEach((b) => appt(b, 'Reserved'));
    }
    return out;
  }

  function availability(kind, id, date) {
    const ent = entity(kind, id);
    if (!ent) throw err(404, 'Not found.');
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date || '')) throw err(400, 'A date in YYYY-MM-DD format is required.');
    const windows = openWindows(kind, ent, date);
    const busy = busyFor(kind, ent.id, date);
    return { date, windows, busy, free: S.subtract(windows, busy) };
  }

  function isFree(kind, id, date, start, end) {
    return availability(kind, id, date).free.some((w) => S.toMin(w.start) <= S.toMin(start) && S.toMin(end) <= S.toMin(w.end));
  }

  function checkTimes(date, start, end) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date || '') || !/^\d{2}:\d{2}$/.test(start || '') || !/^\d{2}:\d{2}$/.test(end || '')) {
      throw err(400, 'Date, start time and end time are required.');
    }
    if (S.toMin(end) <= S.toMin(start)) throw err(400, 'End time must be after the start time.');
  }

  // ---------- serializers ----------
  const publicUser = (u) => ({ id: u.id, name: u.name, email: u.email, role: u.role });
  const publicFaculty = (u) => ({ id: u.id, name: u.name, email: u.email, department: u.department, office: u.office, title: u.title, subjects: u.subjects || [] });
  const adminUser = (u) => { const c = clone(u); delete c.password; delete c.workingHours; delete c.blocks; return c; };
  const sortByTime = (a, b) => (a.date + a.start).localeCompare(b.date + b.start);
  const enrichAppt = (a) => ({
    ...a,
    facultyName: byId(db.users, a.facultyId)?.name,
    studentName: byId(db.users, a.studentId)?.name,
    roomName: a.roomId ? byId(db.rooms, a.roomId)?.name : null,
    roomLocation: a.roomId ? byId(db.rooms, a.roomId)?.building : null
  });
  const enrichBooking = (b) => ({
    ...b,
    userName: byId(db.users, b.userId)?.name,
    roomName: b.roomId ? byId(db.rooms, b.roomId)?.name : null,
    roomLocation: b.roomId ? byId(db.rooms, b.roomId)?.building : null,
    resourceNames: b.resourceIds.map((id) => byId(db.resources, id)?.name)
  });

  // ---------- routing ----------
  const routes = [];
  function r(method, pattern, roles, fn) {
    routes.push({ method, roles, fn, re: new RegExp('^' + pattern.replace(/:(\w+)/g, '(?<$1>[^/]+)') + '$') });
  }

  r('POST', '/auth/login', null, ({ body }) => {
  const email = String(body.email || '').trim().toLowerCase();
  const password = String(body.password || '');

  if (!/^[a-zA-Z0-9._%+-]+@srmap\.edu\.in$/.test(email)) {
  throw err(400, 'Use an email ending in @srmap.edu.in.');
  }
  
  if (
    password.length < 8 ||
    !/[A-Z]/.test(password) ||
    !/[a-z]/.test(password) ||
    !/[0-9]/.test(password) ||
    !/[^A-Za-z0-9]/.test(password)
  ) {
    throw err(
      400,
      'Password must have at least 8 characters, uppercase, lowercase, a number and a special character.'
    );
  }

  const u = db.users.find(
    (x) =>
      x.email.toLowerCase() === email &&
      x.password === password
  );

  if (!u || !u.active) {
    throw err(401, 'Email or password is incorrect.');
  }

  return {
    token: 'mock-' + u.id,
    user: publicUser(u)
  };
});

  r('POST', '/auth/register', null, ({ body }) => {
  const name = String(body.name || '').trim();
  const email = String(body.email || '').trim().toLowerCase();
  const password = String(body.password || '');

  if (name.length < 2) {
    throw err(400, 'Enter your full name.');
  }

 if (!/^[a-zA-Z0-9._%+-]+@srmap\.edu\.in$/.test(email)) {
  throw err(400, 'Use an email ending in @srmap.edu.in.');
 } 

  if (
    password.length < 8 ||
    !/[A-Z]/.test(password) ||
    !/[a-z]/.test(password) ||
    !/[0-9]/.test(password) ||
    !/[^A-Za-z0-9]/.test(password)
  ) {
    throw err(
      400,
      'Password must have at least 8 characters, uppercase, lowercase, a number and a special character.'
    );
  }

  if (db.users.some((u) => u.email.toLowerCase() === email)) {
    throw err(409, 'An account with this email already exists.');
  }

  const u = {
    id: nextId(),
    name,
    email,
    role: 'STUDENT',
    active: true,
    password
  };

  db.users.push(u);
  save();

  return {
    token: 'mock-' + u.id,
    user: publicUser(u)
  };
});

  r('GET', '/auth/me', [], ({ user }) => publicUser(user));

  r('GET', '/faculty', [], () => db.users.filter((u) => u.role === 'FACULTY' && u.active).map(publicFaculty));
  r('GET', '/faculty/:id/availability', [], ({ params, query }) => availability('faculty', Number(params.id), query.date));
  r('GET', '/rooms', [], ({ query }) => db.rooms.filter((x) => x.active && (!query.type || x.type === query.type)));
  r('GET', '/rooms/:id/availability', [], ({ params, query }) => availability('room', Number(params.id), query.date));
  r('GET', '/resources', [], () => db.resources.filter((x) => x.active));
  r('GET', '/resources/:id/availability', [], ({ params, query }) => availability('resource', Number(params.id), query.date));

  // Combined faculty + room availability: the intersection of both free lists.
  r('POST', '/availability/combined', ['STUDENT', 'FACULTY', 'ADMIN'], ({ body }) => {
    const { facultyId, needRoom, roomId, roomType, block, floor, minCapacity, dateFrom, dateTo, durationMinutes } = body;
    const duration = Number(durationMinutes);
    if (!facultyId || !dateFrom || !dateTo || !(duration > 0)) throw err(400, 'Faculty, date range and duration are required.');
    const span = S.daysBetween(dateFrom, dateTo);
    if (span < 0) throw err(400, 'The end date must not be before the start date.');
    if (span >= window.APP_CONFIG.MAX_SEARCH_DAYS) throw err(400, `Search at most ${window.APP_CONFIG.MAX_SEARCH_DAYS} days at a time.`);
    if (!entity('faculty', facultyId)) throw err(404, 'Faculty member not found.');

    let rooms = [];
    if (needRoom) {
      rooms = db.rooms.filter((x) => x.active
        && (!roomId || x.id === Number(roomId))
        && (!roomType || x.type === roomType)
        && inBlock(x, block)
        && inFloor(x, floor)
        && (!minCapacity || x.capacity >= Number(minCapacity)));
    }
    const windows = [];
    for (let i = 0; i <= span; i++) {
      const date = S.addDays(dateFrom, i);
      const f = availability('faculty', Number(facultyId), date).free;
      if (!needRoom) {
        f.filter((w) => S.toMin(w.end) - S.toMin(w.start) >= duration)
          .forEach((w) => windows.push({ date, ...w, roomId: null, roomName: null, capacity: null }));
        continue;
      }
      let roomsUsed = 0;
      for (const room of rooms) {
        if (!roomId && !floor && roomsUsed >= MAX_ROOMS_PER_DAY) break;
        const found = S.intersect(f, availability('room', room.id, date).free)
          .filter((w) => S.toMin(w.end) - S.toMin(w.start) >= duration);
        if (found.length) roomsUsed++;
        found.forEach((w) => windows.push({ date, ...w, roomId: room.id, roomName: room.name, roomLocation: room.building, capacity: room.capacity }));
      }
    }
    return { durationMinutes: duration, windows: windows.slice(0, 300) };
  });

  // Appointments
  r('POST', '/appointments', ['STUDENT'], ({ body, user }) => {
    const { facultyId, roomId, date, start, end, purpose } = body;
    checkTimes(date, start, end);
    if (!purpose || String(purpose).trim().length < 3) throw err(400, 'Add a short purpose for the meeting.');
    if (!entity('faculty', facultyId)) throw err(404, 'Faculty member not found.');
    if (!isFree('faculty', Number(facultyId), date, start, end)) throw err(409, 'The faculty member is no longer available at that time.');
    if (roomId) {
      if (!entity('room', roomId)) throw err(404, 'Room not found.');
      if (!isFree('room', Number(roomId), date, start, end)) throw err(409, 'The room is no longer available at that time.');
    }
    const a = { id: nextId(), studentId: user.id, facultyId: Number(facultyId), roomId: roomId ? Number(roomId) : null, date, start, end, purpose: String(purpose).trim(), status: 'PENDING' };
    db.appointments.push(a); save();
    return enrichAppt(a);
  });
  r('GET', '/appointments/mine', ['STUDENT'], ({ user }) => db.appointments.filter((a) => a.studentId === user.id).sort(sortByTime).map(enrichAppt));
  r('GET', '/appointments/incoming', ['FACULTY'], ({ user }) => db.appointments.filter((a) => a.facultyId === user.id).sort(sortByTime).map(enrichAppt));
  r('PATCH', '/appointments/:id/status', ['FACULTY'], ({ params, body, user }) => {
    const a = byId(db.appointments, params.id);
    if (!a || a.facultyId !== user.id) throw err(404, 'Appointment not found.');
    if (!['CONFIRMED', 'DECLINED'].includes(body.status)) throw err(400, 'Status must be CONFIRMED or DECLINED.');
    if (a.status !== 'PENDING') throw err(409, 'Only pending requests can be answered.');
    a.status = body.status; save();
    return enrichAppt(a);
  });
  r('PATCH', '/appointments/:id/cancel', [], ({ params, user }) => {
    const a = byId(db.appointments, params.id);
    if (!a) throw err(404, 'Appointment not found.');
    const owns = a.studentId === user.id || a.facultyId === user.id || user.role === 'ADMIN';
    if (!owns) throw err(403, 'You cannot cancel this appointment.');
    if (!ACTIVE.includes(a.status)) throw err(409, 'This appointment is already closed.');
    a.status = 'CANCELLED'; save();
    return enrichAppt(a);
  });

  // Room / resource reservations
  r('POST', '/bookings', ['STUDENT', 'FACULTY'], ({ body, user }) => {
    const { roomId, date, start, end, purpose } = body;
    const resourceIds = (body.resourceIds || []).map(Number);
    checkTimes(date, start, end);
    if (!roomId && !resourceIds.length) throw err(400, 'Choose a room or at least one resource.');
    if (!purpose || String(purpose).trim().length < 3) throw err(400, 'Add a short purpose for the reservation.');
    if (roomId) {
      if (!entity('room', roomId)) throw err(404, 'Room not found.');
      if (!isFree('room', Number(roomId), date, start, end)) throw err(409, 'The room is no longer available at that time.');
    }
    for (const id of resourceIds) {
      const res = entity('resource', id);
      if (!res) throw err(404, 'Resource not found.');
      if (!isFree('resource', id, date, start, end)) throw err(409, `${res.name} is not available at that time.`);
    }
    const b = { id: nextId(), userId: user.id, roomId: roomId ? Number(roomId) : null, resourceIds, date, start, end, purpose: String(purpose).trim(), status: 'CONFIRMED' };
    db.bookings.push(b); save();
    return enrichBooking(b);
  });
  r('GET', '/bookings/mine', [], ({ user }) => db.bookings.filter((b) => b.userId === user.id).sort(sortByTime).map(enrichBooking));
  r('PATCH', '/bookings/:id/cancel', [], ({ params, user }) => {
    const b = byId(db.bookings, params.id);
    if (!b) throw err(404, 'Reservation not found.');
    if (b.userId !== user.id && user.role !== 'ADMIN') throw err(403, 'You cannot cancel this reservation.');
    if (b.status !== 'CONFIRMED') throw err(409, 'This reservation is already cancelled.');
    b.status = 'CANCELLED'; save();
    return enrichBooking(b);
  });

  // Faculty schedules (faculty: own; admin: anyone)
  const scheduleOwner = (params, user) => {
    const id = Number(params.id);
    if (user.role !== 'ADMIN' && user.id !== id) throw err(403, 'You can only manage your own schedule.');
    const f = entity('faculty', id);
    if (!f) throw err(404, 'Faculty member not found.');
    return f;
  };
  r('GET', '/faculty/:id/schedule', ['FACULTY', 'ADMIN'], ({ params, user }) => {
    const f = scheduleOwner(params, user);
    return { workingHours: clone(f.workingHours), blocks: clone(f.blocks) };
  });
  r('PUT', '/faculty/:id/schedule/working-hours', ['FACULTY', 'ADMIN'], ({ params, body, user }) => {
    const f = scheduleOwner(params, user);
    const wh = {};
    for (let d = 0; d < 7; d++) {
      const list = (body.workingHours?.[d] || []).map((x) => ({ start: x.start, end: x.end }));
      list.forEach((x) => checkTimes('2000-01-01', x.start, x.end));
      const sorted = list.sort((a, b) => S.toMin(a.start) - S.toMin(b.start));
      for (let i = 1; i < sorted.length; i++) if (S.overlaps(sorted[i - 1], sorted[i])) throw err(400, 'Working hours on one day must not overlap.');
      wh[d] = sorted;
    }
    f.workingHours = wh; save();
    return { workingHours: clone(wh) };
  });
  r('POST', '/faculty/:id/schedule/blocks', ['FACULTY', 'ADMIN'], ({ params, body, user }) => {
    const f = scheduleOwner(params, user);
    checkTimes(body.date, body.start, body.end);
    const clash = db.appointments.some((a) => a.facultyId === f.id && a.date === body.date && ACTIVE.includes(a.status) && S.overlaps(a, body));
    if (clash) throw err(409, 'That time overlaps an existing appointment. Cancel or move the appointment first.');
    const block = { id: nextId(), date: body.date, start: body.start, end: body.end, reason: String(body.reason || '').trim() };
    f.blocks.push(block); save();
    return block;
  });
  r('DELETE', '/faculty/:id/schedule/blocks/:blockId', ['FACULTY', 'ADMIN'], ({ params, user }) => {
    const f = scheduleOwner(params, user);
    f.blocks = f.blocks.filter((b) => b.id !== Number(params.blockId)); save();
    return {};
  });

  // Administration
  r('GET', '/admin/stats', ['ADMIN'], () => {
    const t = S.todayStr();
    return {
      activeUsers: db.users.filter((u) => u.active).length,
      facultyCount: db.users.filter((u) => u.role === 'FACULTY' && u.active).length,
      studentCount: db.users.filter((u) => u.role === 'STUDENT' && u.active).length,
      rooms: db.rooms.filter((x) => x.active).length,
      resources: db.resources.filter((x) => x.active).length,
      pendingAppointments: db.appointments.filter((a) => a.status === 'PENDING').length,
      upcomingAppointments: db.appointments.filter((a) => ACTIVE.includes(a.status) && a.date >= t).length,
      upcomingReservations: db.bookings.filter((b) => b.status === 'CONFIRMED' && b.date >= t).length
    };
  });
  r('GET', '/admin/appointments', ['ADMIN'], () => db.appointments.slice().sort(sortByTime).map(enrichAppt));
  r('GET', '/admin/bookings', ['ADMIN'], () => db.bookings.slice().sort(sortByTime).map(enrichBooking));

  r('GET', '/admin/users', ['ADMIN'], () => db.users.map(adminUser));
  r('POST', '/admin/users', ['ADMIN'], ({ body }) => {
  const name = String(body.name || '').trim();
  const email = String(body.email || '').trim().toLowerCase();
  const password = String(body.password || '');
  const role = body.role;

  if (
    name.length < 2 ||
    !['STUDENT', 'FACULTY', 'ADMIN'].includes(role)
  ) {
    throw err(400, 'Name and a valid role are required.');
  }

  if (!/^[a-zA-Z0-9._%+-]+@srmap\.edu\.in$/.test(email)) {
    throw err(400, 'Use an email ending in @srmap.edu.in.');
  }

  if (
    password.length < 8 ||
    !/[A-Z]/.test(password) ||
    !/[a-z]/.test(password) ||
    !/[0-9]/.test(password) ||
    !/[^A-Za-z0-9]/.test(password)
  ) {
    throw err(400, 'Password must have at least 8 characters, uppercase, lowercase, a number and a special character.');
  }

  if (db.users.some((u) => u.email.toLowerCase() === email)) {
    throw err(409, 'A user with this email already exists.');
  }

  const u = {
    id: nextId(),
    name,
    email,
    role,
    active: body.active !== false,
    password
  };

  if (u.role === 'FACULTY') {
    Object.assign(u, {
      department: body.department || '',
      office: body.office || '',
      title: body.title || '',
      subjects: body.subjects || [],
      workingHours: weekdays([{ start: '09:00', end: '17:00' }]),
      blocks: []
    });
  }

  db.users.push(u);
  save();

  return adminUser(u);
});

  r('PUT', '/admin/users/:id', ['ADMIN'], ({ params, body, user }) => {
    const u = byId(db.users, params.id);
    if (!u) throw err(404, 'User not found.');
    if (db.users.some((x) => x.id !== u.id && x.email.toLowerCase() === String(body.email).toLowerCase())) throw err(409, 'A user with this email already exists.');
    if (u.id === user.id && (body.active === false || body.role !== u.role)) throw err(409, 'You cannot deactivate or change the role of your own account.');
    Object.assign(u, { name: body.name, email: body.email, role: body.role, active: body.active !== false });
    if (body.password) { if (body.password.length < 8) throw err(400, 'Password must be at least 8 characters.'); u.password = body.password; }
    if (u.role === 'FACULTY') {
      Object.assign(u, { department: body.department || '', office: body.office || '', title: body.title || '', subjects: body.subjects || u.subjects || [] });
      u.workingHours = u.workingHours || weekdays([{ start: '09:00', end: '17:00' }]);
      u.blocks = u.blocks || [];
    }
    save();
    return adminUser(u);
  });

  // A campus room is described by its block and floor; the building text is built from them.
  const applyLocation = (row) => {
    if (!row.block) { delete row.block; delete row.floor; return row; }
    if (row.floor !== '' && row.floor != null) { row.floor = Number(row.floor); row.building = `${row.block}, Floor ${row.floor}`; }
    return row;
  };

  for (const [name, list, required] of [['rooms', 'rooms', ['name', 'type', 'capacity']], ['resources', 'resources', ['name']]]) {
    r('GET', `/admin/${name}`, ['ADMIN'], () => clone(db[list]));
    r('POST', `/admin/${name}`, ['ADMIN'], ({ body }) => {
      if (required.some((k) => !body[k])) throw err(400, 'Please fill in all required fields.');
      const row = { ...body, id: nextId(), active: body.active !== false };
      if (name === 'rooms') applyLocation(row);
      db[list].push(row); save();
      return clone(row);
    });
    r('PUT', `/admin/${name}/:id`, ['ADMIN'], ({ params, body }) => {
      const row = byId(db[list], params.id);
      if (!row) throw err(404, 'Not found.');
      if (required.some((k) => !body[k])) throw err(400, 'Please fill in all required fields.');
      Object.assign(row, body, { id: row.id, active: body.active !== false });
      if (name === 'rooms') applyLocation(row);
      save();
      return clone(row);
    });
  }

  async function handle(method, fullPath, body, token) {
    await sleep(120);
    const [path, qs] = fullPath.split('?');
    const query = Object.fromEntries(new URLSearchParams(qs || ''));
    const route = routes.find((x) => x.method === method && x.re.test(path));
    if (!route) throw err(404, 'Not found.');
    let user = null;
    if (route.roles !== null) {
      const m = /^mock-(\d+)$/.exec(token || '');
      user = m ? byId(db.users, m[1]) : null;
      if (!user || !user.active) throw err(401, 'Please sign in again.');
      if (route.roles.length && !route.roles.includes(user.role)) throw err(403, 'You do not have access to this.');
    }
    const params = route.re.exec(path).groups || {};
    return clone(await route.fn({ params, query, body: body || {}, user }));
  }

  function reset() { localStorage.removeItem(KEY); db = seed(); save(); }

  return { handle, reset, generateCampusRooms };
})();