#!/usr/bin/env node

import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';

const args = parseArgs(process.argv.slice(2));
const host = args.host ?? '0.0.0.0';
const port = parsePort(args.port ?? '39393');
const storeDir = path.resolve(args['store-dir'] ?? path.join(os.homedir(), '.openclaw', 'clawchat-media-store'));
const filesDir = path.join(storeDir, 'files');
const metaDir = path.join(storeDir, 'meta');
const stateFile = path.resolve(args['state-file'] ?? path.join(storeDir, 'server.json'));

fs.mkdirSync(filesDir, { recursive: true });
fs.mkdirSync(metaDir, { recursive: true });

const server = http.createServer((req, res) => {
  const originHost = req.headers.host ?? `127.0.0.1:${port}`;
  const requestUrl = new URL(req.url ?? '/', `http://${originHost}`);

  if (req.method === 'GET' && requestUrl.pathname === '/health') {
    writeJson(res, 200, {
      ok: true,
      host,
      port,
      storeDir,
      pid: process.pid,
    });
    return;
  }

  if (req.method === 'GET' && requestUrl.pathname.startsWith('/media/')) {
    const token = decodeURIComponent(requestUrl.pathname.slice('/media/'.length));
    if (!token || path.basename(token) !== token) {
      writeJson(res, 400, { ok: false, error: 'invalid media token' });
      return;
    }

    const metadataPath = path.join(metaDir, `${token}.json`);
    const mediaPath = path.join(filesDir, token);
    if (!fs.existsSync(metadataPath) || !fs.existsSync(mediaPath)) {
      writeJson(res, 404, { ok: false, error: 'media not found' });
      return;
    }

    const metadata = JSON.parse(fs.readFileSync(metadataPath, 'utf8'));
    const stat = fs.statSync(mediaPath);
    const rangeHeader = req.headers.range;
    if (rangeHeader) {
      const parsedRange = parseRangeHeader(rangeHeader, stat.size);
      if (!parsedRange) {
        res.writeHead(416, {
          'Content-Range': `bytes */${stat.size}`,
          'Accept-Ranges': 'bytes',
        });
        res.end();
        return;
      }

      const { start, end } = parsedRange;
      res.writeHead(206, {
        'Content-Type': metadata.mimeType ?? 'application/octet-stream',
        'Content-Length': String(end - start + 1),
        'Content-Range': `bytes ${start}-${end}/${stat.size}`,
        'Cache-Control': 'public, max-age=31536000, immutable',
        'Accept-Ranges': 'bytes',
        ETag: metadata.sha256 ?? token,
        'X-ClawChat-Media-Token': token,
      });
      fs.createReadStream(mediaPath, { start, end }).pipe(res);
      return;
    }

    res.writeHead(200, {
      'Content-Type': metadata.mimeType ?? 'application/octet-stream',
      'Content-Length': String(stat.size),
      'Cache-Control': 'public, max-age=31536000, immutable',
      'Accept-Ranges': 'bytes',
      ETag: metadata.sha256 ?? token,
      'X-ClawChat-Media-Token': token,
    });
    fs.createReadStream(mediaPath).pipe(res);
    return;
  }

  writeJson(res, 404, { ok: false, error: 'not found' });
});

server.listen(port, host, () => {
  fs.writeFileSync(
    stateFile,
    JSON.stringify(
      {
        pid: process.pid,
        host,
        port,
        storeDir,
        startedAt: new Date().toISOString(),
      },
      null,
      2,
    ) + '\n',
    'utf8',
  );
  process.stdout.write(
    `${JSON.stringify({ ok: true, pid: process.pid, host, port, storeDir, stateFile })}\n`,
  );
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    server.close(() => process.exit(0));
  });
}

function writeJson(res, statusCode, payload) {
  res.writeHead(statusCode, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(`${JSON.stringify(payload)}\n`);
}

function parseArgs(argv) {
  const parsed = {};
  for (let i = 0; i < argv.length; i += 1) {
    const part = argv[i];
    if (!part.startsWith('--')) fail(`Unknown argument: ${part}`);
    const key = part.slice(2);
    const next = argv[i + 1];
    if (!next || next.startsWith('--')) {
      parsed[key] = 'true';
      continue;
    }
    parsed[key] = next;
    i += 1;
  }
  return parsed;
}

function parsePort(value) {
  const parsed = Number.parseInt(String(value), 10);
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 65535) {
    fail(`Invalid port: ${value}`);
  }
  return parsed;
}

function parseRangeHeader(rangeHeader, totalSize) {
  const match = /^bytes=(\d*)-(\d*)$/i.exec(String(rangeHeader).trim());
  if (!match) return null;

  const [, startText, endText] = match;
  if (startText === '' && endText === '') return null;

  if (startText === '') {
    const suffixLength = Number.parseInt(endText, 10);
    if (!Number.isInteger(suffixLength) || suffixLength <= 0) return null;
    const start = Math.max(totalSize - suffixLength, 0);
    return { start, end: totalSize - 1 };
  }

  const start = Number.parseInt(startText, 10);
  if (!Number.isInteger(start) || start < 0 || start >= totalSize) return null;

  const end =
    endText === ''
      ? totalSize - 1
      : Number.parseInt(endText, 10);

  if (!Number.isInteger(end) || end < start) return null;
  return { start, end: Math.min(end, totalSize - 1) };
}

function fail(message) {
  process.stderr.write(`${message}\n`);
  process.exit(1);
}
