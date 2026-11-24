import test from 'node:test'
import assert from 'node:assert/strict'
import {
  compareMatchSchedule,
  formatDayGroupHeading,
  formatVietnamTimeHHmm,
  kickoffInstant,
  matchDateTimeLabel,
  matchDayKey,
  matchKickoffParts,
  matchWeekdayDateLabel,
} from './matchTime.js'
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

test('formats Vietnamese weekday + dd/mm and day group headings with relative labels', () => {
  // 2026-11-21 15:00Z = 22:00 Vietnam time, Saturday 21/11/2026
  const satMatch = fixture(1, '2026-11-21T15:00:00Z', '2026-11-21')
  assert.equal(matchWeekdayDateLabel(satMatch, 2026), 'Thứ Bảy 21/11')
  const satParts = matchKickoffParts(satMatch, 2026)
  assert.equal(satParts.time, '22:00')
  assert.equal(satParts.weekdayDate, 'Thứ Bảy 21/11')
  assert.equal(satParts.shortDate, '21/11')

  // 2026-11-21 17:30Z = 00:30 Vietnam time, Sunday 22/11/2026
  const sunEarly = fixture(2, '2026-11-21T17:30:00Z', '2026-11-21')
  assert.equal(matchWeekdayDateLabel(sunEarly, 2026), 'Chủ nhật 22/11')
  assert.equal(matchKickoffParts(sunEarly, 2026).time, '00:30')

  // 2026-11-23 20:00Z = 03:00 Vietnam time, Tuesday 24/11/2026
  const tueEarly = fixture(3, '2026-11-23T20:00:00Z', '2026-11-23')
  assert.equal(matchWeekdayDateLabel(tueEarly, 2026), 'Thứ Ba 24/11')
  assert.equal(matchKickoffParts(tueEarly, 2026).time, '03:00')

  // Relative day labels in Vietnam timezone (now = 2026-11-24 01:20 VN = 2026-11-23T18:20:00Z)
  const nowVn = new Date('2026-11-23T18:20:00Z')
  assert.deepEqual(formatDayGroupHeading('2026-11-24', nowVn), { label: 'Thứ Ba, 24/11/2026', relative: 'Hôm nay' })
  assert.deepEqual(formatDayGroupHeading('2026-11-25', nowVn), { label: 'Thứ Tư, 25/11/2026', relative: 'Ngày mai' })
  assert.deepEqual(formatDayGroupHeading('2026-11-23', nowVn), { label: 'Thứ Hai, 23/11/2026', relative: 'Hôm qua' })
  assert.deepEqual(formatDayGroupHeading('2026-11-21', nowVn), { label: 'Thứ Bảy, 21/11/2026', relative: '' })
})

test('formatVietnamTimeHHmm converts UTC ISO timestamp to Vietnam HH:mm and handles null/invalid', () => {
  assert.equal(formatVietnamTimeHHmm('2026-11-22T19:30:00Z'), '02:30')
  assert.equal(formatVietnamTimeHHmm('2026-11-22T12:05:00Z'), '19:05')
  assert.equal(formatVietnamTimeHHmm(null), null)
  assert.equal(formatVietnamTimeHHmm(''), null)
  assert.equal(formatVietnamTimeHHmm('invalid'), null)
})
