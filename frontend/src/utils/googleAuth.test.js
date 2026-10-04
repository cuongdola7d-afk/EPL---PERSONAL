import test from 'node:test'
import assert from 'node:assert/strict'
import { googleResult, googleReturnPath } from './googleAuth.js'

test('Google results have fixed messages without echoing provider/client parameters', () => {
  assert.equal(googleResult('?google=cancelled&error_description=secret').message, 'Bạn đã hủy đăng nhập Google.')
  assert.match(googleResult('?google=link_required').message, /đăng nhập tài khoản đó/)
  assert.equal(googleResult('?google=untrusted'), null)
  assert.equal(googleResult('?google=confirm_link').result, 'confirm_link')
})

test('OAuth return route preserves the open page/hash while removing the previous result', () => {
  assert.equal(googleReturnPath({ pathname: '/', search: '?google=cancelled&view=compact', hash: '#matches/12?season=2026' }), '/?view=compact#matches/12?season=2026')
})
