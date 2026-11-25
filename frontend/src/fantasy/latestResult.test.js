import test from 'node:test'
import assert from 'node:assert/strict'
import {
  dismissLatestResult,
  dismissStorageKey,
  formatCardPoints,
  formatOpenDeadlineFriendly,
  formatPublishedDayMonth,
  formatSeasonRankChange,
  isLatestResultDismissed,
  shouldShowLatestResultCard,
} from './latestResult.js'

function memoryStorage() {
  const map = new Map()
  return {
    getItem: key => (map.has(key) ? map.get(key) : null),
    setItem: (key, value) => { map.set(key, String(value)) },
  }
}

test('latest result card formats points, Vietnam published date, friendly open deadline and season rank change', () => {
  assert.equal(formatCardPoints(64.5), '64.5')
  assert.equal(formatCardPoints(71), '71.0')
  assert.equal(formatCardPoints(10.0), '10.0')
  assert.equal(formatCardPoints(64.55), '64.55')
  assert.equal(formatCardPoints(null), '–')

  // 2026-11-24T16:52:52Z is 24/11 23:52 VN; 2026-11-24T17:10:00Z rolls to 25/11 VN
  assert.equal(formatPublishedDayMonth('2026-11-24T16:52:52Z'), '24/11')
  assert.equal(formatPublishedDayMonth('2026-11-24T17:10:00Z'), '25/11')

  // GW12 deadline 2026-11-27T19:30:00Z -> 02:30 Thứ Bảy 28/11
  assert.equal(formatOpenDeadlineFriendly('2026-11-27T19:30:00Z'), '02:30 Thứ Bảy 28/11')

  assert.equal(formatSeasonRankChange(2), '↑2')
  assert.equal(formatSeasonRankChange(-1), '↓1')
  assert.equal(formatSeasonRankChange(0), '=')
  assert.equal(formatSeasonRankChange(null), null)
})

test('dismissing hides only the exact published gameweek and reappears when a newer gameweek is published', () => {
  const storage = memoryStorage()
  assert.equal(dismissStorageKey(11), 'prismaxi:fantasy:latest-result-dismissed:2026:gw11')
  assert.equal(isLatestResultDismissed(11, storage), false)

  assert.equal(dismissLatestResult(11, storage), true)
  assert.equal(isLatestResultDismissed(11, storage), true)
  assert.equal(isLatestResultDismissed(12, storage), false)

  // Throwing storage is safely swallowed without crashing.
  const broken = {
    getItem: () => { throw new Error('blocked') },
    setItem: () => { throw new Error('blocked') },
  }
  assert.equal(isLatestResultDismissed(11, broken), false)
  assert.equal(dismissLatestResult(11, broken), false)
})

test('visibility hides card when no published GW, when viewing that GW, or when dismissed by logged-in user', () => {
  assert.equal(shouldShowLatestResultCard({ summary: null, viewingGameweek: 12, isGuest: false, dismissed: false }), false)
  assert.equal(shouldShowLatestResultCard({ summary: { gameweek: null, participants: 0 }, viewingGameweek: 12, isGuest: false, dismissed: false }), false)
  assert.equal(shouldShowLatestResultCard({ summary: { gameweek: 11, participants: 3 }, viewingGameweek: 11, isGuest: false, dismissed: false }), false)
  assert.equal(shouldShowLatestResultCard({ summary: { gameweek: 11, participants: 3 }, viewingGameweek: 12, isGuest: false, dismissed: true }), false)
  assert.equal(shouldShowLatestResultCard({ summary: { gameweek: 11, participants: 3 }, viewingGameweek: 12, isGuest: false, dismissed: false }), true)
  // Guest has no dismiss control, so card remains visible for another GW
  assert.equal(shouldShowLatestResultCard({ summary: { gameweek: 11, participants: 3 }, viewingGameweek: 12, isGuest: true, dismissed: true }), true)
})

test('visibility hides card when latest published GW has zero participants', () => {
  assert.equal(shouldShowLatestResultCard({ summary: { gameweek: 11, participants: 0 }, viewingGameweek: 12, isGuest: true, dismissed: false }), false)
  assert.equal(shouldShowLatestResultCard({ summary: { gameweek: 11, participants: 0 }, viewingGameweek: 12, isGuest: false, dismissed: false }), false)
})
