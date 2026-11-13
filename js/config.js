/**
 * Frontend configuration.
 *
 * USE_MOCK = true  -> runs entirely in the browser against js/mock.js (UI preview only).
 * USE_MOCK = false -> talks to the Spring Boot backend at API_BASE.
 *
 * You can also override without editing this file:
 *   index.html?mock=0   (use the real backend)
 *   index.html?mock=1   (use the demo data)
 */
(function () {
  const params = new URLSearchParams(location.search);
  const cfg = {
    APP_NAME: 'Smart Faculty and Resource Booking System',
    BRAND: 'Smart Faculty Booking',
    API_BASE: 'http://localhost:8081/api',
    USE_MOCK: false,
    DAY_START: 8 * 60,   // timeline start (minutes from midnight)
    DAY_END: 20 * 60,    // timeline end
    SLOT_STEP: 30,       // minutes between suggested start times
    MAX_SEARCH_DAYS: 14  // keep in sync with the backend limit
  };
  if (params.has('mock')) cfg.USE_MOCK = params.get('mock') !== '0';
  window.APP_CONFIG = cfg;
})();