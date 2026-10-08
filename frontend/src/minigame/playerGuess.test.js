import test from 'node:test'
import assert from 'node:assert/strict'
import { createAction, formatCountdown, parseMinigameRoute, remainingSeconds } from './playerGuess.js'
import { fetchCurrentGame, fetchGuessLeaderboard, mutateGuessGame, validGame } from '../api/playerGuess.js'

const gameId = '12345678-1234-1234-1234-123456789012'
const actionId = '22345678-1234-1234-1234-123456789012'
const serverTime = '2026-10-08T05:00:00Z', nextDailyAt = '2026-10-08T17:00:00Z'
function game(overrides = {}) {
  return { gameId, accountId: 101, season: 2026, mode: 'DAILY', questionDate: '2026-10-08', status: 'IN_PROGRESS', version: 1,
    currentScore: 100, finalScore: null, guessesUsed: 0, guessesRemaining: 3, revealedHintCount: 2, totalHints: 8,
    canGuess: true, canRevealHint: true, nextHintKey: 'age', serverTime, nextDailyAt, expiresAt: nextDailyAt,
    hints: ['height', 'foot', 'age', 'ovr', 'nationality', 'position', 'club', 'shirtNumber'].map((key, index) =>
      ({ key, label: key, revealed: index < 2, value: index < 2 ? 'Test hint' : null })), guesses: [], answer: null, ...overrides }
}
async function withFetch(mock, run) {
  const previous = globalThis.fetch
  globalThis.fetch = mock
  try { await run() } finally { globalThis.fetch = previous }
}
const json = (data, status = 200) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })

test('routes recognize hub and both playable modes without swallowing other pages', () => {
  assert.deepEqual(parseMinigameRoute('#minigame'), { mode: null })
  assert.deepEqual(parseMinigameRoute('#minigame/guess/practice'), { mode: 'PRACTICE' })
  assert.deepEqual(parseMinigameRoute('#minigame/guess/daily'), { mode: 'DAILY' })
  assert.equal(parseMinigameRoute('#fantasy'), null)
  assert.equal(parseMinigameRoute('#minigame/guess/unknown'), null)
})
test('daily countdown advances from server time and stops at Vietnamese midnight', () => {
  assert.equal(formatCountdown(remainingSeconds(serverTime, nextDailyAt)), '12:00:00')
  assert.equal(remainingSeconds('2026-10-08T16:59:59Z', nextDailyAt, 500), 1)
  assert.equal(remainingSeconds('2026-10-08T16:59:59Z', nextDailyAt, 1100), 0)
  assert.equal(remainingSeconds(null, null), null)
})
test('explicit start and subsequent actions retain server identity/version', () => {
  assert.deepEqual(createAction('start', null, null, actionId).body, { actionId, expectedVersion: 0, expectedGameId: null })
  assert.deepEqual(createAction('start', game(), null, actionId).body, { actionId, expectedVersion: 1, expectedGameId: gameId })
  assert.deepEqual(createAction('guess', game(), 8, actionId).body, { actionId, expectedVersion: 1, player_id: 8 })
  assert.equal(createAction('hint', game(), null, actionId).body.player_id, undefined)
})
test('game validation rejects another owner, hidden answers and hidden hint values', () => {
  assert.equal(validGame(game(), 101), true)
  assert.equal(validGame(game(), 102), false)
  assert.equal(validGame(game({ answer: { playerId: 1, name: 'Secret', club: 'Secret' } }), 101), false)
  const leaked = game(); leaked.hints[2].value = '26'
  assert.equal(validGame(leaked, 101), false)
  const finished = game({ status: 'LOST', finalScore: 0, currentScore: 40,
    hints: game().hints.map(hint => ({ ...hint, revealed: true, value: 'Test hint' })),
    answer: { playerId: 1, name: 'Answer', club: 'Club' }, canGuess: false, canRevealHint: false, nextHintKey: null })
  assert.equal(validGame(finished, 101), true)
})
test('GET current carries account/cookie, accepts no game and never performs a POST', async () => {
  await withFetch(async (url, options) => {
    assert.equal(url, '/api/minigame/2026/player-guess/daily/current')
    assert.equal(options.credentials, 'include'); assert.equal(options.headers['X-PrismaXI-Account-ID'], '101')
    assert.equal(options.method, undefined)
    return json({ status: 'NOT_STARTED', game: null, serverTime, nextDailyAt })
  }, async () => assert.equal((await fetchCurrentGame(101, 'DAILY')).game, null))
})
test('mutation sends CSRF and original action body; ambiguous network retry uses the same key', async () => {
  const bodies = [], action = createAction('guess', game(), 2, actionId)
  let count = 0
  await withFetch(async (url, options) => {
    if (url === '/api/auth/csrf') return json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-test' })
    assert.equal(options.headers['X-CSRF-TOKEN'], 'csrf-test')
    assert.equal(options.headers['X-PrismaXI-Account-ID'], '101')
    assert.equal(options.credentials, 'include'); assert.equal(options.method, 'POST')
    bodies.push(options.body)
    if (++count === 1) throw new TypeError('Network disconnected after send')
    return json({ code: 'OK', game: game(), effect: { type: 'REPLAY', replayed: true } })
  }, async () => {
    await assert.rejects(mutateGuessGame(101, 'DAILY', action), failure => failure.uncertain === true)
    const result = await mutateGuessGame(101, 'DAILY', action)
    assert.equal(result.effect.replayed, true)
    assert.equal(bodies[0], bodies[1])
  })
})
test('version conflict and repeat guess preserve validated server game for recovery', async () => {
  for (const [status, code] of [[409, 'VERSION_CONFLICT'], [422, 'PLAYER_ALREADY_GUESSED']]) {
    await withFetch(async url => url === '/api/auth/csrf' ? json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-test' }) :
      json({ code, message: 'Conflict', game: game({ version: 2 }) }, status), async () => {
      await assert.rejects(mutateGuessGame(101, 'DAILY', createAction('hint', game(), null, actionId)), failure =>
        failure.game.version === 2 && failure.status === status && !failure.uncertain)
    })
  }
})
test('failed CSRF blocks mutation; cancellation is forwarded to fetch', async () => {
  let calls = 0
  const controller = new AbortController()
  await withFetch(async (url, options) => {
    calls++; assert.equal(url, '/api/auth/csrf'); assert.equal(options.signal, controller.signal)
    return json({}, 403)
  }, async () => {
    await assert.rejects(mutateGuessGame(101, 'DAILY', createAction('hint', game(), null, actionId), controller.signal), error => error.status === 403 && !error.uncertain)
    assert.equal(calls, 1)
  })
})
test('malformed successful mutation remains uncertain, preventing a fresh action', async () => {
  await withFetch(async url => url === '/api/auth/csrf' ? json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-test' }) :
    json({ code: 'OK', game: game({ accountId: 102 }) }), async () => {
    await assert.rejects(mutateGuessGame(101, 'DAILY', createAction('hint', game(), null, actionId)), error => error.uncertain)
  })
})
test('null or non-JSON successful replies retain uncertainty; rejected CSRF HTML does not', async () => {
  for (const [response, uncertain] of [[json(null), true], [new Response('gateway', { status: 200 }), true], [new Response('denied', { status: 403 }), false]]) {
    await withFetch(async url => url === '/api/auth/csrf' ? json({ headerName: 'X-CSRF-TOKEN', token: 'csrf-test' }) : response,
      async () => assert.rejects(mutateGuessGame(101, 'DAILY', createAction('hint', game(), null, actionId)), error => error.uncertain === uncertain))
  }
})
test('network failure fetching CSRF never reports the mutation as sent', async () => {
  let calls = 0
  await withFetch(async url => { calls++; assert.equal(url, '/api/auth/csrf'); throw new TypeError('Offline') }, async () => {
    await assert.rejects(mutateGuessGame(101, 'DAILY', createAction('hint', game(), null, actionId)), error => !error.uncertain)
    assert.equal(calls, 1)
  })
})
test('leaderboard is public and preserves server tie ranks without sorting by time', async () => {
  const players = [101, 102, 103].map((accountId, index) => ({ accountId, rank: index < 2 ? 1 : 3,
    displayName: `Player ${accountId}`, totalPoints: index < 2 ? 100 : 90, dailyGames: 1, tied: index < 2 }))
  await withFetch(async (url, options) => {
    assert.equal(options.headers['X-PrismaXI-Account-ID'], undefined)
    assert.equal(url, '/api/minigame/2026/player-guess/leaderboard?offset=0&limit=20')
    return json({ season: 2026, serverTime, players, offset: 0, limit: 20 })
  }, async () => assert.deepEqual((await fetchGuessLeaderboard()).players.map(player => player.rank), [1, 1, 3]))
})
