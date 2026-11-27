import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildRowWordGroups,
  buildShareText,
  filterWordleSuggestions,
  formatWordleDateLabel,
  highlightWordPrefix,
  keyboardKeyStates,
  resolveWordleAccess,
  wordleHubSummary,
  wordleToastMessage,
  wordShape,
} from './wordle.js'
import {
  fetchWordleDictionary,
  fetchWordleInfo,
  fetchWordleStats,
  fetchWordleToday,
  isValidWordleDictionary,
  isValidWordleInfo,
  isValidWordleStats,
  isValidWordleToday,
  revealWordleHint,
  submitWordleGuess,
} from '../api/wordle.js'

const serverTime = '2026-11-27T12:00:00Z'
const nextAt = '2026-11-27T17:00:00Z'

function sampleStats(overrides = {}) {
  return {
    played: 9,
    wins: 8,
    winPercentage: 89,
    currentStreak: 5,
    maxStreak: 7,
    distribution: [0, 1, 3, 2, 1, 1],
    ...overrides,
  }
}

function sampleToday(overrides = {}) {
  return {
    puzzleNumber: 12,
    date: '2026-11-27',
    length: 7,
    serverTime,
    nextAt,
    status: 'PLAYING',
    hintUsed: false,
    hintClub: null,
    guesses: [
      {
        turn: 1,
        word: 'BARCOLA',
        letters: 'BARCOLA',
        feedback: 'MNMMMMN',
        playerName: 'Bradley Barcola',
        club: 'Liverpool',
      },
    ],
    answer: null,
    stats: sampleStats(),
    ...overrides,
  }
}

async function withFetch(mock, run) {
  const previous = globalThis.fetch
  globalThis.fetch = mock
  try { await run() } finally { globalThis.fetch = previous }
}

const json = (data, status = 200) =>
  new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })

test('wordShape and buildRowWordGroups preserve multi-word and single-word tile shapes', () => {
  assert.deepEqual(wordShape('VAN DIJK'), [3, 4])
  assert.deepEqual(wordShape('BARCOLA'), [7])
  assert.deepEqual(wordShape('DE LA CRUZ'), [2, 2, 4])

  const multi = buildRowWordGroups({ word: 'VAN DIJK', letters: 'VANDIJK', feedback: 'HHHHHHH', length: 7 })
  assert.equal(multi.length, 2)
  assert.deepEqual(multi[0].map(t => [t.letter, t.state, t.index]), [
    ['V', 'hit', 0], ['A', 'hit', 1], ['N', 'hit', 2],
  ])
  assert.deepEqual(multi[1].map(t => [t.letter, t.state, t.index]), [
    ['D', 'hit', 3], ['I', 'hit', 4], ['J', 'hit', 5], ['K', 'hit', 6],
  ])

  const active = buildRowWordGroups({ letters: 'VAN', length: 7 })
  assert.equal(active.length, 1)
  assert.equal(active[0].length, 7)
  assert.deepEqual(active[0].map(t => t.state), ['filled', 'filled', 'filled', 'empty', 'empty', 'empty', 'empty'])
})

test('highlightWordPrefix handles spaces across multi-word names', () => {
  assert.deepEqual(highlightWordPrefix('VAN DIJK', 'VAN'), { matched: 'VAN', rest: ' DIJK' })
  assert.deepEqual(highlightWordPrefix('VAN DIJK', 'VAND'), { matched: 'VAN D', rest: 'IJK' })
  assert.deepEqual(highlightWordPrefix('DE LA CRUZ', 'DELAC'), { matched: 'DE LA C', rest: 'RUZ' })
  assert.deepEqual(highlightWordPrefix('BARCOLA', 'BA'), { matched: 'BA', rest: 'RCOLA' })
})

test('filterWordleSuggestions excludes guessed names, sorts A-Z, and caps at 5', () => {
  const dict = [
    { letters: 'BRAYDEN', word: 'BRAYDEN', name: 'Player 6', club: 'Club F' },
    { letters: 'BRADLEY', word: 'BRADLEY', name: 'Conor Bradley', club: 'Liverpool' },
    { letters: 'BROBBEY', word: 'BROBBEY', name: 'Brian Brobbey', club: 'Sunderland' },
    { letters: 'BARCOLA', word: 'BARCOLA', name: 'Bradley Barcola', club: 'Liverpool' },
    { letters: 'BALLARD', word: 'BALLARD', name: 'Daniel Ballard', club: 'Sunderland' },
    { letters: 'BUENDIA', word: 'BUENDIA', name: 'Emiliano Buendía', club: 'Aston Villa' },
    { letters: 'BALEBAA', word: 'BALEBAA', name: 'Carlos Baleba', club: 'Brighton' },
    { letters: 'BENNETT', word: 'BENNETT', name: 'Player 7', club: 'Club G' },
  ]
  const results = filterWordleSuggestions(dict, 'b', ['BARCOLA'], 5, 7)
  assert.deepEqual(
    results.map(item => item.letters),
    ['BALEBAA', 'BALLARD', 'BENNETT', 'BRADLEY', 'BRAYDEN']
  )
  assert.equal(results[0].matched, 'B')
  assert.equal(results[0].rest, 'ALEBAA')
  assert.deepEqual(filterWordleSuggestions(dict, '', [], 5, 7), [])
})

test('keyboardKeyStates keeps best state per letter (hit > near > miss)', () => {
  const states = keyboardKeyStates([
    { letters: 'BARCOLA', feedback: 'MNMMMMN' },
    { letters: 'HAALAND', feedback: 'MHMMMNH' },
    { letters: 'VANDIJK', feedback: 'HHNHMMM' },
  ])
  assert.equal(states.A, 'hit')
  assert.equal(states.N, 'near')
  assert.equal(states.D, 'hit')
  assert.equal(states.B, 'miss')
  assert.equal(states.V, 'hit')
})

test('buildShareText preserves multi-word spaces, omits player names, and prints X/6 on loss', () => {
  const winText = buildShareText({
    puzzleNumber: 12,
    status: 'WON',
    guesses: [
      { word: 'BARCOLA', letters: 'BARCOLA', feedback: 'MNMMMMN', playerName: 'Bradley Barcola', club: 'Liverpool' },
      { word: 'VAN DIJK', letters: 'VANDIJK', feedback: 'HHHHHHH', playerName: 'Virgil van Dijk', club: 'Liverpool' },
    ],
  })
  assert.equal(winText, 'PrismaXI Wordle #12 2/6\n⬛🟨⬛⬛⬛⬛🟨\n🟩🟩🟩 🟩🟩🟩🟩')
  assert.equal(winText.includes('DIJK'), false)
  assert.equal(winText.includes('Barcola'), false)

  const lossText = buildShareText({
    puzzleNumber: 12,
    status: 'LOST',
    guesses: [
      { word: 'VAN BERG', letters: 'VANBERG', feedback: 'HHHMMMM', playerName: 'Test', club: 'Club' },
    ],
  })
  assert.equal(lossText, 'PrismaXI Wordle #12 X/6\n🟩🟩🟩 ⬛⬛⬛⬛')
})

test('formatWordleDateLabel, wordleToastMessage, and wordleHubSummary format Vietnam labels accurately', () => {
  assert.equal(formatWordleDateLabel('2026-11-27'), 'T6 27/11')
  assert.equal(formatWordleDateLabel('2026-11-29'), 'CN 29/11')
  assert.equal(wordleToastMessage('WRONG_LENGTH', 7), 'Cần đủ 7 chữ cái')
  assert.equal(wordleToastMessage('NOT_A_PLAYER', 7), 'Không có cầu thủ Premier League nào tên như vậy')
  assert.equal(wordleToastMessage('ALREADY_GUESSED', 7), 'Bạn đã đoán tên này rồi')

  assert.equal(wordleHubSummary(null, { loading: false, data: null }).statusText, 'Đăng nhập để chơi')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: false, data: sampleToday({ guesses: [] }) }).statusText, 'Chưa chơi')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: false, data: sampleToday() }).statusText, 'Đang chơi dở · lượt 2/6')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: false, data: sampleToday({ status: 'WON' }) }).statusText, 'Đã đoán ra · 1/6')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: false, data: sampleToday({ status: 'LOST' }) }).statusText, 'Hết lượt')
})

test('API validators verify TodayView, DictionaryEntry, and StatsView contracts', () => {
  assert.equal(isValidWordleToday(sampleToday()), true)
  // PLAYING must not leak answer
  assert.equal(isValidWordleToday(sampleToday({
    answer: { playerId: 1, name: 'Virgil van Dijk', clubId: 1, club: 'Liverpool', shirtNumber: 4, position: 'CB', nationality: 'Netherlands', age: 35, overall: 88, word: 'VAN DIJK' },
  })), false)
  // WON must include answer
  assert.equal(isValidWordleToday(sampleToday({ status: 'WON', answer: null })), false)
  assert.equal(isValidWordleToday(sampleToday({
    status: 'WON',
    hintUsed: true,
    hintClub: 'Liverpool',
    answer: { playerId: 1, name: 'Virgil van Dijk', clubId: 1, club: 'Liverpool', shirtNumber: 4, position: 'CB', nationality: 'Netherlands', age: 35, overall: 88, word: 'VAN DIJK' },
  })), true)
  // hintUsed=false must not leak hintClub
  assert.equal(isValidWordleToday(sampleToday({ hintUsed: false, hintClub: 'Liverpool' })), false)

  assert.equal(isValidWordleDictionary([
    { letters: 'VANDIJK', word: 'VAN DIJK', name: 'Virgil van Dijk', club: 'Liverpool' },
  ]), true)
  assert.equal(isValidWordleDictionary([
    { letters: 'VANDIJK', word: 'BARCOLA', name: 'Virgil van Dijk', club: 'Liverpool' },
  ]), false)

  assert.equal(isValidWordleStats(sampleStats()), true)
  assert.equal(isValidWordleStats(sampleStats({ distribution: [0, 1, 2] })), false)
})

test('Wordle API client sends account header, CSRF token on POST, and preserves 404/domain error codes', async () => {
  await withFetch(async (url, options) => {
    assert.equal(url, '/api/minigame/2026/wordle/today')
    assert.equal(options.credentials, 'include')
    assert.equal(options.headers['X-PrismaXI-Account-ID'], '42')
    return json(sampleToday())
  }, async () => {
    const today = await fetchWordleToday(42)
    assert.equal(today.puzzleNumber, 12)
  })

  await withFetch(async (url, options) => {
    if (url === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'tok-1' })
    assert.equal(url, '/api/minigame/2026/wordle/today/guesses')
    assert.equal(options.method, 'POST')
    assert.equal(options.headers['X-CSRF-TOKEN'], 'tok-1')
    assert.equal(options.headers['X-PrismaXI-Account-ID'], '42')
    assert.deepEqual(JSON.parse(options.body), { letters: 'HAALAND', date: '2026-11-27' })
    return json({ code: 'NOT_A_PLAYER', message: 'Không hợp lệ' }, 422)
  }, async () => {
    await assert.rejects(
      submitWordleGuess(42, 'HAALAND', { date: '2026-11-27' }),
      err => err.status === 422 && err.code === 'NOT_A_PLAYER'
    )
  })

  await withFetch(async (url, options) => {
    if (url === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'tok-2' })
    assert.equal(url, '/api/minigame/2026/wordle/today/hint')
    assert.equal(options.method, 'POST')
    return json(sampleToday({ hintUsed: true, hintClub: 'Liverpool' }))
  }, async () => {
    const res = await revealWordleHint(42, { date: '2026-11-27' })
    assert.equal(res.hintClub, 'Liverpool')
  })

  await withFetch(async url => {
    if (url === '/api/minigame/2026/wordle/dictionary') {
      return json([{ letters: 'VANDIJK', word: 'VAN DIJK', name: 'Virgil van Dijk', club: 'Liverpool' }])
    }
    if (url === '/api/minigame/2026/wordle/stats') return json(sampleStats())
    return json({ code: 'NOT_FOUND', message: 'Not found' }, 404)
  }, async () => {
    const dict = await fetchWordleDictionary(42)
    assert.equal(dict.length, 1)
    const stats = await fetchWordleStats(42)
    assert.equal(stats.winPercentage, 89)
    await assert.rejects(fetchWordleToday(42), err => err.status === 404)
  })
})

test('resolveWordleAccess handles all 4 combinations of auth x feature flag plus loading/error', () => {
  // 1. Logged out + flag OFF -> hide on Hub, redirect route to #minigame
  assert.deepEqual(resolveWordleAccess({ infoStatus: 'disabled', account: null }), {
    showOnHub: false,
    routeAction: 'redirect',
  })
  // 2. Logged in + flag OFF -> hide on Hub, redirect route to #minigame
  assert.deepEqual(resolveWordleAccess({ infoStatus: 'disabled', account: { id: 101 } }), {
    showOnHub: false,
    routeAction: 'redirect',
  })
  // 3. Logged out + flag ON -> show on Hub ("Đăng nhập để chơi"), show LoginGate on route
  assert.deepEqual(resolveWordleAccess({ infoStatus: 'enabled', account: null }), {
    showOnHub: true,
    routeAction: 'login',
  })
  // 4. Logged in + flag ON -> show on Hub (with /today status), play on route
  assert.deepEqual(resolveWordleAccess({ infoStatus: 'enabled', account: { id: 101 } }), {
    showOnHub: true,
    routeAction: 'play',
  })
  // Loading / error stay hidden on Hub to prevent flash of unlaunched feature
  assert.deepEqual(resolveWordleAccess({ infoStatus: 'loading', account: null }), {
    showOnHub: false,
    routeAction: 'loading',
  })
  assert.deepEqual(resolveWordleAccess({ infoStatus: 'error', account: { id: 101 } }), {
    showOnHub: false,
    routeAction: 'error',
  })
})

test('fetchWordleInfo is public (omits credentials/account header), validates nullable puzzle fields, and surfaces 404', async () => {
  const infoNullPuzzle = {
    enabled: true,
    puzzleNumber: null,
    date: '2026-11-27',
    length: null,
    nextAt,
    serverTime,
  }
  const infoWithPuzzle = { ...infoNullPuzzle, puzzleNumber: 12, length: 7 }
  assert.equal(isValidWordleInfo(infoNullPuzzle), true)
  assert.equal(isValidWordleInfo(infoWithPuzzle), true)
  assert.equal(isValidWordleInfo({ ...infoNullPuzzle, enabled: false }), false)
  assert.equal(isValidWordleInfo({ ...infoNullPuzzle, length: 2 }), false)

  await withFetch(async (url, options) => {
    assert.equal(url, '/api/minigame/2026/wordle/info')
    assert.equal(options.credentials, 'omit')
    assert.equal(options.headers['X-PrismaXI-Account-ID'], undefined)
    return json(infoNullPuzzle)
  }, async () => {
    const info = await fetchWordleInfo()
    assert.equal(info.enabled, true)
    assert.equal(info.puzzleNumber, null)
    assert.equal(info.length, null)
  })

  await withFetch(async () => json({ code: 'NOT_FOUND', message: 'Not found' }, 404), async () => {
    await assert.rejects(fetchWordleInfo(), err => err.status === 404)
  })
})
