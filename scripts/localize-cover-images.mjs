import fs from 'node:fs/promises'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(scriptDir, '..')
const backendDir = path.join(rootDir, 'StudyPlatform-back')
const storageDir = path.join(backendDir, 'storage')

const jsonFiles = [
  'studyplatform-vue/src/data/onlineOpenCourses.json',
  'studyplatform-vue/src/data/generalCourses.json',
  'studyplatform-vue/src/data/microMajorCourses.json',
  'studyplatform-vue/src/data/textbooks.json',
]

const sqlFiles = [
  'StudyPlatform-back/src/main/resources/db/migration/V5__seed_online_open_courses.sql',
  'StudyPlatform-back/src/main/resources/db/migration/V6__create_and_seed_general_courses.sql',
  'StudyPlatform-back/src/main/resources/db/migration/V7__create_and_seed_micro_major_courses.sql',
  'StudyPlatform-back/src/main/resources/db/migration/V8__create_and_seed_textbooks.sql',
  'StudyPlatform-back/src/main/resources/db/migration/V15__create_course_question_bank_catalog.sql',
  'StudyPlatform-back/src/main/resources/db/migration/V16__seed_more_course_question_banks.sql',
  'StudyPlatform-back/src/main/resources/db/migration/V17__update_public_english_qualification_question_banks.sql',
]

const migrationFile = 'StudyPlatform-back/src/main/resources/db/migration/V56__localize_seed_cover_urls.sql'
const shouldDownload = process.argv.includes('--download')
const shouldWrite = !process.argv.includes('--check')

const stats = {
  jsonUrls: 0,
  sqlUrls: 0,
  existing: 0,
  downloaded: 0,
  missing: [],
}

function isExternalImageUrl(value) {
  return typeof value === 'string' && /^https?:\/\/.+\.(?:jpg|jpeg|png|webp)(?:[?#].*)?$/i.test(value)
}

function normalizeStoragePath(value) {
  if (!value || typeof value !== 'string') return ''
  return value.replace(/\\/g, '/').replace(/^\/+/, '')
}

function storagePathToDisk(value) {
  const normalized = normalizeStoragePath(value)
  const relativePath = normalized.startsWith('storage/') ? normalized.slice('storage/'.length) : normalized
  return path.join(storageDir, ...relativePath.split('/'))
}

function storagePathToPublicUrl(value) {
  const normalized = normalizeStoragePath(value)
  const relativePath = normalized.startsWith('storage/') ? normalized.slice('storage/'.length) : normalized
  return `/files/${relativePath
    .split('/')
    .map((part) => encodeURIComponent(part).replace(/%20/g, '%20'))
    .join('/')}`
}

async function ensureLocalImage(url, storagePath) {
  const target = storagePathToDisk(storagePath)
  if (existsSync(target)) {
    stats.existing += 1
    return
  }

  if (!shouldDownload) {
    stats.missing.push({ url, storagePath })
    return
  }

  await fs.mkdir(path.dirname(target), { recursive: true })
  const response = await fetch(url, {
    redirect: 'follow',
    headers: {
      'User-Agent': 'Mozilla/5.0 local-cover-migration',
    },
  })
  if (!response.ok) {
    throw new Error(`Failed to download ${url}: ${response.status} ${response.statusText}`)
  }
  const bytes = Buffer.from(await response.arrayBuffer())
  await fs.writeFile(target, bytes)
  stats.downloaded += 1
}

function walk(value, visitor) {
  if (Array.isArray(value)) {
    value.forEach((item) => walk(item, visitor))
    return
  }
  if (!value || typeof value !== 'object') return
  visitor(value)
  Object.values(value).forEach((child) => walk(child, visitor))
}

async function localizeJsonFile(relativeFile) {
  const absoluteFile = path.join(rootDir, relativeFile)
  const source = await fs.readFile(absoluteFile, 'utf8')
  const data = JSON.parse(source)

  const tasks = []
  walk(data, (item) => {
    const externalCover = isExternalImageUrl(item.coverUrl)
      ? item.coverUrl
      : isExternalImageUrl(item.originalCover)
        ? item.originalCover
        : ''
    if (!externalCover || !item.coverFilePath) return
    stats.jsonUrls += 1
    const localUrl = storagePathToPublicUrl(item.coverFilePath)
    tasks.push(ensureLocalImage(externalCover, item.coverFilePath))
    item.cover = localUrl
    if (Object.hasOwn(item, 'coverUrl')) {
      item.coverUrl = localUrl
    }
    if (Object.hasOwn(item, 'originalCover')) {
      item.originalCover = localUrl
    }
  })

  await Promise.all(tasks)
  if (shouldWrite) {
    await fs.writeFile(absoluteFile, `${JSON.stringify(data, null, 2)}\n`, 'utf8')
  }
}

async function localizeSqlFile(relativeFile) {
  const absoluteFile = path.join(rootDir, relativeFile)
  let source = await fs.readFile(absoluteFile, 'utf8')
  const tasks = []
  const imageAndPath =
    /'((?:https?:\/\/)[^']+\.(?:jpg|jpeg|png|webp)(?:\?[^']*)?)'(\s*(?:AS\s+cover_url)?\s*,\s*)'(storage\/[^']+\.(?:jpg|jpeg|png|webp))'/gi

  source = source.replace(imageAndPath, (match, url, separator, storagePath) => {
    stats.sqlUrls += 1
    tasks.push(ensureLocalImage(url, storagePath))
    return `'${storagePathToPublicUrl(storagePath)}'${separator}'${storagePath}'`
  })

  await Promise.all(tasks)
  if (shouldWrite) {
    await fs.writeFile(absoluteFile, source, 'utf8')
  }
}

async function writeMigration() {
  const migrationSql = `UPDATE online_open_courses
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';

UPDATE general_courses
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';

UPDATE micro_major_courses
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';

UPDATE excellent_textbooks
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';

UPDATE course_question_bank_sets
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';
`
  await fs.writeFile(path.join(rootDir, migrationFile), migrationSql, 'utf8')
}

for (const file of jsonFiles) {
  await localizeJsonFile(file)
}
for (const file of sqlFiles) {
  await localizeSqlFile(file)
}
if (shouldWrite) {
  await writeMigration()
}

console.log(JSON.stringify(stats, null, 2))
if (stats.missing.length > 0) {
  process.exitCode = 2
}
