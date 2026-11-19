import test from 'node:test'
import assert from 'node:assert/strict'
import {
  BEST_SCORE_KEY,
  advance,
  applyGuess,
  calculateAge,
  cardTier,
  createGame,
  eligiblePlayers,
  formatFoot,
  pickRound,
  readBest,
  saveBest,
  statValue,
} from './higherLower.js'

function player(id, overrides = {}) {
  return {
    id,
    name: `Player ${id}`,
    clubId: 1000000057,
    club: 'Arsenal',
    position: 'MIDFIELDER',
    primaryPosition: 'CM',
    fc27Overall: 80 + (id % 10),
    nationality: 'Anh',
    birthDate: '2000-06-15',
    heightCm: 180,
    preferredFoot: 'RIGHT',
    shirtNumber: id,
    goals: id,
    assists: id * 2,
    ...overrides,
  }
}

function memoryStorage(initial = {}) {
  const data = new Map(Object.entries(initial))
  return {
    getItem(key) { return data.has(key) ? data.get(key) : null },
    setItem(key, value) { data.set(key, String(value)) },
  }
}

function failingStorage() {
  return {
    getItem() { throw new Error('Storage blocked') },
    setItem() { throw new Error('Storage blocked') },
  }
}

test('eligiblePlayers excludes players missing required fields and keeps null goals/assists eligible for OVR', () => {
  const list = [
    player(1),
    player(2, { goals: null, assists: null }),
    player(3, { fc27Overall: null }),
    player(4, { nationality: '' }),
    player(5, { birthDate: null }),
    player(6, { heightCm: null }),
    player(7, { preferredFoot: null }),
    player(8, { shirtNumber: null }),
    player(9, { primaryPosition: null }),
    player(10, { club: '   ' }),
    player(11, { birthDate: '2000-02-31' }),
    player(1, { name: 'Duplicate ID' }),
  ]

  const pool = eligiblePlayers(list, new Date('2026-11-19T10:00:00Z'))
  assert.deepEqual(pool.map((item) => item.id), [1, 2])
  assert.equal(statValue(pool[1], 'goals'), null)
  assert.equal(statValue(pool[1], 'assists'), null)
  assert.equal(statValue(pool[1], 'ovr'), 82)
})

test('players with null goals and assists are never selected for goals or assists rounds but work for OVR', () => {
  const a = player(1, { fc27Overall: 84, goals: null, assists: null })
  const b = player(2, { fc27Overall: 88, goals: 5, assists: 3 })
  const pool = eligiblePlayers([a, b], new Date('2026-11-19T10:00:00Z'))

  for (const seed of [0, 0.25, 0.5, 0.75, 0.99]) {
    const roundFromA = pickRound(pool, a, [], () => seed)
    assert.ok(roundFromA)
    assert.equal(roundFromA.stat, 'ovr')
    assert.equal(roundFromA.right.id, b.id)

    const roundFromB = pickRound(pool, b, [], () => seed)
    assert.ok(roundFromB)
    assert.equal(roundFromB.stat, 'ovr')
    assert.equal(roundFromB.right.id, a.id)
  }
})

test('left player with goals=0 or assists=0 never triggers that stat round but still allows OVR', () => {
  const zeroBoth = player(1, { fc27Overall: 82, goals: 0, assists: 0 })
  const zeroGoalsOnly = player(2, { fc27Overall: 85, goals: 0, assists: 4 })
  const zeroAssistsOnly = player(3, { fc27Overall: 88, goals: 6, assists: 0 })
  const scorer = player(4, { fc27Overall: 90, goals: 10, assists: 8 })
  const pool = [zeroBoth, zeroGoalsOnly, zeroAssistsOnly, scorer]

  for (const seed of [0, 0.15, 0.33, 0.49, 0.66, 0.85, 0.99]) {
    const fromZeroBoth = pickRound(pool, zeroBoth, [], () => seed)
    assert.ok(fromZeroBoth)
    assert.equal(fromZeroBoth.stat, 'ovr')

    const fromZeroGoals = pickRound(pool, zeroGoalsOnly, [], () => seed)
    assert.ok(fromZeroGoals)
    assert.notEqual(fromZeroGoals.stat, 'goals')
    assert.ok(['ovr', 'assists'].includes(fromZeroGoals.stat))

    const fromZeroAssists = pickRound(pool, zeroAssistsOnly, [], () => seed)
    assert.ok(fromZeroAssists)
    assert.notEqual(fromZeroAssists.stat, 'assists')
    assert.ok(['ovr', 'goals'].includes(fromZeroAssists.stat))
  }
})

test('pickRound never returns equal stat values or duplicate players and avoids recent players when possible', () => {
  const pool = [
    player(1, { fc27Overall: 85, goals: 4, assists: 2 }),
    player(2, { fc27Overall: 85, goals: 4, assists: 2 }),
    player(3, { fc27Overall: 82, goals: 7, assists: 5 }),
    player(4, { fc27Overall: 88, goals: 1, assists: 0 }),
    player(5, { fc27Overall: 79, goals: 3, assists: 6 }),
    player(6, { fc27Overall: 91, goals: 9, assists: 1 }),
    player(7, { fc27Overall: 76, goals: 0, assists: 4 }),
  ]

  let cursor = 0
  const seq = [0.05, 0.37, 0.68, 0.91, 0.14, 0.52, 0.83]
  const nextRand = () => seq[cursor++ % seq.length]

  for (let i = 0; i < 40; i += 1) {
    const round = pickRound(pool, pool[0], [3, 4, 5, 6], nextRand)
    assert.ok(round)
    assert.notEqual(round.left.id, round.right.id)
    assert.notEqual(statValue(round.left, round.stat), statValue(round.right, round.stat))
    assert.equal(round.right.id, 7)
  }
})

test('applyGuess evaluates up and down accurately and updates score, status and best', () => {
  const low = player(1, { fc27Overall: 80, goals: 2, assists: 1 })
  const high = player(2, { fc27Overall: 89, goals: 8, assists: 6 })

  const baseHigher = {
    left: low, right: high, stat: 'ovr', recentIds: [2, 1],
    score: 0, best: 0, startBest: 0, newBest: false,
    status: 'play', pick: null, result: null, round: 1, shifted: false,
  }

  const upWin = applyGuess(baseHigher, 'up')
  assert.equal(upWin.status, 'reveal')
  assert.equal(upWin.score, 1)
  assert.equal(upWin.best, 1)
  assert.equal(upWin.newBest, true)
  assert.equal(upWin.result.ok, true)

  const downLose = applyGuess(baseHigher, 'down')
  assert.equal(downLose.status, 'over')
  assert.equal(downLose.score, 0)
  assert.equal(downLose.result.ok, false)

  const baseLower = { ...baseHigher, left: high, right: low, stat: 'goals', best: 3, startBest: 3 }
  const downWin = applyGuess(baseLower, 'down')
  assert.equal(downWin.status, 'reveal')
  assert.equal(downWin.score, 1)
  assert.equal(downWin.best, 3)
  assert.equal(downWin.newBest, false)
  assert.equal(downWin.result.ok, true)

  const upLose = applyGuess(baseLower, 'up')
  assert.equal(upLose.status, 'over')
  assert.equal(upLose.result.ok, false)
})

test('advance shifts right player to left and picks a fresh opponent', () => {
  const pool = [
    player(1, { fc27Overall: 80, goals: 1, assists: 1 }),
    player(2, { fc27Overall: 85, goals: 4, assists: 3 }),
    player(3, { fc27Overall: 90, goals: 9, assists: 7 }),
  ]
  const started = createGame(pool, 0, () => 0)
  assert.ok(started)
  const dir = statValue(started.right, started.stat) > statValue(started.left, started.stat) ? 'up' : 'down'
  const revealed = applyGuess(started, dir)
  const next = advance(revealed, pool, () => 0)
  assert.ok(next)
  assert.equal(next.left.id, started.right.id)
  assert.notEqual(next.right.id, next.left.id)
  assert.equal(next.status, 'play')
  assert.equal(next.round, 2)
  assert.equal(next.shifted, true)
})

test('best score only increases and remains safe when storage throws', () => {
  const store = memoryStorage()
  assert.equal(readBest(store), 0)

  assert.deepEqual(saveBest(4, store), { best: 4, updated: true })
  assert.equal(store.getItem(BEST_SCORE_KEY), '4')
  assert.deepEqual(saveBest(2, store), { best: 4, updated: false })
  assert.equal(readBest(store), 4)
  assert.deepEqual(saveBest(7, store), { best: 7, updated: true })
  assert.equal(readBest(store), 7)

  const broken = failingStorage()
  assert.equal(readBest(broken), 0)
  assert.deepEqual(saveBest(5, broken), { best: 5, updated: true })
})

test('pool too small or without differing values returns null cleanly', () => {
  assert.equal(createGame([], 0), null)
  assert.equal(createGame([player(1)], 0), null)

  const identical = [
    player(1, { fc27Overall: 84, goals: 3, assists: 2 }),
    player(2, { fc27Overall: 84, goals: 3, assists: 2 }),
  ]
  assert.equal(pickRound(identical, null), null)
  assert.equal(createGame(identical, 0), null)
})

test('age uses Vietnam date, foot labels and card tiers match specification', () => {
  // 2026-11-19T17:30:00Z is 2026-11-20 00:30 in Asia/Ho_Chi_Minh.
  const vnMidnight = new Date('2026-11-19T17:30:00Z')
  assert.equal(calculateAge('2000-11-20', vnMidnight), 26)
  assert.equal(calculateAge('2000-11-21', vnMidnight), 25)
  assert.equal(calculateAge('invalid', vnMidnight), null)

  assert.equal(formatFoot('LEFT'), 'Trái')
  assert.equal(formatFoot('RIGHT'), 'Phải')
  assert.equal(formatFoot('BOTH'), 'Hai chân')
  assert.equal(formatFoot('OTHER'), null)

  assert.equal(cardTier(90), 'prism')
  assert.equal(cardTier(85), 'rare')
  assert.equal(cardTier(78), 'gold')
  assert.equal(cardTier(70), 'silver')
  assert.equal(cardTier(69), 'bronze')
})
