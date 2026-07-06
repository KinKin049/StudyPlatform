import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const seedPath = path.join(root, 'StudyPlatform-back/src/main/resources/db/migration/V8__create_and_seed_textbooks.sql')
const outputPath = path.join(root, 'StudyPlatform-back/src/main/resources/db/migration/V45__update_textbook_real_overview_catalog.sql')
const origin = 'https://www.icourse163.org'

const seedSql = fs.readFileSync(seedPath, 'utf8')
const rows = parseSeedRows(seedSql)
const token = await loadMoocToken(rows[0]?.moocTextbookId)
const fetched = []

for (const [index, row] of rows.entries()) {
  try {
    const detail = await fetchTextbookInfo(row.moocTextbookId, token)
    const overview = normalizeText(stripHtml(detail.shortDesc || ''))
    const catalog = flattenCatalog(detail.catalogList || []).join('\n')
    fetched.push({
      id: row.id,
      recommendation: overview || row.description,
      overview: overview || row.description,
      catalog,
      originalPrice: toPrice(detail.originalPrice, 69),
      discountPrice: toPrice(detail.price, 49),
      readerCount: Number.isFinite(Number(detail.readCount)) ? Number(detail.readCount) : 0,
    })
    console.log(`[${index + 1}/${rows.length}] ok ${row.id} ${row.name}`)
    await delay(80)
  } catch (error) {
    fetched.push({
      id: row.id,
      recommendation: row.description,
      overview: row.description,
      catalog: '',
      originalPrice: 69,
      discountPrice: 49,
      readerCount: 0,
    })
    console.warn(`[${index + 1}/${rows.length}] fallback ${row.id} ${row.name}: ${error.message}`)
  }
}

fs.writeFileSync(outputPath, buildMigration(fetched), 'utf8')
console.log(`wrote ${outputPath}`)

function parseSeedRows(sql) {
  const valuesStart = sql.indexOf('VALUES')
  const valuesEnd = sql.indexOf('ON DUPLICATE KEY UPDATE', valuesStart)
  if (valuesStart < 0 || valuesEnd < 0) {
    throw new Error('Cannot locate excellent_textbooks VALUES block.')
  }
  const tuples = []
  const source = sql.slice(valuesStart + 'VALUES'.length, valuesEnd)
  let current = []
  let value = ''
  let inString = false
  let inTuple = false

  for (let index = 0; index < source.length; index += 1) {
    const char = source[index]
    const next = source[index + 1]
    if (!inTuple) {
      if (char === '(') {
        inTuple = true
        current = []
        value = ''
      }
      continue
    }
    if (inString) {
      if (char === "'" && next === "'") {
        value += "'"
        index += 1
      } else if (char === "'") {
        inString = false
      } else {
        value += char
      }
      continue
    }
    if (char === "'") {
      inString = true
    } else if (char === ',') {
      current.push(value.trim())
      value = ''
    } else if (char === ')') {
      current.push(value.trim())
      tuples.push(current)
      inTuple = false
      value = ''
    } else {
      value += char
    }
  }

  return tuples
    .map((tuple) => {
      const sourceUrl = tuple[10] || ''
      const moocTextbookId = (sourceUrl.match(/textbook\/(\d+)\.htm/) || [])[1]
      return {
        id: tuple[0],
        name: tuple[1],
        description: tuple[9],
        sourceUrl,
        moocTextbookId,
      }
    })
    .filter((row) => row.id && row.moocTextbookId)
}

async function loadMoocToken(textbookId) {
  const response = await fetch(`${origin}/textbook/${textbookId}.htm`, {
    headers: { 'user-agent': userAgent() },
  })
  const setCookie = response.headers.get('set-cookie') || ''
  const token = (setCookie.match(/NTESSTUDYSI=([^;]+)/) || [])[1]
  if (!token) {
    throw new Error('Cannot read NTESSTUDYSI token from MOOC.')
  }
  return token
}

async function fetchTextbookInfo(textbookId, token) {
  const url = `${origin}/web/j/textbookBean.getTextbookInfo.rpc?csrfKey=${encodeURIComponent(token)}`
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'content-type': 'application/x-www-form-urlencoded;charset=utf-8',
      cookie: `NTESSTUDYSI=${token}`,
      'edu-script-token': token,
      origin,
      referer: `${origin}/textbook/${textbookId}.htm`,
      'user-agent': userAgent(),
    },
    body: `textbookId=${encodeURIComponent(textbookId)}`,
  })
  const payload = await response.json()
  if (payload.code !== 0 || !payload.result) {
    throw new Error(payload.message || `MOOC response code ${payload.code}`)
  }
  return payload.result
}

function flattenCatalog(items, depth = 0) {
  return items.flatMap((item) => {
    const title = normalizeText(item?.title || '')
    const line = title ? `${'　'.repeat(depth)}${title}` : ''
    return [
      ...(line ? [line] : []),
      ...flattenCatalog(Array.isArray(item?.childs) ? item.childs : [], depth + 1),
    ]
  })
}

function stripHtml(value) {
  return String(value)
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>\s*<p[^>]*>/gi, '\n')
    .replace(/<\/?[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
}

function normalizeText(value) {
  return String(value)
    .replace(/\r/g, '')
    .replace(/\t/g, ' ')
    .split('\n')
    .map((line) => line.replace(/\s+/g, ' ').trim())
    .filter(Boolean)
    .join('\n')
}

function toPrice(value, fallback) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue.toFixed(2) : fallback.toFixed(2)
}

function buildMigration(rows) {
  const values = rows
    .map((row) => `  (${[
      sql(row.id),
      sql(row.recommendation),
      row.originalPrice,
      row.discountPrice,
      Number(row.readerCount) || 0,
      sql(row.overview),
      sql(row.catalog),
    ].join(', ')})`)
    .join(',\n')

  return `INSERT INTO academy_textbook_details
  (textbook_id, recommendation, original_price, discount_price, reader_count, overview, catalog_text)
VALUES
${values}
ON DUPLICATE KEY UPDATE
  recommendation = VALUES(recommendation),
  original_price = VALUES(original_price),
  discount_price = VALUES(discount_price),
  reader_count = VALUES(reader_count),
  overview = VALUES(overview),
  catalog_text = VALUES(catalog_text),
  crawled_at = CURRENT_TIMESTAMP;
`
}

function sql(value) {
  return `'${String(value ?? '').replace(/\\/g, '\\\\').replace(/'/g, "''")}'`
}

function delay(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function userAgent() {
  return 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36'
}
