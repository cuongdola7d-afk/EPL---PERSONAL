import test from 'node:test'
import assert from 'node:assert/strict'
import { kickoffInstant, matchDateTimeLabel, matchDayKey, compareMatchSchedule } from './matchTime.js'
import { homeFixturePreview } from './homeView.js'
import { clubMatches } from './clubView.js'
import { isValidMatch } from '../api/matches.js'

const fixture = (id, kickoffUtc, date = '2026-10-04') => ({
  id, kickoffUtc, date, status: 'SCHEDULED', matchweek: 6,
  homeClubId: 1, homeClub: 'Arsenal', awayClubId: 2, awayClub: 'Chelsea',
  homeGoals: null, awayGoals: null,
})

test('UTC evening becomes the next Vietnam day with a full date and time', () => {
  const match = fixture(1, '2026-10-04T20:00:00Z')
  assert.equal(matchDateTimeLabel(match, 2026), '05/10/2026 · 03:00')
  assert.equal(matchDayKey(match, 2026), '2026-10-05')
  assert.equal(match.date, '2026-10-04')
  assert.equal(match.kickoffUtc, '2026-10-04T20:00:00Z')
})

test('UTC midday stays on the same Vietnam day and midnight is 00:00', () => {
  assert.equal(matchDateTimeLabel(fixture(1, '2026-10-04T12:00:00Z'), 2026), '04/10/2026 · 19:00')
  assert.equal(matchDayKey(fixture(1, '2026-10-04T12:00:00Z'), 2026), '2026-10-04')
  assert.equal(matchDateTimeLabel(fixture(2, '2026-10-04T17:00:00Z'), 2026), '05/10/2026 · 00:00')
})

test('missing, date-only and unzoned kickoff values never become invented midnight kickoffs', () => {
  for (const kickoffUtc of [undefined, null, '', '2026-10-04', '2026-10-04T20:00:00', 'invalid']) {
    const match = fixture(1, kickoffUtc)
    assert.equal(kickoffInstant(match, 2026), null)
    assert.equal(matchDateTimeLabel(match, 2026), '04/10/2026 · Chưa xác định giờ')
    assert.equal(matchDayKey(match, 2026), '2026-10-04')
  }
  const providerMatch = { ...fixture(1, undefined), utcDate: '2026-10-04T20:00:00Z' }
  assert.equal(matchDateTimeLabel(providerMatch, 2026), '05/10/2026 · 03:00')
})

test('day groups and chronological order use Vietnam dates even when UTC dates differ', () => {
  const late = fixture(1, '2026-10-04T20:00:00Z')
  const later = fixture(2, '2026-10-05T01:00:00Z', '2026-10-05')
  const early = fixture(3, '2026-10-04T12:00:00Z')
  const source = [later, late, early]
  const sorted = [...source].sort((a, b) => compareMatchSchedule(a, b, 2026))
  assert.deepEqual(sorted.map(match => match.id), [3, 1, 2])
  assert.equal(matchDayKey(late, 2026), matchDayKey(later, 2026))
  assert.notEqual(matchDayKey(early, 2026), matchDayKey(late, 2026))
  assert.equal(source[0].id, 2)
})

test('Home and club upcoming date filters include a kickoff moved into today in Vietnam', () => {
  const matches = [fixture(1, '2026-10-04T20:00:00Z'), fixture(2, '2026-10-04T12:00:00Z')]
  assert.equal(homeFixturePreview(matches, '2026-10-05', 2026).mode, 'upcoming')
  assert.deepEqual(clubMatches(matches, 1, '2026-10-05', 2026).soon.map(match => match.id), [1])
})

test('2024 display, date grouping and upcoming filter ignore kickoff timestamps', () => {
  const match = fixture(1, '2024-08-16T20:00:00Z', '2024-08-16')
  assert.equal(kickoffInstant(match, 2024), null)
  assert.equal(matchDateTimeLabel(match, 2024), '16/08/2024')
  assert.equal(matchDayKey(match, 2024), '2024-08-16')
  assert.equal(clubMatches([match], 1, '2024-08-17', 2024).soon.length, 0)
  assert.equal(homeFixturePreview([match], '2024-08-17', 2024).mode, 'scheduled')
})

test('match API validation accepts UTC or missing kickoff, rejects malformed new fields', () => {
  assert.equal(isValidMatch(fixture(1, '2026-10-04T20:00:00Z')), true)
  assert.equal(isValidMatch(fixture(1, null)), true)
  assert.equal(isValidMatch(fixture(1, undefined)), true)
  assert.equal(isValidMatch(fixture(1, '2026-10-04')), false)
  assert.equal(isValidMatch(fixture(1, '2026-10-04T20:00:00')), false)
})
