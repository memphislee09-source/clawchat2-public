#!/usr/bin/env node

import { spawn } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const usage = `
Usage:
  scripts/send-clawchat-media.mjs --file <path> [options]

Options:
  --file <path>           Media file to send. Required.
  --agent <id>            Agent id. Default: main
  --session-key <key>     Full session key. Default: agent:<agentId>:clawchat2
  --message <text>        Optional caption text.
  --type <kind>           image | audio | video. Auto-detected by default.
  --mime <mime>           Override MIME type.
  --name <fileName>       Override file name in chat.
  --sender <name>         Sender label metadata. Default: current agent id
  --public-host <host>    Host used inside mediaUrl. Default: 10.0.2.2
  --bind-host <host>      Host the local media server binds to. Default: 0.0.0.0
  --port <port>           Media server port. Default: 39393
  --store-dir <path>      Media store dir. Default: ~/.openclaw/clawchat-media-store
  --refresh-hint          Print a manual refresh hint. Default: true
  -h, --help              Show help
`;

const args = parseArgs(process.argv.slice(2));
if (args.help) {
  process.stdout.write(usage.trimStart());
  process.exit(0);
}

await main(args);

async function main(parsedArgs) {
  const filePath = parsedArgs.file;
  if (!filePath) fail('Missing --file');

  const resolvedFilePath = path.resolve(filePath);
  if (!fs.existsSync(resolvedFilePath)) fail(`File not found: ${resolvedFilePath}`);

  const agentId = parsedArgs.agent ?? 'main';
  const sessionKey = parsedArgs.sessionKey ?? `agent:${agentId}:clawchat2`;
  const sender = parsedArgs.sender ?? agentId;
  const publicHost = parsedArgs['public-host'] ?? '10.0.2.2';
  const bindHost = parsedArgs['bind-host'] ?? '0.0.0.0';
  const port = parsePort(parsedArgs.port ?? '39393');

  const mediaBytes = fs.readFileSync(resolvedFilePath);
  if (mediaBytes.length === 0) fail(`File is empty: ${resolvedFilePath}`);

  const detectedMime = parsedArgs.mime ?? detectMimeType(resolvedFilePath);
  const detectedType = parsedArgs.type ?? detectAttachmentKind(detectedMime, resolvedFilePath);
  if (!detectedType) {
    fail(
      `Could not determine attachment type for ${resolvedFilePath}. ` +
        'Use --type image|audio|video or --mime <mime>.',
    );
  }

  const openClawDir = path.join(os.homedir(), '.openclaw');
  const storeDir = path.resolve(parsedArgs['store-dir'] ?? path.join(openClawDir, 'clawchat-media-store'));
  const filesDir = path.join(storeDir, 'files');
  const metaDir = path.join(storeDir, 'meta');
  const serverStatePath = path.join(storeDir, 'server.json');
  fs.mkdirSync(filesDir, { recursive: true });
  fs.mkdirSync(metaDir, { recursive: true });

  const fileName = parsedArgs.name ?? path.basename(resolvedFilePath);
  const sha256 = crypto.createHash('sha256').update(mediaBytes).digest('hex');
  const extension = resolveStoredExtension(fileName, resolvedFilePath, detectedMime, detectedType);
  const token = `${sha256}.${extension}`;
  const storedFilePath = path.join(filesDir, token);
  const metadataPath = path.join(metaDir, `${token}.json`);
  if (!fs.existsSync(storedFilePath) || fs.statSync(storedFilePath).size !== mediaBytes.length) {
    fs.copyFileSync(resolvedFilePath, storedFilePath);
  }
  fs.writeFileSync(
    metadataPath,
    JSON.stringify(
      {
        token,
        type: detectedType,
        mimeType: detectedMime,
        fileName,
        sha256,
        sizeBytes: mediaBytes.length,
        sourcePath: resolvedFilePath,
        storedFilePath,
        createdAt: new Date().toISOString(),
      },
      null,
      2,
    ) + '\n',
    'utf8',
  );

  await ensureMediaServer({
    bindHost,
    port,
    publicHost,
    storeDir,
    stateFile: serverStatePath,
  });

  const sessionsDir = path.join(openClawDir, 'agents', agentId, 'sessions');
  const sessionsIndexPath = path.join(sessionsDir, 'sessions.json');
  if (!fs.existsSync(sessionsIndexPath)) fail(`Session index not found: ${sessionsIndexPath}`);

  const sessionsIndex = JSON.parse(fs.readFileSync(sessionsIndexPath, 'utf8'));
  const nowMs = Date.now();
  const entry = ensureSessionEntry({
    sessionsIndex,
    sessionsDir,
    sessionKey,
    nowMs,
  });

  const transcriptPath = entry.sessionFile;
  const lastMessageId = findLastRecordId(transcriptPath);
  const messageId = randomId();
  const mediaUrl = `http://${publicHost}:${port}/media/${encodeURIComponent(token)}`;
  const content = [];

  if (parsedArgs.message) {
    content.push({
      type: 'text',
      text: parsedArgs.message,
    });
  }

  content.push({
    type: detectedType,
    mimeType: detectedMime,
    fileName,
    mediaUrl,
    mediaSha256: sha256,
    sizeBytes: mediaBytes.length,
  });

  const record = {
    type: 'message',
    id: messageId,
    parentId: lastMessageId ?? null,
    timestamp: new Date(nowMs).toISOString(),
    message: {
      role: 'assistant',
      content,
      timestamp: nowMs,
      stopReason: 'stop',
      usage: {
        input: 0,
        output: 0,
        cacheRead: 0,
        cacheWrite: 0,
        totalTokens: 0,
        cost: {
          input: 0,
          output: 0,
          cacheRead: 0,
          cacheWrite: 0,
          total: 0,
        },
      },
      api: 'openclaw',
      provider: 'clawchat-media-script',
      model: 'local-media-ref-injected',
      senderLabel: sender,
    },
  };

  appendJsonLine(transcriptPath, record);

  entry.updatedAt = nowMs;
  entry.systemSent = true;
  entry.abortedLastRun = false;
  entry.lastChannel = entry.lastChannel ?? 'webchat';
  entry.deliveryContext = entry.deliveryContext ?? { channel: 'webchat' };
  entry.origin =
    entry.origin ?? {
      provider: 'webchat',
      surface: 'webchat',
      chatType: 'direct',
      label: sender,
    };
  entry.chatType = entry.chatType ?? 'direct';

  writeJsonAtomic(sessionsIndexPath, sessionsIndex);

  const result = {
    ok: true,
    protocol: 'clawchat-media-ref-v1',
    agentId,
    sessionKey,
    sessionId: entry.sessionId,
    transcriptPath,
    messageId,
    attachment: {
      type: detectedType,
      mimeType: detectedMime,
      fileName,
      sizeBytes: mediaBytes.length,
      sha256,
      mediaUrl,
    },
    mediaServer: {
      bindHost,
      publicHost,
      port,
      storeDir,
      stateFile: serverStatePath,
    },
    refreshHint:
      parsedArgs.refreshHint === 'false'
        ? null
        : 'If ClawChat2 is already open on this chat, pull down to refresh or reopen the chat once.',
  };

  process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
}

function parseArgs(argv) {
  const parsed = {};
  for (let i = 0; i < argv.length; i += 1) {
    const part = argv[i];
    if (part === '-h' || part === '--help') {
      parsed.help = true;
      continue;
    }
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

async function ensureMediaServer({ bindHost, port, publicHost, storeDir, stateFile }) {
  const publicBaseUrl = `http://${publicHost}:${port}`;
  const localHealthUrl = `http://127.0.0.1:${port}`;
  if (await isServerHealthy(localHealthUrl)) return;

  const serverScript = path.join(__dirname, 'clawchat-media-server.mjs');
  const logPath = path.join(storeDir, 'server.log');
  const outFd = fs.openSync(logPath, 'a');
  const child = spawn(
    process.execPath,
    [
      serverScript,
      '--host',
      bindHost,
      '--port',
      String(port),
      '--store-dir',
      storeDir,
      '--state-file',
      stateFile,
    ],
    {
      detached: true,
      stdio: ['ignore', outFd, outFd],
    },
  );
  child.unref();
  fs.closeSync(outFd);

  for (let attempt = 0; attempt < 25; attempt += 1) {
    await sleep(200);
    if (await isServerHealthy(localHealthUrl)) return;
  }

  fail(`Media server did not become ready at ${publicBaseUrl}`);
}

function isServerHealthy(baseUrl) {
  return new Promise((resolve) => {
    const req = http.get(`${baseUrl}/health`, (res) => {
      res.resume();
      resolve(res.statusCode === 200);
    });
    req.on('error', () => resolve(false));
    req.setTimeout(750, () => {
      req.destroy();
      resolve(false);
    });
  });
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function ensureSessionEntry({ sessionsIndex, sessionsDir, sessionKey, nowMs }) {
  let entry = sessionsIndex[sessionKey];
  if (entry) return entry;

  const sessionId = crypto.randomUUID();
  const transcriptPath = path.join(sessionsDir, `${sessionId}.jsonl`);
  fs.mkdirSync(sessionsDir, { recursive: true });
  fs.closeSync(fs.openSync(transcriptPath, 'a'));

  entry = {
    sessionId,
    updatedAt: nowMs,
    systemSent: true,
    abortedLastRun: false,
    chatType: 'direct',
    deliveryContext: {
      channel: 'webchat',
    },
    lastChannel: 'webchat',
    origin: {
      provider: 'webchat',
      surface: 'webchat',
      chatType: 'direct',
    },
    sessionFile: transcriptPath,
    compactionCount: 0,
  };
  sessionsIndex[sessionKey] = entry;
  return entry;
}

function findLastRecordId(transcriptPath) {
  if (!fs.existsSync(transcriptPath)) return null;
  const lines = fs.readFileSync(transcriptPath, 'utf8').trim().split('\n').filter(Boolean);
  for (let index = lines.length - 1; index >= 0; index -= 1) {
    try {
      const row = JSON.parse(lines[index]);
      if (typeof row.id === 'string' && row.id.length > 0) return row.id;
    } catch {
      // Skip malformed historical lines.
    }
  }
  return null;
}

function appendJsonLine(targetPath, value) {
  fs.mkdirSync(path.dirname(targetPath), { recursive: true });
  fs.appendFileSync(targetPath, `${JSON.stringify(value)}\n`, 'utf8');
}

function writeJsonAtomic(targetPath, value) {
  const tempPath = `${targetPath}.tmp-${process.pid}-${Date.now()}`;
  fs.writeFileSync(tempPath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
  fs.renameSync(tempPath, targetPath);
}

function detectMimeType(filePathValue) {
  const extension = path.extname(filePathValue).toLowerCase();
  switch (extension) {
    case '.jpg':
    case '.jpeg':
      return 'image/jpeg';
    case '.png':
      return 'image/png';
    case '.webp':
      return 'image/webp';
    case '.gif':
      return 'image/gif';
    case '.mp3':
      return 'audio/mpeg';
    case '.m4a':
    case '.aac':
      return 'audio/mp4';
    case '.wav':
      return 'audio/wav';
    case '.ogg':
      return 'audio/ogg';
    case '.mp4':
      return 'video/mp4';
    case '.mov':
      return 'video/quicktime';
    case '.webm':
      return 'video/webm';
    default:
      return 'application/octet-stream';
  }
}

function detectAttachmentKind(mimeType, filePathValue) {
  if (mimeType.startsWith('image/')) return 'image';
  if (mimeType.startsWith('audio/')) return 'audio';
  if (mimeType.startsWith('video/')) return 'video';

  const extension = path.extname(filePathValue).toLowerCase();
  if (['.jpg', '.jpeg', '.png', '.webp', '.gif'].includes(extension)) return 'image';
  if (['.mp3', '.m4a', '.aac', '.wav', '.ogg'].includes(extension)) return 'audio';
  if (['.mp4', '.mov', '.webm'].includes(extension)) return 'video';
  return null;
}

function resolveStoredExtension(fileName, filePathValue, mimeType, attachmentKind) {
  const byName = path.basename(fileName).split('.').pop()?.trim().toLowerCase();
  if (byName && byName !== path.basename(fileName).toLowerCase()) return byName;

  const byPath = path.extname(filePathValue).slice(1).trim().toLowerCase();
  if (byPath) return byPath;

  switch (mimeType) {
    case 'image/jpeg':
      return 'jpg';
    case 'image/png':
      return 'png';
    case 'image/webp':
      return 'webp';
    case 'audio/mpeg':
      return 'mp3';
    case 'audio/mp4':
      return 'm4a';
    case 'audio/wav':
      return 'wav';
    case 'video/mp4':
      return 'mp4';
    default:
      return attachmentKind === 'image' ? 'img' : attachmentKind === 'audio' ? 'audio' : 'bin';
  }
}

function parsePort(value) {
  const parsed = Number.parseInt(String(value), 10);
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > 65535) {
    fail(`Invalid port: ${value}`);
  }
  return parsed;
}

function randomId() {
  return crypto.randomBytes(4).toString('hex');
}

function fail(message) {
  process.stderr.write(`${message}\n`);
  process.exit(1);
}
