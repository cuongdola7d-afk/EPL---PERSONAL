import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildRowWordGroups,
  buildShareText,
  createTelexBurstQueue,
  decodeTelexBurst,
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
  isValidWordleShape,
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
    shape: [3, 4],
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

test('createTelexBurstQueue keeps real Backspace separate from inserts more than 25ms later, and groups rapid delete+insert', async () => {
  const flushed = []
  const queue = createTelexBurstQueue({ onFlush: b => flushed.push(b), windowMs: 25 })

  // Real Backspace, wait 45ms (>25ms), then insert 'A' -> two separate bursts
  queue.pushDelete(1)
  await new Promise(r => setTimeout(r, 45))
  queue.pushInsert('A')
  await new Promise(r => setTimeout(r, 45))
  assert.deepEqual(flushed, [{ deletes: 1, insert: '' }, { deletes: 0, insert: 'A' }])

  // Rapid Telex replacement (V -> A -> N -> ⌫⌫ÁN) inside <10ms -> deletes+insert grouped into 1 burst
  flushed.length = 0
  queue.pushInsert('V')
  queue.pushInsert('A')
  queue.pushInsert('N')
  queue.pushDelete(1)
  queue.pushDelete(1)
  queue.pushInsert('Á')
  queue.pushInsert('N')
  await new Promise(r => setTimeout(r, 45))
  assert.deepEqual(flushed, [
    { deletes: 0, insert: 'V' },
    { deletes: 0, insert: 'A' },
    { deletes: 0, insert: 'N' },
    { deletes: 2, insert: 'ÁN' },
  ])
})
