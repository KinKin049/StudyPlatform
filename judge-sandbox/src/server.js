import { createServer } from 'node:http'
import { mkdir, rm, writeFile } from 'node:fs/promises'
import { createWriteStream } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { spawn } from 'node:child_process'
import { randomUUID } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const port = Number(process.env.PORT || 9000)
const currentDir = dirname(fileURLToPath(import.meta.url))
const sandboxRoot = resolve(currentDir, '..')
const bundledCompiler = process.platform === 'win32'
  ? join(sandboxRoot, 'toolchains', 'mingw64', 'bin', 'g++.exe')
  : join(sandboxRoot, 'toolchains', 'mingw64', 'bin', 'g++')
const compiler = process.env.CPP_COMPILER || bundledCompiler
const workRoot = resolve(process.env.JUDGE_WORK_DIR || join(tmpdir(), 'studyplatform-judge'))
const maxBodyBytes = Number(process.env.MAX_BODY_BYTES || 1024 * 1024)
const compileTimeoutMs = Number(process.env.COMPILE_TIMEOUT_MS || 10000)

const server = createServer(async (request, response) => {
  try {
    if (request.method === 'GET' && request.url === '/health') {
      return sendJson(response, 200, { status: 'ok' })
    }

    if (request.method !== 'POST' || request.url !== '/judge') {
      return sendJson(response, 404, { message: 'Not found' })
    }

    const payload = await readJsonBody(request)
    const result = await judge(payload)
    return sendJson(response, 200, result)
  } catch (error) {
    return sendJson(response, 500, {
      status: 'SYSTEM_ERROR',
      score: 0,
      timeUsedMs: null,
      memoryUsedKb: null,
      message: error instanceof Error ? error.message : 'Unknown judge error',
      cases: [],
    })
  }
})

server.listen(port, () => {
  console.log(`Judge sandbox listening on http://localhost:${port}`)
  console.log(`C++ compiler: ${compiler}`)
  console.log(`Work root: ${workRoot}`)
})

async function judge(payload) {
  if (!['cpp', 'c++'].includes(String(payload.language || '').toLowerCase())) {
    return systemError(`Unsupported language: ${payload.language}`)
  }

  const cases = Array.isArray(payload.cases) ? payload.cases : []
  if (cases.length === 0) {
    return systemError('No test cases provided')
  }

  const workDir = join(workRoot, randomUUID())
  await mkdir(workDir, { recursive: true })

  try {
    const sourcePath = join(workDir, 'main.cpp')
    const executablePath = process.platform === 'win32' ? join(workDir, 'main.exe') : join(workDir, 'main')
    await writeFile(sourcePath, String(payload.sourceCode || ''), 'utf8')

    const compileResult = await compileCpp(sourcePath, executablePath, workDir)

    if (compileResult.spawnError) {
      return aggregateResult(
        'SYSTEM_ERROR',
        0,
        null,
        null,
        `C++ compiler is not available: ${compileResult.stderr}`,
        [],
      )
    }

    if (compileResult.timedOut) {
      return aggregateResult('COMPILE_ERROR', 0, null, null, 'Compile timeout', [])
    }

    if (compileResult.exitCode !== 0) {
      return aggregateResult(
        'COMPILE_ERROR',
        0,
        null,
        null,
        truncate(compileResult.stderr || compileResult.stdout || 'Compile error'),
        [],
      )
    }

    const caseResults = []
    let acceptedWeight = 0
    let totalWeight = 0
    let maxTimeUsedMs = 0

    for (const testCase of cases) {
      const weight = Number(testCase.weight || 1)
      totalWeight += weight

      const runResult = await runProcess(executablePath, [], {
        cwd: workDir,
        input: String(testCase.inputData || ''),
        timeoutMs: Number(payload.timeLimitMs || 1000),
      })

      const caseStatus = toCaseStatus(runResult, testCase.expectedOutput)
      if (caseStatus === 'ACCEPTED') {
        acceptedWeight += weight
      }

      maxTimeUsedMs = Math.max(maxTimeUsedMs, runResult.timeUsedMs)
      caseResults.push({
        testCaseId: testCase.testCaseId,
        status: caseStatus,
        timeUsedMs: runResult.timeUsedMs,
        memoryUsedKb: null,
        message: caseMessage(caseStatus, runResult),
      })
    }

    const score = totalWeight === 0 ? 0 : Math.floor((acceptedWeight * 100) / totalWeight)
    const status = caseResults.every((item) => item.status === 'ACCEPTED')
      ? 'ACCEPTED'
      : firstFailedStatus(caseResults)

    return aggregateResult(status, score, maxTimeUsedMs, null, summaryMessage(status), caseResults)
  } finally {
    await rm(workDir, { recursive: true, force: true })
  }
}

function toCaseStatus(runResult, expectedOutput) {
  if (runResult.timedOut) return 'TIME_LIMIT_EXCEEDED'
  if (runResult.exitCode !== 0) return 'RUNTIME_ERROR'
  return normalizeOutput(runResult.stdout) === normalizeOutput(String(expectedOutput || ''))
    ? 'ACCEPTED'
    : 'WRONG_ANSWER'
}

function caseMessage(status, runResult) {
  if (status === 'ACCEPTED') return 'Accepted'
  if (status === 'WRONG_ANSWER') return 'Wrong answer'
  if (status === 'TIME_LIMIT_EXCEEDED') return 'Time limit exceeded'
  if (status === 'RUNTIME_ERROR') return truncate(runResult.stderr || 'Runtime error')
  return status
}

function firstFailedStatus(caseResults) {
  return caseResults.find((item) => item.status !== 'ACCEPTED')?.status || 'SYSTEM_ERROR'
}

function summaryMessage(status) {
  if (status === 'ACCEPTED') return 'Accepted'
  if (status === 'WRONG_ANSWER') return 'Wrong answer'
  if (status === 'TIME_LIMIT_EXCEEDED') return 'Time limit exceeded'
  if (status === 'RUNTIME_ERROR') return 'Runtime error'
  return status
}

function systemError(message) {
  return aggregateResult('SYSTEM_ERROR', 0, null, null, message, [])
}

async function compileCpp(sourcePath, executablePath, workDir) {
  const attempts = [
    ['-std=c++17', '-O2', '-pipe', sourcePath, '-o', executablePath],
    ['-std=c++11', '-O2', '-pipe', sourcePath, '-o', executablePath],
    ['-O2', '-pipe', sourcePath, '-o', executablePath],
  ]

  let lastResult = null
  for (const args of attempts) {
    const result = await runProcess(compiler, args, {
      cwd: workDir,
      timeoutMs: compileTimeoutMs,
    })
    if (result.spawnError || result.timedOut || result.exitCode === 0) {
      return result
    }
    lastResult = result
  }
  return lastResult
}

function aggregateResult(status, score, timeUsedMs, memoryUsedKb, message, cases) {
  return {
    status,
    score,
    timeUsedMs,
    memoryUsedKb,
    message,
    cases,
  }
}

function runProcess(command, args, options) {
  return new Promise((resolveProcess) => {
    let startedAt = null
    let stdout = ''
    let stderr = ''
    let settled = false
    let timedOut = false

    const child = spawn(command, args, {
      cwd: options.cwd,
      windowsHide: true,
      stdio: ['pipe', 'pipe', 'pipe'],
    })

    child.once('spawn', () => {
      startedAt = performance.now()
    })

    const timer = setTimeout(() => {
      timedOut = true
      child.kill('SIGKILL')
    }, options.timeoutMs)

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString('utf8')
    })

    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString('utf8')
    })

    child.on('error', (error) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      resolveProcess({
        exitCode: -1,
        stdout,
        stderr: error.message,
        spawnError: true,
        timedOut,
        timeUsedMs: elapsedMs(startedAt),
      })
    })

    child.on('close', (exitCode) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      resolveProcess({
        exitCode,
        stdout,
        stderr,
        spawnError: false,
        timedOut,
        timeUsedMs: elapsedMs(startedAt),
      })
    })

    if (options.input) {
      child.stdin.end(options.input)
    } else {
      child.stdin.end()
    }
  })
}

function elapsedMs(startedAt) {
  const start = startedAt ?? performance.now()
  return Math.max(1, Math.round(performance.now() - start))
}

function normalizeOutput(output) {
  return output
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/[ \t]+(?=\n)/g, '')
    .trim()
}

function truncate(value, maxLength = 1000) {
  return String(value).length > maxLength ? `${String(value).slice(0, maxLength)}...` : String(value)
}

function readJsonBody(request) {
  return new Promise((resolveBody, rejectBody) => {
    let body = ''
    request.on('data', (chunk) => {
      body += chunk.toString('utf8')
      if (Buffer.byteLength(body, 'utf8') > maxBodyBytes) {
        request.destroy()
        rejectBody(new Error('Request body is too large'))
      }
    })
    request.on('end', () => {
      try {
        resolveBody(JSON.parse(body || '{}'))
      } catch {
        rejectBody(new Error('Invalid JSON request body'))
      }
    })
    request.on('error', rejectBody)
  })
}

function sendJson(response, statusCode, payload) {
  response.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
  })
  response.end(JSON.stringify(payload))
}
