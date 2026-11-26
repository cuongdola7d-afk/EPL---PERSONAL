import test from 'node:test'
import assert from 'node:assert/strict'
import {
  DATA_HEALTH_META,
  formatClosedMembership,
  formatDataHealthCells,
  formatLatestMatchStatus,
  formatMissingFields,
  initialOpenCheckKeys,
  summarizeDataHealth,
} from './adminDataHealth.js'

test('initialOpenCheckKeys opens only checks with count > 0', () => {
  const open = initialOpenCheckKeys([
    { key: 'playedWithoutMembership', count: 2, status: 'WARN' },
    { key: 'rosterNotSelectable', count: 1, status: 'WARN' },
    { key: 'fplUnmapped', count: 0, status: 'OK' },
    { key: 'multipleOpenMemberships', count: 0, status: 'OK' },
    { key: 'playedMissingCore', count: 0, status: 'OK' },
  ])
  assert.deepEqual(open, {
    playedWithoutMembership: true,
    rosterNotSelectable: true,
  })
})

test('summarizeDataHealth returns OK when all checks are zero and WARN otherwise', () => {
  assert.deepEqual(
    summarizeDataHealth({
      checks: [
        { key: 'playedWithoutMembership', count: 0, status: 'OK' },
        { key: 'rosterNotSelectable', count: 0, status: 'OK' },
      ],
    }),
    { status: 'OK', warnCount: 0, label: 'Tất cả ổn' },
  )

  assert.deepEqual(
    summarizeDataHealth({
      checks: [
        { key: 'playedWithoutMembership', count: 2, status: 'WARN' },
        { key: 'rosterNotSelectable', count: 1, status: 'WARN' },
        { key: 'fplUnmapped', count: 0, status: 'OK' },
      ],
    }),
    { status: 'WARN', warnCount: 2, label: '2 mục cần xem' },
  )
})

test('formats playedWithoutMembership status and closed date like mockup', () => {
  assert.equal(
    formatLatestMatchStatus({ gameweek: 12, participationStatus: 'DID_NOT_PLAY', minutes: 0 }),
    'GW12 · dự bị',
  )
  assert.equal(
    formatLatestMatchStatus({ gameweek: 12, participationStatus: 'PLAYED', minutes: 12 }),
    "GW12 · 12'",
  )
  assert.equal(formatClosedMembership('2026-08-31'), 'đóng 31/08')
  assert.equal(formatClosedMembership(null), 'chưa có')
})

test('formats missing fields and table cells for all 5 check keys', () => {
  assert.equal(Object.keys(DATA_HEALTH_META).length, 5)
  assert.equal(formatMissingFields(['POSITION', 'OVR']), 'Vị trí, OVR')
  assert.equal(formatMissingFields(['OVR_RANGE']), 'OVR ngoài 1–99')
  assert.equal(formatMissingFields(['GOALS', 'RATING']), 'Bàn thắng, Rating')

  assert.deepEqual(
    formatDataHealthCells('playedWithoutMembership', {
      playerName: 'Romaine Mundle',
      clubName: 'Sunderland AFC',
      gameweek: 12,
      participationStatus: 'DID_NOT_PLAY',
      minutes: 0,
      lastClosedEndDate: '2026-08-31',
    }),
    ['Romaine Mundle', 'Sunderland AFC', 'GW12 · dự bị', 'đóng 31/08'],
  )

  assert.deepEqual(
    formatDataHealthCells('rosterNotSelectable', {
      playerName: 'Rookie',
      clubName: 'Chelsea FC',
      missing: ['POSITION'],
    }),
    ['Rookie', 'Chelsea FC', 'Vị trí'],
  )

  assert.deepEqual(
    formatDataHealthCells('fplUnmapped', {
      fplName: 'FPL Kid',
      clubName: 'Hull City AFC',
      gameweek: 12,
      fixtureLabel: 'CRY vs HUL',
      minutes: 14,
    }),
    ['FPL Kid', 'Hull City AFC', "GW12 · CRY vs HUL (14')"],
  )

  assert.deepEqual(
    formatDataHealthCells('multipleOpenMemberships', {
      playerName: 'Two Clubs',
      openCount: 2,
      clubs: ['Arsenal FC', 'Chelsea FC'],
    }),
    ['Two Clubs', '2', 'Arsenal FC, Chelsea FC'],
  )

  assert.deepEqual(
    formatDataHealthCells('playedMissingCore', {
      playerName: 'No Rating',
      clubName: 'Hull City AFC',
      gameweek: 11,
      fixtureLabel: 'HUL vs SUN',
      minutes: 45,
      missing: ['RATING'],
    }),
    ['No Rating', 'Hull City AFC', "GW11 · HUL vs SUN (45')", 'Rating'],
  )
})
