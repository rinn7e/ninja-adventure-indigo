// A live game session: a headed browser kept open, driven over HTTP.
//   /key?k=ArrowUp&ms=500   hold a key (k=Space for space), returns after release
//   /press?k=Enter          tap a key
//   /shot?name=x            screenshot to out/live-x.png
//   /route?r=...            drive a route printed by route.py ("Keys:ms Keys:ms ...")
//   /wait?ms=500            wait
//   /reload                 reload the page
// Needs Playwright (npm install playwright) and the game served on localhost:8787.
const { chromium } = require('playwright');
const http = require('http');
(async () => {
  const b = await chromium.launch({ headless: false });
  const p = await b.newPage({ viewport: { width: 1280, height: 720 } });
  const errs = [];
  p.on('pageerror', e => errs.push(e.message));
  p.on('console', m => { if (m.type() === 'error' && !m.text().includes('404')) errs.push(m.text()); });
  await p.goto('http://localhost:8787');
  http.createServer(async (req, res) => {
    const u = new URL(req.url, 'http://x');
    const k = u.searchParams.get('k') === 'Space' ? ' ' : u.searchParams.get('k');
    try {
      if (u.pathname === '/key') {
        const ks = k.split('+');
        for (const x of ks) await p.keyboard.down(x);
        await p.waitForTimeout(+u.searchParams.get('ms'));
        for (const x of ks) await p.keyboard.up(x);
      }
      else if (u.pathname === '/route') {
        // "Keys:ms Keys:ms ..." as printed by route.py
        for (const leg of u.searchParams.get('r').split(' ')) {
          const [ks, ms] = leg.split(':');
          for (const x of ks.split('+')) await p.keyboard.down(x);
          await p.waitForTimeout(+ms);
          for (const x of ks.split('+')) await p.keyboard.up(x);
        }
      }
      else if (u.pathname === '/press') { await p.keyboard.press(k); }
      else if (u.pathname === '/shot') { await p.screenshot({ path: `out/live-${u.searchParams.get('name')}.png` }); }
      else if (u.pathname === '/reload') { await p.reload(); errs.length = 0; }
      else if (u.pathname === '/wait') { await p.waitForTimeout(+u.searchParams.get('ms')); }
      res.end(JSON.stringify({ ok: true, errors: errs }));
    } catch (e) { res.end(JSON.stringify({ ok: false, error: String(e) })); }
  }).listen(9555);
  console.log('live on 9555');
})();
