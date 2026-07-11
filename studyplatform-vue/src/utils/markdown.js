const allowedMessageTags = new Set([
  'A',
  'BLOCKQUOTE',
  'BR',
  'CODE',
  'EM',
  'HR',
  'LI',
  'OL',
  'P',
  'PRE',
  'S',
  'STRONG',
  'UL',
])

const allowedLinkProtocols = new Set(['http:', 'https:', 'mailto:'])

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInlineMarkdown(text) {
  return escapeHtml(text)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_]+)__/g, '<strong>$1</strong>')
    .replace(/~~([^~]+)~~/g, '<s>$1</s>')
    .replace(/\*([^*\n]+)\*/g, '<em>$1</em>')
    .replace(/_([^_\n]+)_/g, '<em>$1</em>')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+|mailto:[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
    .replace(/(^|[\s>])((?:https?:\/\/)[^\s<]+)/g, '$1<a href="$2" target="_blank" rel="noopener noreferrer">$2</a>')
}

function renderSimpleMarkdown(text) {
  const source = String(text || '').replace(/\r\n/g, '\n')
  const codeBlocks = []
  const withoutCodeBlocks = source.replace(/```([\s\S]*?)```/g, (_, code) => {
    const index = codeBlocks.push(`<pre><code>${escapeHtml(code.replace(/^\n|\n$/g, ''))}</code></pre>`) - 1
    return `\n@@AI_MESSAGE_CODE_BLOCK_${index}@@\n`
  })

  const blocks = withoutCodeBlocks
    .split(/\n{2,}/)
    .map((block) => block.trim())
    .filter(Boolean)
    .map((block) => {
      const codeMatch = block.match(/^@@AI_MESSAGE_CODE_BLOCK_(\d+)@@$/)
      if (codeMatch) {
        return codeBlocks[Number(codeMatch[1])] || ''
      }
      return `<p>${renderInlineMarkdown(block).replace(/\n/g, '<br>')}</p>`
    })

  return blocks.join('')
}

function sanitizeMessageHtml(html) {
  if (typeof document === 'undefined') return String(html || '')
  const template = document.createElement('template')
  template.innerHTML = String(html || '')

  template.content.querySelectorAll('*').forEach((element) => {
    if (!allowedMessageTags.has(element.tagName)) {
      element.replaceWith(...Array.from(element.childNodes))
      return
    }

    Array.from(element.attributes).forEach((attribute) => {
      const name = attribute.name.toLowerCase()
      if (element.tagName === 'A' && ['href', 'target', 'rel'].includes(name)) {
        return
      }
      element.removeAttribute(attribute.name)
    })

    if (element.tagName === 'A') {
      const href = element.getAttribute('href') || ''
      let safeHref = false
      try {
        const parsedUrl = new URL(href, window.location.origin)
        safeHref = allowedLinkProtocols.has(parsedUrl.protocol)
      } catch {
        safeHref = false
      }
      if (!safeHref) {
        element.removeAttribute('href')
      }
      element.setAttribute('target', '_blank')
      element.setAttribute('rel', 'noopener noreferrer')
    }
  })

  return template.innerHTML
}

export function renderMessageMarkdown(text) {
  return sanitizeMessageHtml(renderSimpleMarkdown(text))
}
