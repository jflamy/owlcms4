const http = require('http');
const fs = require('fs');
const path = require('path');

const repository = path.resolve(__dirname, '../..');

function parsePort() {
  const portArgument = process.argv.indexOf('--port');
  const port = Number(portArgument >= 0 ? process.argv[portArgument + 1]
    : process.env.SCOREBOARD_FIXTURE_PORT || 60745);
  if (!Number.isInteger(port) || port < 0 || port > 65535) throw new Error('Invalid fixture port');
  return port;
}

for (const module of ['lit', 'lit-html', 'lit-element', '@lit/reactive-element']) {
  if (!fs.existsSync(path.join(repository, 'owlcms/node_modules', module))) {
    throw new Error(`Missing ${module}; restore the existing owlcms frontend dependencies first`);
  }
}

const routes = {
  '/fixture/': __dirname,
  '/components/': path.join(repository, 'owlcms/src/main/frontend/components'),
  '/local/css/': path.join(repository, 'shared/src/main/resources/css'),
  '/local/logos/': path.join(repository, 'shared/src/main/resources/logos'),
  '/modules/': path.join(repository, 'owlcms/node_modules')
};
const modulePrefixes = ['lit/', 'lit-html/', 'lit-element/', '@lit/reactive-element/'];
const contentTypes = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml', '.png': 'image/png'
};

const server = http.createServer((request, response) => {
  const pathname = new URL(request.url, 'http://localhost').pathname;
  let file;
  if (pathname === '/') file = path.join(__dirname, 'index.html');
  else {
    for (const [prefix, directory] of Object.entries(routes)) {
      if (!pathname.startsWith(prefix)) continue;
      const relative = pathname.slice(prefix.length);
      const resolved = path.resolve(directory, relative);
      if (!resolved.startsWith(directory + path.sep)
          || (prefix === '/modules/' && !modulePrefixes.some(p => relative.startsWith(p)))) {
        response.writeHead(403); response.end('Forbidden asset path'); return;
      }
      file = resolved;
      break;
    }
  }
  const contentType = file && contentTypes[path.extname(file)];
  if (!file || !contentType) {
    response.writeHead(404); response.end('Unknown fixture asset'); return;
  }
  fs.readFile(file, (error, data) => {
    if (error) {
      console.error(`Fixture asset unavailable: ${pathname}: ${error.message}`);
      response.writeHead(404); response.end('Fixture asset unavailable'); return;
    }
    response.writeHead(200, {'Content-Type': contentType, 'Cache-Control': 'no-store'});
    response.end(data);
  });
});

/** Starts the fixture server; resolves with the base URL. Port 0 picks a free port. */
function start(port) {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(port, '127.0.0.1', () => resolve(`http://127.0.0.1:${server.address().port}/`));
  });
}

module.exports = {start, server};

if (require.main === module) {
  start(parsePort())
    .then(url => console.log(`Scoreboard rendering fixtures: ${url}`))
    .catch(error => { console.error(error); process.exitCode = 1; });
}
