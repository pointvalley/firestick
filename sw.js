// Firestick Home service worker: makes the page installable and opens it offline.
// Pages are network-first (sandbox changes show up right away); the small app shell is cached.
// Video wallpapers are never cached (too large, and they use range requests).
const CACHE = 'firestick-home-v1';
const SHELL = ['./', 'manifest.json', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).catch(() => {}).then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener('fetch', e => {
  const req = e.request, url = new URL(req.url);
  if (req.method !== 'GET' || url.origin !== location.origin) return;
  if (req.headers.has('range') || /\.(mp4|webm)$/i.test(url.pathname)) return;

  const isPage = req.mode === 'navigate' || (req.headers.get('accept') || '').includes('text/html');
  if (isPage) {
    e.respondWith(fetch(req).then(res => {
      if (res.ok) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(req, copy)); }
      return res;
    }).catch(() => caches.match(req, { ignoreSearch: true }).then(r => r || caches.match('./'))));
    return;
  }
  const inShell = SHELL.some(p => p !== './' && url.pathname.endsWith('/' + p));
  if (inShell) e.respondWith(caches.match(req).then(r => r || fetch(req)));
});
