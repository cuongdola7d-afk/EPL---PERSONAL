import assert from 'node:assert/strict'
import test from 'node:test'
import { homeFixturePreview, vietnamToday } from './homeView.js'

const fixture = (id, matchweek, date, status = 'SCHEDULED') => ({ id, matchweek, date, status })
test('home uses the next real round, excludes cancellations and keeps results in that round', () => {
  const data = [fixture(7, 7, '2026-10-17'), fixture(1, 6, '2026-10-10', 'CANCELLED'),
    fixture(2, 6, '2026-10-09', 'FINISHED'), fixture(3, 6, '2026-10-10'), fixture(4, 5, '2026-09-20', 'FINISHED')]
  const result = homeFixturePreview(data, '2026-10-04')
  assert.equal(result.week, 6)
  assert.equal(result.mode, 'upcoming')
  assert.deepEqual(result.matches.map(item => item.id), [2, 3])
  assert.equal(data[0].id, 7)
})
test('finished seasons show the last results without inventing upcoming fixtures', () => {
  const result = homeFixturePreview([fixture(2, 37, '2025-05-18', 'FINISHED'),
    fixture(1, 38, '2025-05-25', 'FINISHED')], '2026-10-04')
  assert.equal(result.week, 38)
  assert.equal(result.mode, 'recent')
})
test('old scheduled dates are not labelled upcoming and previews are limited to five', () => {
  const result = homeFixturePreview(Array.from({ length: 10 }, (_, i) => fixture(i + 1, 6, '2026-09-27')), '2026-10-04')
  assert.equal(result.mode, 'scheduled')
  assert.equal(result.matches.length, 5)
})
test('empty and postponed-only lists do not produce invented fixtures', () => {
  assert.deepEqual(homeFixturePreview([], '2026-10-04'), { week: null, mode: 'empty', matches: [] })
  assert.equal(homeFixturePreview([fixture(1, 6, '2026-10-10', 'POSTPONED')], '2026-10-04').week, null)
})
test('home date follows Vietnam when UTC is still on the previous day', () => {
  assert.equal(vietnamToday(new Date('2026-10-03T18:00:00Z')), '2026-10-04')
})
