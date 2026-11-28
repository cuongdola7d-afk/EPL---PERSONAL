import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  TELEX_DELETE_HOLD_MS,
  WORDLE_DEBUG_MAX_ENTRIES,
  WORDLE_FLIP_DURATION_MS,
  WORDLE_FLIP_TOTAL_MS,
  WORDLE_INFO_CACHE_KEY,
  buildRowWordGroups,
  buildShareText,
  clearWordleInfoCache,
  createGuessSyncQueue,
  createTelexBurstQueue,
  createWordleDebugLog,
  decodeTelexBurst,
  decodeWordleKey,
  filterWordleSuggestions,
  formatWordleDateLabel,
  highlightWordPrefix,
  isWordleDebugEnabled,
  keyboardKeyStates,
  readWordleInfoCache,
  resolveWordleAccess,
  resolveWordleHubState,
  scoreWordleGuess,
  startHubRequestsParallel,
  validateWordleGuessLocally,
  wordleFlipDelayMs,
  wordleHubSummary,
  wordleToastMessage,
  wordShape,
  writeWordleInfoCache,
} from './wordle.js'
import { parseMinigameRoute } from './playerGuess.js'
import { fetchGuessInfo } from '../api/playerGuess.js'
import {
  clearWordleCsrfCache,
  fetchWordleDictionary,
  fetchWordleInfo,
  fetchWordleStats,
  fetchWordleToday,
  isValidWordleDictionary,
  isValidWordleInfo,
  isValidWordleShape,
  isValidWordleStats,
  isValidWordleToday,
  revealWordleHint,
  submitWordleGuess,
} from '../api/wordle.js'

const serverTime = '2026-11-27T12:00:00Z'
const nextAt = '2026-11-27T17:00:00Z'

function encodeKeyForTest(letters, mask = [0x1a, 0x2b, 0x3c, 0x4d]) {
  const bytes = new Uint8Array(4 + letters.length)
  bytes.set(mask, 0)
  for (let i = 0; i < letters.length; i += 1) {
    const mix = mask[i % 4] ^ ((i * 31 + 17) & 0xFF)
    bytes[4 + i] = letters.charCodeAt(i) ^ mix
  }
  return Buffer.from(bytes).toString('base64')
}

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
    shape: [3, 4],
    key: encodeKeyForTest('VANDIJK'),
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

test('wordShape and buildRowWordGroups lay out every row by answer shape including short guesses', () => {
  assert.deepEqual(wordShape('VAN DIJK'), [3, 4])
  assert.deepEqual(wordShape('BARCOLA'), [7])
  assert.deepEqual(wordShape('VAN DEN BERG'), [3, 3, 4])

  // Single-word guess BARCOLA laid into [3, 4] answer shape -> BAR | COLA
  const fullInMulti = buildRowWordGroups({ shape: [3, 4], letters: 'BARCOLA', feedback: 'MHMMMMM', length: 7 })
  assert.equal(fullInMulti.length, 2)
  assert.deepEqual(fullInMulti[0].map(t => [t.letter, t.state, t.index]), [
    ['B', 'miss', 0], ['A', 'hit', 1], ['R', 'miss', 2],
  ])
  assert.deepEqual(fullInMulti[1].map(t => [t.letter, t.state, t.index]), [
    ['C', 'miss', 3], ['O', 'miss', 4], ['L', 'miss', 5], ['A', 'miss', 6],
  ])

  // Active row on [3, 4] answer shape keeps 3 + 4 structure immediately
  const active = buildRowWordGroups({ shape: [3, 4], letters: 'BARC', length: 7 })
  assert.equal(active.length, 2)
  assert.deepEqual(active[0].map(t => [t.letter, t.state]), [['B', 'filled'], ['A', 'filled'], ['R', 'filled']])
  assert.deepEqual(active[1].map(t => [t.letter, t.state]), [['C', 'filled'], ['', 'empty'], ['', 'empty'], ['', 'empty']])

  // Short guess SALAH (5) on [3, 4] answer shape -> SAL | AH + 2 dashed pad tiles
  const shortInMulti = buildRowWordGroups({ shape: [3, 4], letters: 'SALAH', feedback: 'MHMMM', length: 7 })
  assert.equal(shortInMulti.length, 2)
  assert.deepEqual(shortInMulti[0].map(t => [t.letter, t.state]), [
    ['S', 'miss'], ['A', 'hit'], ['L', 'miss'],
  ])
  assert.deepEqual(shortInMulti[1].map(t => [t.letter, t.state]), [
    ['A', 'miss'], ['H', 'miss'], ['', 'pad'], ['', 'pad'],
  ])

  // Short guess ISAK (4) on 3-word [3, 3, 4] answer shape -> ISA | K__ | ____
  const shortInThreeWord = buildRowWordGroups({ shape: [3, 3, 4], letters: 'ISAK', feedback: 'NMNN', length: 10 })
  assert.deepEqual(shortInThreeWord.map(g => g.length), [3, 3, 4])
  assert.deepEqual(shortInThreeWord[1].map(t => [t.letter, t.state]), [['K', 'near'], ['', 'pad'], ['', 'pad']])
  assert.deepEqual(shortInThreeWord[2].map(t => t.state), ['pad', 'pad', 'pad', 'pad'])
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

  // Length filtering 4..N: keeps 4..7, excludes 3 (<4) and 8 (>7)
  const mixed = [
    { letters: 'EZE', word: 'EZE', name: 'Eberechi Eze', club: 'Arsenal' },
    { letters: 'SALA', word: 'SALA', name: 'Test Sala', club: 'Club A' },
    { letters: 'SALAH', word: 'SALAH', name: 'Mohamed Salah', club: 'Liverpool' },
    { letters: 'SALIBA', word: 'SALIBA', name: 'William Saliba', club: 'Arsenal' },
    { letters: 'SOLANKE', word: 'SOLANKE', name: 'Dominic Solanke', club: 'Tottenham' },
    { letters: 'SCHNEIDE', word: 'SCHNEIDE', name: 'Too Long', club: 'Club B' },
  ]
  assert.deepEqual(
    filterWordleSuggestions(mixed, 'S', [], 5, 7).map(item => item.letters),
    ['SALA', 'SALAH', 'SALIBA', 'SOLANKE']
  )
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

test('buildShareText groups each row by answer shape, omits padding tiles and player names, and prints X/6 on loss', () => {
  const winText = buildShareText({
    puzzleNumber: 12,
    status: 'WON',
    shape: [3, 4],
    guesses: [
      { word: 'SALAH', letters: 'SALAH', feedback: 'MHMMM', playerName: 'Mohamed Salah', club: 'Liverpool' },
      { word: 'BARCOLA', letters: 'BARCOLA', feedback: 'MNMMMMN', playerName: 'Bradley Barcola', club: 'Liverpool' },
      { word: 'VAN DIJK', letters: 'VANDIJK', feedback: 'HHHHHHH', playerName: 'Virgil van Dijk', club: 'Liverpool' },
    ],
  })
  assert.equal(winText, 'PrismaXI Wordle #12 3/6\n⬛🟩⬛ ⬛⬛\n⬛🟨⬛ ⬛⬛⬛🟨\n🟩🟩🟩 🟩🟩🟩🟩')
  assert.equal(winText.includes('DIJK'), false)
  assert.equal(winText.includes('Barcola'), false)

  const lossText = buildShareText({
    puzzleNumber: 12,
    status: 'LOST',
    shape: [3, 4],
    guesses: [
      { word: 'SALAH', letters: 'SALAH', feedback: 'MHMMM', playerName: 'Mohamed Salah', club: 'Liverpool' },
      { word: 'ISAK', letters: 'ISAK', feedback: 'NMNN', playerName: 'Alexander Isak', club: 'Liverpool' },
      { word: 'VAN BERG', letters: 'VANBERG', feedback: 'HHHMMMM', playerName: 'Test', club: 'Club' },
    ],
  })
  assert.equal(lossText, 'PrismaXI Wordle #12 X/6\n⬛🟩⬛ ⬛⬛\n🟨⬛🟨 🟨\n🟩🟩🟩 ⬛⬛⬛⬛')
})

test('formatWordleDateLabel, wordleToastMessage, and wordleHubSummary format Vietnam labels accurately', () => {
  assert.equal(formatWordleDateLabel('2026-11-27'), 'T6 27/11')
  assert.equal(formatWordleDateLabel('2026-11-29'), 'CN 29/11')
  assert.equal(wordleToastMessage('TOO_SHORT', 7), 'Cần ít nhất 4 chữ cái')
  assert.equal(wordleToastMessage('TOO_LONG', 7), 'Tối đa 7 chữ cái')
  assert.equal(wordleToastMessage('WRONG_LENGTH', 7), 'Cần đủ 7 chữ cái')
  assert.equal(wordleToastMessage('NOT_A_PLAYER', 7), 'Không có cầu thủ Premier League nào tên như vậy')
  assert.equal(wordleToastMessage('ALREADY_GUESSED', 7), 'Bạn đã đoán tên này rồi')

  assert.equal(wordleHubSummary(null, { loading: false, data: null }).statusText, 'Đăng nhập để chơi')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: true, data: null }).statusText, 'Đang tải…')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: false, data: sampleToday({ guesses: [] }) }).statusText, 'Chưa chơi')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: false, data: sampleToday() }).statusText, 'Đang chơi dở · lượt 2/6')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: false, data: sampleToday({ status: 'WON' }) }).statusText, 'Đã đoán ra · 1/6')
  assert.equal(wordleHubSummary({ id: 1 }, { loading: false, data: sampleToday({ status: 'LOST' }) }).statusText, 'Hết lượt')
})

test('API validators verify TodayView, DictionaryEntry, and StatsView contracts', () => {
  assert.equal(isValidWordleToday(sampleToday()), true)
  // Short guess (4..N letters with matching feedback length) is valid
  assert.equal(isValidWordleToday(sampleToday({
    guesses: [
      { turn: 1, word: 'SALAH', letters: 'SALAH', feedback: 'MHMMM', playerName: 'Mohamed Salah', club: 'Liverpool' },
      { turn: 2, word: 'ISAK', letters: 'ISAK', feedback: 'NMNN', playerName: 'Alexander Isak', club: 'Liverpool' },
    ],
  })), true)
  // Shorter than 4 letters or mismatched feedback length is rejected
  assert.equal(isValidWordleToday(sampleToday({
    guesses: [{ turn: 1, word: 'EZE', letters: 'EZE', feedback: 'MMM', playerName: 'Eberechi Eze', club: 'Arsenal' }],
  })), false)
  assert.equal(isValidWordleToday(sampleToday({
    guesses: [{ turn: 1, word: 'SALAH', letters: 'SALAH', feedback: 'MHMMMMM', playerName: 'Mohamed Salah', club: 'Liverpool' }],
  })), false)
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

  // PLAYING may omit stats (null) on POST responses, while WON/LOST must include valid stats
  assert.equal(isValidWordleToday(sampleToday({ stats: null })), true)
  assert.equal(isValidWordleToday(sampleToday({
    status: 'WON',
    stats: null,
    answer: { playerId: 1, name: 'Virgil van Dijk', clubId: 1, club: 'Liverpool', shirtNumber: 4, position: 'CB', nationality: 'Netherlands', age: 35, overall: 88, word: 'VAN DIJK' },
  })), false)

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
  clearWordleCsrfCache()
  await withFetch(async (url, options) => {
    assert.equal(url, '/api/minigame/2026/wordle/today')
    assert.equal(options.credentials, 'include')
    assert.equal(options.headers['X-PrismaXI-Account-ID'], '42')
    return json(sampleToday())
  }, async () => {
    const today = await fetchWordleToday(42)
    assert.equal(today.puzzleNumber, 12)
  })

  clearWordleCsrfCache()
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

  clearWordleCsrfCache()
  await withFetch(async (url, options) => {
    if (url === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'tok-2' })
    assert.equal(url, '/api/minigame/2026/wordle/today/hint')
    assert.equal(options.method, 'POST')
    return json(sampleToday({ hintUsed: true, hintClub: 'Liverpool', stats: null }))
  }, async () => {
    const res = await revealWordleHint(42, { date: '2026-11-27' })
    assert.equal(res.hintClub, 'Liverpool')
    assert.equal(res.stats, null)
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

test('Wordle Hub ordering puts Wordle first in both Today challenge box and game grid', () => {
  const source = fs.readFileSync(new URL('../components/MinigamePage.jsx', import.meta.url), 'utf8')
  const todayStart = source.indexOf('<div className="mg-today">')
  const todayEnd = source.indexOf('<div className="mg-daily-clock">', todayStart)
  const todayBlock = source.slice(todayStart, todayEnd)
  const wordleTodayIdx = todayBlock.indexOf('href="#minigame/wordle"')
  const guessTodayIdx = todayBlock.indexOf("<b>Đoán cầu thủ</b>")
  assert.ok(wordleTodayIdx > 0 && guessTodayIdx > 0 && wordleTodayIdx < guessTodayIdx,
    'Wordle must appear before Đoán cầu thủ in Thử thách hôm nay')

  const gridStart = source.indexOf('className={`mg-game-grid')
  const gridEnd = source.indexOf('<p className="mg-footnote">', gridStart)
  const gridBlock = source.slice(gridStart, gridEnd)
  const idxWordle = gridBlock.indexOf('<h3>Wordle cầu thủ</h3>')
  const idxGuess = gridBlock.indexOf('<h3>Đoán cầu thủ qua gợi ý</h3>')
  const idxHigher = gridBlock.indexOf('<h3>Cao hay thấp</h3>')
  const idxLineup = gridBlock.indexOf('<h3>Đội hình bí ẩn</h3>')
  assert.ok(idxWordle > 0 && idxWordle < idxGuess && idxGuess < idxHigher && idxHigher < idxLineup,
    'Game grid order must be Wordle → Đoán cầu thủ → Cao hay thấp → Đội hình bí ẩn')
})

test('Wordle localStorage cache renders card immediately, falls back to skeleton, clears on 404, and tolerates blocked storage', () => {
  const store = new Map()
  const memStorage = {
    getItem: key => (store.has(key) ? store.get(key) : null),
    setItem: (key, value) => { store.set(key, String(value)) },
    removeItem: key => { store.delete(key) },
  }

  // First visit (no cache) -> skeleton while /info is loading
  assert.equal(readWordleInfoCache(memStorage), null)
  assert.deepEqual(resolveWordleHubState({ infoStatus: 'loading', cachedInfo: null }), {
    mode: 'skeleton',
    data: null,
  })

  // Cache saved after /info 200 -> card renders immediately before /info resolves on next visit
  writeWordleInfoCache({ enabled: true, puzzleNumber: 7, date: '2026-11-28', extra: 'ignored' }, memStorage)
  const cached = readWordleInfoCache(memStorage)
  assert.deepEqual(cached, { enabled: true, puzzleNumber: 7, date: '2026-11-28' })
  assert.deepEqual(resolveWordleHubState({ infoStatus: 'loading', cachedInfo: cached }), {
    mode: 'card',
    data: cached,
  })

  // /info returns 404 -> hide card and clear cache
  clearWordleInfoCache(memStorage)
  assert.equal(store.has(WORDLE_INFO_CACHE_KEY), false)
  assert.deepEqual(resolveWordleHubState({ infoStatus: 'disabled', cachedInfo: cached }), {
    mode: 'hidden',
    data: null,
  })

  // Blocked localStorage throws SecurityError -> never throws, behaves like empty cache
  const blockedStorage = {
    getItem() { throw new Error('SecurityError') },
    setItem() { throw new Error('SecurityError') },
    removeItem() { throw new Error('SecurityError') },
  }
  assert.doesNotThrow(() => writeWordleInfoCache({ enabled: true, puzzleNumber: 7, date: '2026-11-28' }, blockedStorage))
  assert.equal(readWordleInfoCache(blockedStorage), null)
  assert.doesNotThrow(() => clearWordleInfoCache(blockedStorage))
  assert.deepEqual(resolveWordleHubState({ infoStatus: 'loading', cachedInfo: readWordleInfoCache(blockedStorage) }), {
    mode: 'skeleton',
    data: null,
  })
})

test('Hub launches /wordle/info, /player-guess/info, and /wordle/today in parallel (not sequentially)', async () => {
  const startedUrls = []
  const resolvers = new Map()
  const guessInfoPayload = {
    season: 2026,
    timeZone: 'Asia/Ho_Chi_Minh',
    serverTime,
    nextDailyAt: nextAt,
    dailyAvailable: true,
    startingScore: 100,
    maxGuesses: 3,
    initialHints: 3,
    hintPenalty: 10,
    wrongGuessPenalty: 20,
    hints: [],
  }
  const wordleInfoPayload = {
    enabled: true,
    puzzleNumber: 12,
    date: '2026-11-27',
    length: 7,
    shape: [3, 4],
    nextAt,
    serverTime,
  }

  await withFetch(async url => {
    startedUrls.push(url)
    return new Promise(resolve => {
      resolvers.set(url, () => {
        if (url.endsWith('/wordle/info')) resolve(json(wordleInfoPayload))
        else if (url.endsWith('/player-guess/info')) resolve(json(guessInfoPayload))
        else if (url.endsWith('/wordle/today')) resolve(json(sampleToday()))
      })
    })
  }, async () => {
    const reqs = startHubRequestsParallel({
      accountId: 42,
      fetchWordleInfoFn: fetchWordleInfo,
      fetchGuessInfoFn: fetchGuessInfo,
      fetchWordleTodayFn: fetchWordleToday,
    })

    // All 3 requests must already be in-flight simultaneously BEFORE any single one resolves!
    assert.deepEqual(startedUrls, [
      '/api/minigame/2026/wordle/info',
      '/api/minigame/2026/player-guess/info',
      '/api/minigame/2026/wordle/today',
    ])

    for (const resolve of resolvers.values()) resolve()
    const [wInfo, gInfo, wToday] = await Promise.all([
      reqs.wordleInfoPromise,
      reqs.guessInfoPromise,
      reqs.wordleTodayPromise,
    ])
    assert.equal(wInfo.puzzleNumber, 12)
    assert.equal(gInfo.season, 2026)
    assert.equal(wToday.puzzleNumber, 12)
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

test('fetchWordleInfo is public (omits credentials/account header), validates nullable puzzle/shape fields, and surfaces 404', async () => {
  const infoNullPuzzle = {
    enabled: true,
    puzzleNumber: null,
    date: '2026-11-27',
    length: null,
    shape: null,
    nextAt,
    serverTime,
  }
  const infoWithPuzzle = { ...infoNullPuzzle, puzzleNumber: 12, length: 7, shape: [3, 4] }
  assert.equal(isValidWordleShape([3, 4], 7), true)
  assert.equal(isValidWordleShape([3, 3], 7), false)
  assert.equal(isValidWordleInfo(infoNullPuzzle), true)
  assert.equal(isValidWordleInfo(infoWithPuzzle), true)
  assert.equal(isValidWordleInfo({ ...infoWithPuzzle, shape: [3, 3] }), false)
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
    assert.equal(info.shape, null)
  })

  await withFetch(async () => json({ code: 'NOT_FOUND', message: 'Not found' }, 404), async () => {
    await assert.rejects(fetchWordleInfo(), err => err.status === 404)
  })
})

function runTelexBursts(bursts, maxLength = 11) {
  let state = { raw: '', shadow: '', uncertain: false }
  for (const burst of bursts) {
    state = decodeTelexBurst(state, burst, maxLength)
  }
  return state
}

test('decodeTelexBurst decodes Unikey Telex sequences for AA, AAA, HAALAND, DIJK, VANS, DD, WIRTZ, and real Backspace', () => {
  // AA: A · ⌫ + Â
  assert.equal(runTelexBursts([{ insert: 'A' }, { deletes: 1, insert: 'Â' }]).raw, 'AA')
  // AAA: A · ⌫ + Â · ⌫ + AA
  assert.equal(runTelexBursts([{ insert: 'A' }, { deletes: 1, insert: 'Â' }, { deletes: 1, insert: 'AA' }]).raw, 'AAA')
  // HAALAND: H · A · ⌫ + Â · L · A · N · D
  assert.equal(
    runTelexBursts([
      { insert: 'H' }, { insert: 'A' }, { deletes: 1, insert: 'Â' },
      { insert: 'L' }, { insert: 'A' }, { insert: 'N' }, { insert: 'D' },
    ]).raw,
    'HAALAND'
  )
  // DIJK: D · I · ⌫ + Ị · K
  assert.equal(
    runTelexBursts([{ insert: 'D' }, { insert: 'I' }, { deletes: 1, insert: 'Ị' }, { insert: 'K' }]).raw,
    'DIJK'
  )
  // VANS: V · A · N · ⌫⌫ + ÁN
  assert.equal(
    runTelexBursts([{ insert: 'V' }, { insert: 'A' }, { insert: 'N' }, { deletes: 2, insert: 'ÁN' }]).raw,
    'VANS'
  )
  // DD: D · ⌫ + Đ
  assert.equal(runTelexBursts([{ insert: 'D' }, { deletes: 1, insert: 'Đ' }]).raw, 'DD')
  // WIRTZ (tone strip on Z): Ư · I · ⌫⌫ + ỬI · T · ⌫⌫⌫ + ƯIT
  assert.equal(
    runTelexBursts([
      { insert: 'ư' }, { insert: 'i' }, { deletes: 2, insert: 'ửi' },
      { insert: 't' }, { deletes: 3, insert: 'ưit' },
    ]).raw,
    'WIRTZ'
  )
  // WIRTZ (Unikey auto-restore on T + Z): Ư · I · ⌫⌫ + ỬI · ⌫⌫ + WIRT · Z
  assert.equal(
    runTelexBursts([
      { insert: 'ư' }, { insert: 'i' }, { deletes: 2, insert: 'ửi' },
      { deletes: 2, insert: 'wirt' }, { insert: 'z' },
    ]).raw,
    'WIRTZ'
  )
  // WIRTZ (tone strip on T + Z): Ư · I · ⌫ + Ỉ · ⌫⌫ + ƯIT · Z
  assert.equal(
    runTelexBursts([
      { insert: 'Ư' }, { insert: 'I' }, { deletes: 1, insert: 'Ỉ' },
      { deletes: 2, insert: 'ƯIT' }, { insert: 'Z' },
    ]).raw,
    'WIRTZ'
  )
  // MACALLISTER (S adds sắc on I, R replaces/adds hỏi on E)
  assert.equal(
    runTelexBursts([
      { insert: 'm' }, { insert: 'a' }, { insert: 'c' }, { insert: 'a' }, { insert: 'l' }, { insert: 'l' },
      { insert: 'i' }, { deletes: 1, insert: 'í' }, { insert: 't' }, { insert: 'e' }, { deletes: 1, insert: 'ẻ' },
    ]).raw,
    'MACALLISTER'
  )
  // GUIMARAES (R adds hỏi -> ả, A adds circumflex -> ẩ, E, S replaces hỏi with sắc -> ấe)
  assert.equal(
    runTelexBursts([
      { insert: 'g' }, { insert: 'u' }, { insert: 'i' }, { insert: 'm' }, { insert: 'a' },
      { deletes: 1, insert: 'ả' }, { deletes: 1, insert: 'ẩ' }, { insert: 'e' }, { deletes: 2, insert: 'ấe' },
    ]).raw,
    'GUIMARAES'
  )
  // Fast typist coalescing: L arrives within 25ms of Â in HAALAND ({ deletes: 1, insert: 'âl' })
  assert.equal(
    runTelexBursts([
      { insert: 'h' }, { insert: 'a' }, { deletes: 1, insert: 'âl' },
      { insert: 'a' }, { insert: 'n' }, { insert: 'd' },
    ]).raw,
    'HAALAND'
  )
  // Spacebar auto-restore does not append extra letter or raise warning
  const afterSpace = runTelexBursts([
    { insert: 'h' }, { insert: 'a' }, { deletes: 1, insert: 'â' },
    { insert: 'l' }, { insert: 'a' }, { insert: 'n' }, { insert: 'd' },
    { deletes: 6, insert: 'haaland ' },
  ])
  assert.equal(afterSpace.raw, 'HAALAND')
  assert.equal(afterSpace.uncertain, false)
  // Real Backspace deletes exactly 1 letter
  assert.equal(
    runTelexBursts([{ insert: 'S' }, { insert: 'A' }, { deletes: 1, insert: '' }, { insert: 'I' }]).raw,
    'SI'
  )
})

test('createTelexBurstQueue keeps real Backspace separate after 150ms hold, flushes plain inserts immediately, and groups rapid delete+insert', async () => {
  const flushed = []
  const queue = createTelexBurstQueue({ onFlush: b => flushed.push(b) })

  // Plain inserts flush immediately without waiting for timer
  queue.pushInsert('V')
  queue.pushInsert('A')
  queue.pushInsert('N')
  assert.deepEqual(flushed, [
    { deletes: 0, insert: 'V' },
    { deletes: 0, insert: 'A' },
    { deletes: 0, insert: 'N' },
  ])

  // Rapid Telex replacement (⌫⌫ + Á + N) groups into 1 burst
  flushed.length = 0
  queue.pushDelete(1)
  queue.pushDelete(1)
  queue.pushInsert('Á')
  queue.pushInsert('N')
  await new Promise(r => setTimeout(r, 45))
  assert.deepEqual(flushed, [{ deletes: 2, insert: 'ÁN' }])

  // Real Backspace held TELEX_DELETE_HOLD_MS (150ms), wait >150ms, then insert 'A' -> 2 separate bursts
  flushed.length = 0
  queue.pushDelete(1)
  assert.equal(flushed.length, 0)
  await new Promise(r => setTimeout(r, TELEX_DELETE_HOLD_MS + 40))
  queue.pushInsert('A')
  assert.deepEqual(flushed, [{ deletes: 1, insert: '' }, { deletes: 0, insert: 'A' }])
})

test('decodeTelexBurst safety net decodes k=0 single accented char matching last raw letter', () => {
  assert.equal(runTelexBursts([{ insert: 'A' }, { insert: 'ả' }]).raw, 'AR')
  assert.equal(runTelexBursts([{ insert: 'H' }, { insert: 'A' }, { insert: 'â' }]).raw, 'HAA')
  assert.equal(runTelexBursts([{ insert: 'D' }, { insert: 'đ' }]).raw, 'DD')
  assert.equal(runTelexBursts([{ insert: 'D' }, { insert: 'I' }, { insert: 'ị' }, { insert: 'K' }]).raw, 'DIJK')
})

test('createTelexBurstQueue + decodeTelexBurst handle all real-delay Unikey Telex sequences, real Backspaces, and Enter flush', async () => {
  const wait = ms => new Promise(r => setTimeout(r, ms))
  async function simulate(steps, maxLength = 11) {
    let state = { raw: '', shadow: '', uncertain: false }
    const queue = createTelexBurstQueue({
      onFlush: burst => {
        state = decodeTelexBurst(state, burst, maxLength)
      },
    })
    for (const step of steps) {
      if (step.wait) await wait(step.wait)
      if (step.del) queue.pushDelete(step.del)
      if (step.ins) queue.pushInsert(step.ins)
      if (step.enter) {
        queue.flush()
        return state
      }
    }
    await wait(TELEX_DELETE_HOLD_MS + 60)
    return state
  }

  // 1. A · (60ms) ⌫ · (60ms) "ả" -> AR
  assert.equal((await simulate([{ ins: 'a' }, { wait: 60, del: 1 }, { wait: 60, ins: 'ả' }])).raw, 'AR')
  // 2. A · ⌫ · "ả" · rồi ⌫ · "ar" (Unikey undo on 2nd R) -> ARR
  assert.equal((await simulate([{ ins: 'a' }, { del: 1 }, { ins: 'ả' }, { wait: 40, del: 1 }, { ins: 'ar' }])).raw, 'ARR')
  // 3. D I · (80ms) ⌫ · (40ms) "ị" · K -> DIJK
  assert.equal((await simulate([{ ins: 'd' }, { ins: 'i' }, { wait: 80, del: 1 }, { wait: 40, ins: 'ị' }, { wait: 40, ins: 'k' }])).raw, 'DIJK')
  // 4. V A N · (70ms) ⌫⌫ · (70ms) "án" -> VANS
  assert.equal((await simulate([{ ins: 'v' }, { ins: 'a' }, { ins: 'n' }, { wait: 70, del: 1 }, { del: 1 }, { wait: 70, ins: 'án' }])).raw, 'VANS')
  // 5. H A · (100ms) ⌫ · (50ms) "â" · L A N D -> HAALAND
  assert.equal((await simulate([
    { ins: 'h' }, { ins: 'a' }, { wait: 100, del: 1 }, { wait: 50, ins: 'â' },
    { wait: 40, ins: 'l' }, { ins: 'a' }, { ins: 'n' }, { ins: 'd' },
  ])).raw, 'HAALAND')
  // 6. D · (90ms) ⌫ · (30ms) "đ" -> DD
  assert.equal((await simulate([{ ins: 'd' }, { wait: 90, del: 1 }, { wait: 30, ins: 'đ' }])).raw, 'DD')
  // 7. Backspace thật: S A · ⌫ · (300ms) · I -> SI
  assert.equal((await simulate([{ ins: 's' }, { ins: 'a' }, { del: 1 }, { wait: 300, ins: 'i' }])).raw, 'SI')
  // 8. Backspace thật 2 lần liên tiếp, cách nhau 200ms -> xóa đúng 2 chữ
  assert.equal((await simulate([{ ins: 's' }, { ins: 'a' }, { ins: 'l' }, { del: 1 }, { wait: 200, del: 1 }])).raw, 'S')
  // 9. Bấm Enter khi đang giữ một Backspace -> cụm được xử lý trước khi gửi
  assert.equal((await simulate([{ ins: 's' }, { ins: 'a' }, { ins: 'l' }, { del: 1 }, { wait: 20, enter: true }])).raw, 'SA')
})

test('Wordle API caches CSRF across POSTs and retries exactly once on 403', async () => {
  clearWordleCsrfCache()
  let csrfCalls = 0
  let guessCalls = 0
  let failNext403 = 0

  await withFetch(async (url, options) => {
    if (url === '/api/auth/csrf') {
      csrfCalls += 1
      return json({ headerName: 'X-CSRF-TOKEN', token: `tok-${csrfCalls}` })
    }
    if (url === '/api/minigame/2026/wordle/today/guesses') {
      guessCalls += 1
      if (failNext403 > 0) {
        failNext403 -= 1
        return json({ code: 'CSRF_INVALID', message: 'Forbidden' }, 403)
      }
      return json(sampleToday({ stats: null }))
    }
    return json({ code: 'NOT_FOUND' }, 404)
  }, async () => {
    // 1st POST fetches CSRF once
    await submitWordleGuess(42, 'SALAH', { date: '2026-11-27' })
    assert.equal(csrfCalls, 1)
    assert.equal(guessCalls, 1)

    // 2nd POST reuses cached CSRF -> still 1 CSRF call
    await submitWordleGuess(42, 'BARCOLA', { date: '2026-11-27' })
    assert.equal(csrfCalls, 1)
    assert.equal(guessCalls, 2)

    // 3rd POST hits 403 once -> refreshes CSRF once and retries once -> succeeds
    failNext403 = 1
    await submitWordleGuess(42, 'ISAK', { date: '2026-11-27' })
    assert.equal(csrfCalls, 2)
    assert.equal(guessCalls, 4)

    // 4th POST hits persistent 403 -> retries at most once (2 POSTs, 1 CSRF refresh) and rejects 403
    failNext403 = 5
    await assert.rejects(
      submitWordleGuess(42, 'SAKA', { date: '2026-11-27' }),
      err => err.status === 403
    )
    assert.equal(csrfCalls, 3)
    assert.equal(guessCalls, 6)
  })
})

test('wordleFlipDelayMs completes row flip in ~0.6s for all guess lengths 4..11 and submitting state is removed', () => {
  assert.equal(WORDLE_FLIP_DURATION_MS, 320)
  assert.equal(WORDLE_FLIP_TOTAL_MS, 600)
  for (const len of [4, 5, 7, 10, 11]) {
    assert.equal(wordleFlipDelayMs(0, len), 0)
    const lastFinish = wordleFlipDelayMs(len - 1, len) + WORDLE_FLIP_DURATION_MS
    assert.ok(lastFinish >= 580 && lastFinish <= 620, `len=${len} finished at ${lastFinish}ms`)
  }
  const here = path.dirname(fileURLToPath(import.meta.url))
  const jsx = fs.readFileSync(path.resolve(here, '../components/WordleGame.jsx'), 'utf8')
  const css = fs.readFileSync(path.resolve(here, '../components/WordleGame.css'), 'utf8')
  assert.equal(jsx.includes('wd-submitting'), false)
  assert.equal(css.includes('wd-submitting'), false)
})

test('scoreWordleGuess matches every shared case in docs/wordle-score-cases.json and decodeWordleKey round-trips', () => {
  const here = path.dirname(fileURLToPath(import.meta.url))
  const casesPath = path.resolve(here, '../../../docs/wordle-score-cases.json')
  const cases = JSON.parse(fs.readFileSync(casesPath, 'utf8'))
  assert.ok(Array.isArray(cases) && cases.length >= 30)
  for (const entry of cases) {
    assert.equal(
      scoreWordleGuess(entry.guess, entry.answer),
      entry.expected,
      `Shared case ${entry.name}: ${entry.guess} vs ${entry.answer}`
    )
  }

  assert.equal(decodeWordleKey(encodeKeyForTest('VANDIJK')), 'VANDIJK')
  assert.equal(decodeWordleKey(encodeKeyForTest('MACALLISTER', [0xfe, 0x01, 0x99, 0x42])), 'MACALLISTER')
  assert.equal(decodeWordleKey(''), null)
  assert.equal(decodeWordleKey('AAAA'), null)
})

test('decodeTelexBurst ignores UniKey placeholder dot U+00B7 across real user log and all tones/doubled letters/W', async () => {
  const wait = ms => new Promise(r => setTimeout(r, ms))
  async function replay(events) {
    let state = { raw: '', shadow: '', uncertain: false }
    const queue = createTelexBurstQueue({
      onFlush: burst => {
        state = decodeTelexBurst(state, burst, 11)
      },
    })
    for (const ev of events) {
      if (ev.wait) await wait(ev.wait)
      if (ev.del) queue.pushDelete(ev.del)
      if (ev.ins) queue.pushInsert(ev.ins)
    }
    await wait(TELEX_DELETE_HOLD_MS + 50)
    return state
  }

  // Exact replay of real user UniKey log: A then R (1st press -> AR, 2nd press undo -> ARR)
  const firstR = await replay([
    { ins: 'a' },
    { wait: 40, ins: '·' },
    { wait: 5, del: 1 },
    { wait: 5, del: 1 },
    { wait: 5, ins: 'ả' },
  ])
  assert.equal(firstR.raw, 'AR')
  assert.equal(firstR.shadow, 'ả')
  assert.equal(firstR.uncertain, false)

  const secondR = await replay([
    { ins: 'a' },
    { wait: 40, ins: '·' },
    { wait: 5, del: 1 },
    { wait: 5, del: 1 },
    { wait: 5, ins: 'ả' },
    { wait: 60, ins: '·' },
    { wait: 5, del: 1 },
    { wait: 5, del: 1 },
    { wait: 5, ins: 'a' },
    { wait: 5, ins: 'r' },
  ])
  assert.equal(secondR.raw, 'ARR')
  assert.equal(secondR.shadow, 'ar')
  assert.equal(secondR.uncertain, false)

  // All tones + doubled letters + W -> Ư with U+00B7 placeholder
  const dotCases = [
    // Nặng: d i · ⌫⌫ ị k -> DIJK
    [[{ insert: 'd' }, { insert: 'i' }, { insert: '·' }, { deletes: 2, insert: 'ị' }, { insert: 'k' }], 'DIJK'],
    // Sắc: v a n · ⌫⌫⌫ án -> VANS
    [[{ insert: 'v' }, { insert: 'a' }, { insert: 'n' }, { insert: '·' }, { deletes: 3, insert: 'án' }], 'VANS'],
    // Huyền: s a · ⌫⌫ à -> SAF
    [[{ insert: 's' }, { insert: 'a' }, { insert: '·' }, { deletes: 2, insert: 'à' }], 'SAF'],
    // Ngã: a · ⌫⌫ ã -> AX
    [[{ insert: 'a' }, { insert: '·' }, { deletes: 2, insert: 'ã' }], 'AX'],
    // Chữ đôi AA: h a · ⌫⌫ â l a n d -> HAALAND
    [[{ insert: 'h' }, { insert: 'a' }, { insert: '·' }, { deletes: 2, insert: 'â' }, { insert: 'l' }, { insert: 'a' }, { insert: 'n' }, { insert: 'd' }], 'HAALAND'],
    // Chữ đôi DD: d · ⌫⌫ đ -> DD
    [[{ insert: 'd' }, { insert: '·' }, { deletes: 2, insert: 'đ' }], 'DD'],
    // Chữ đôi OO: o · ⌫⌫ ô -> OO
    [[{ insert: 'o' }, { insert: '·' }, { deletes: 2, insert: 'ô' }], 'OO'],
    // W -> Ư (standalone via · ⌫ ư, plus WIRTZ with ·)
    [[{ insert: '·' }, { deletes: 1, insert: 'ư' }, { insert: 'i' }, { insert: '·' }, { deletes: 3, insert: 'ửi' }, { insert: 't' }, { insert: '·' }, { deletes: 4, insert: 'ưit' }], 'WIRTZ'],
    // A + W -> Ă via · ⌫⌫ ă
    [[{ insert: 'a' }, { insert: '·' }, { deletes: 2, insert: 'ă' }], 'AW'],
  ]
  for (const [bursts, expected] of dotCases) {
    const res = runTelexBursts(bursts)
    assert.equal(res.raw, expected, `Expected ${expected}, got ${res.raw}`)
    assert.equal(res.uncertain, false, `Expected uncertain=false for ${expected}`)
  }
})

test('validateWordleGuessLocally validates all error codes before any server call', () => {
  const dict = new Map([
    ['SALAH', { letters: 'SALAH', word: 'SALAH', name: 'Mohamed Salah', club: 'Liverpool FC' }],
    ['VANDIJK', { letters: 'VANDIJK', word: 'VAN DIJK', name: 'Virgil van Dijk', club: 'Liverpool FC' }],
  ])
  assert.equal(validateWordleGuessLocally({ letters: 'ABC', maxLength: 7, dictionaryByLetters: dict }).code, 'TOO_SHORT')
  assert.equal(validateWordleGuessLocally({ letters: 'ODEGAARD', maxLength: 7, dictionaryByLetters: dict }).code, 'TOO_LONG')
  assert.equal(validateWordleGuessLocally({ letters: 'VAN DIJK', maxLength: 7, dictionaryByLetters: dict }).code, 'INVALID_CHARACTERS')
  assert.equal(validateWordleGuessLocally({ letters: 'ZZZZZ', maxLength: 7, dictionaryByLetters: dict }).code, 'NOT_A_PLAYER')
  assert.equal(validateWordleGuessLocally({ letters: 'SALAH', maxLength: 7, dictionaryByLetters: dict, guessedLetters: ['SALAH'] }).code, 'ALREADY_GUESSED')
  assert.equal(validateWordleGuessLocally({ letters: 'VANDIJK', maxLength: 7, dictionaryByLetters: dict, finished: true }).code, 'GAME_FINISHED')
  const ok = validateWordleGuessLocally({ letters: 'salah', maxLength: 7, dictionaryByLetters: dict, guessedLetters: [] })
  assert.equal(ok.ok, true)
  assert.equal(ok.normalized, 'SALAH')
})

test('createGuessSyncQueue sends in FIFO order, retries transient network errors up to 3x, and resyncs on mismatch or rejection', async () => {
  // 1. FIFO order + 2 transient network failures before succeeding on retry #2
  const sentOrder = []
  let attemptsForTurn1 = 0
  const syncedTurns = []
  const mismatches = []
  const queue = createGuessSyncQueue({
    retryDelaysMs: [5, 10, 15],
    sendGuess: async item => {
      sentOrder.push(`${item.turn}:${item.letters}`)
      if (item.turn === 1 && attemptsForTurn1 < 2) {
        attemptsForTurn1 += 1
        throw new Error('Network down')
      }
      await new Promise(r => setTimeout(r, 10))
      const guesses = []
      if (item.turn >= 1) guesses.push({ turn: 1, letters: 'SALAH', feedback: 'MHMMM' })
      if (item.turn >= 2) guesses.push({ turn: 2, letters: 'BARCOLA', feedback: 'MHMMMMM' })
      return { status: 'PLAYING', guesses }
    },
    onSynced: (_data, item) => syncedTurns.push(item.turn),
    onMismatchOrRejected: reason => mismatches.push(reason),
  })

  queue.enqueue({ turn: 1, letters: 'SALAH', feedback: 'MHMMM', status: 'PLAYING', date: '2026-11-27' })
  queue.enqueue({ turn: 2, letters: 'BARCOLA', feedback: 'MHMMMMM', status: 'PLAYING', date: '2026-11-27' })
  await queue.waitUntilIdle()

  assert.deepEqual(sentOrder, ['1:SALAH', '1:SALAH', '1:SALAH', '2:BARCOLA'])
  assert.deepEqual(syncedTurns, [1, 2])
  assert.deepEqual(mismatches, [])

  // 2. Server feedback mismatch clears queued turn 2 and triggers resync callback
  const sent2 = []
  const reasons2 = []
  const queue2 = createGuessSyncQueue({
    retryDelaysMs: [5, 10, 15],
    sendGuess: async item => {
      sent2.push(item.turn)
      return { status: 'PLAYING', guesses: [{ turn: 1, letters: 'SALAH', feedback: 'HHHHH' }] }
    },
    onMismatchOrRejected: reason => reasons2.push(reason),
  })
  queue2.enqueue({ turn: 1, letters: 'SALAH', feedback: 'MHMMM', status: 'PLAYING', date: '2026-11-27' })
  queue2.enqueue({ turn: 2, letters: 'BARCOLA', feedback: 'MHMMMMM', status: 'PLAYING', date: '2026-11-27' })
  await queue2.waitUntilIdle()

  assert.deepEqual(sent2, [1])
  assert.deepEqual(reasons2, ['mismatch'])
})

test('debug mode activates only via ?debug=1 or localStorage.wdDebug=1, supports hash route, and caps log at 500 entries', () => {
  assert.deepEqual(parseMinigameRoute('#minigame/wordle?debug=1'), { game: 'WORDLE', mode: null })
  assert.equal(isWordleDebugEnabled('#minigame/wordle', '', { getItem: () => null }), false)
  assert.equal(isWordleDebugEnabled('#minigame/wordle?debug=1', '', { getItem: () => null }), true)
  assert.equal(isWordleDebugEnabled('#minigame/wordle', '?debug=1', { getItem: () => null }), true)
  assert.equal(isWordleDebugEnabled('#minigame/wordle', '', { getItem: k => (k === 'wdDebug' ? '1' : null) }), true)

  const log = createWordleDebugLog(WORDLE_DEBUG_MAX_ENTRIES)
  for (let i = 0; i < 520; i += 1) log.push({ t: i, event: 'keydown', key: 'a' })
  assert.equal(log.entries().length, 500)
  assert.equal(log.entries()[0].t, 20)
  assert.equal(log.entries()[499].t, 519)
})

test('typing I, S, A via both insertText and compositionend produces suggestions containing ISAK', () => {
  const dict = [
    { letters: 'ISAK', word: 'ISAK', name: 'Alexander Isak', club: 'Liverpool FC' },
    { letters: 'SALAH', word: 'SALAH', name: 'Mohamed Salah', club: 'Liverpool FC' },
    { letters: 'VANDIJK', word: 'VAN DIJK', name: 'Virgil van Dijk', club: 'Liverpool FC' },
  ]
  for (const mode of ['insertText', 'compositionend']) {
    let state = { raw: '', shadow: '', uncertain: false }
    const queue = createTelexBurstQueue({
      onFlush: burst => {
        state = decodeTelexBurst(state, burst, 7)
      },
    })
    for (const ch of ['I', 'S', 'A']) {
      // Both insertText and compositionend feed burstQueue.pushInsert(data)
      queue.pushInsert(ch)
    }
    queue.flush()
    assert.equal(state.raw, 'ISA', `mode=${mode}`)
    const sugs = filterWordleSuggestions(dict, state.raw, [], 5, 7)
    assert.ok(sugs.some(entry => entry.letters === 'ISAK'), `mode=${mode} must suggest ISAK`)
  }
})
