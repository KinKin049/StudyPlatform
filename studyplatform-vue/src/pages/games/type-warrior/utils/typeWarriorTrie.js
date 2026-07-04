function createTrieNode() {
  return {
    children: Object.create(null),
    enemyIds: [],
  }
}

export function buildEnemyKeywordTrie(enemies) {
  const root = createTrieNode()

  for (const enemy of enemies) {
    if (!enemy.keyword) continue

    let node = root
    for (const letter of enemy.keyword) {
      node.children[letter] ||= createTrieNode()
      node = node.children[letter]
      node.enemyIds.push(enemy.id)
    }
  }

  return root
}

function isBetterMatch(left, right) {
  if (!right) return true

  const matchDiff = left.matchLength - right.matchLength
  if (matchDiff !== 0) return matchDiff > 0

  const distanceDiff = left.distance - right.distance
  if (distanceDiff !== 0) return distanceDiff < 0

  return left.enemy.keyword.length < right.enemy.keyword.length
}

export function findBestTrieSuffixMatch(root, buffer, evaluateCandidate) {
  let bestMatch = null

  for (let startIndex = 0; startIndex < buffer.length; startIndex += 1) {
    let node = root

    for (let index = startIndex; index < buffer.length; index += 1) {
      node = node.children[buffer[index]]
      if (!node) break

      const matchLength = index - startIndex + 1
      for (const enemyId of node.enemyIds) {
        const candidate = evaluateCandidate(enemyId, matchLength)
        if (candidate && isBetterMatch(candidate, bestMatch)) {
          bestMatch = candidate
        }
      }
    }
  }

  return bestMatch
}
