import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const leagueId = 39
const seasonYear = Number(process.argv[2] ?? 2024)
const gameweek = Number(process.argv[3] ?? 1)
const maxRequests = 8
const baseUrl = 'https://v3.football.api-sports.io'
const localEnvFile = fileURLToPath(new URL('../.env.local', import.meta.url))

if (!Number.isInteger(seasonYear) || seasonYear < 2000 ||
    !Number.isInteger(gameweek) || gameweek < 1 || gameweek > 38) {
  console.error('Cách dùng: node scripts/verify-api-football.mjs [seasonYear] [gameweek]')
  process.exit(1)
}

function apiKey() {
  if (process.env.API_FOOTBALL_KEY?.trim()) return process.env.API_FOOTBALL_KEY.trim()

  try {
    const content = readFileSync(localEnvFile, 'utf8')
    const match = content.match(/^\s*API_FOOTBALL_KEY\s*=\s*(.*?)\s*$/m)
    return match?.[1].replace(/^['"]|['"]$/g, '').trim() || null
  } catch (error) {
    if (error.code === 'ENOENT') return null
    throw error
  }
}

const key = apiKey()
if (!key) {
  console.error('Thiếu API_FOOTBALL_KEY trong môi trường hoặc backend/.env.local.')
  process.exit(1)
}

let requestsUsed = 0
let dailyRemaining = 'unknown'

async function get(path) {
  if (requestsUsed >= maxRequests) throw new Error('Đã đạt giới hạn request của lần kiểm chứng.')
  if (requestsUsed > 0) await new Promise((resolve) => setTimeout(resolve, 6500))

  const response = await fetch(`${baseUrl}/${path}`, {
    headers: { 'x-apisports-key': key, Accept: 'application/json' },
    signal: AbortSignal.timeout(20000),
  })
  requestsUsed++
  dailyRemaining = response.headers.get('x-ratelimit-requests-remaining') ?? 'unknown'

  const body = await response.json()
  const errorNames = body.errors ? Object.keys(body.errors) : []
  console.log(`GET /${path}: HTTP ${response.status}, results=${body.results ?? 'unknown'}, ` +
    `page=${body.paging?.current ?? 'unknown'}/${body.paging?.total ?? 'unknown'}, ` +
    `daily remaining=${dailyRemaining}`)
  if (!response.ok || errorNames.length > 0 || !Array.isArray(body.response)) {
    const providerErrors = errorNames.map((name) =>
      `${name}: ${String(body.errors[name]).replaceAll(key, '[REDACTED]')}`)
    throw new Error(`API từ chối hoặc trả response không hợp lệ tại /${path}; ` +
      `errors: ${providerErrors.join('; ') || 'none'}`)
  }
  return body
}

function countNull(rows, value) {
  return rows.filter((row) => value(row) == null).length
}

try {
  const leagues = await get(`leagues?id=${leagueId}&season=${seasonYear}`)
  const league = leagues.response.find((item) => item.league?.id === leagueId)
  const season = league?.seasons?.find((item) => item.year === seasonYear)
  if (!season) throw new Error(`Tài khoản không trả về mùa Premier League ${seasonYear}/${seasonYear + 1}.`)

  const coverage = season.coverage
  console.log('Coverage:', JSON.stringify({
    current: season.current,
    standings: coverage?.standings,
    players: coverage?.players,
    fixturePlayerStatistics: coverage?.fixtures?.statistics_players,
  }))
  if (!coverage?.standings || !coverage?.players || !coverage?.fixtures?.statistics_players) {
    throw new Error('Ít nhất một loại dữ liệu thiết yếu không có coverage cho mùa này.')
  }

  const teams = await get(`teams?league=${leagueId}&season=${seasonYear}`)
  console.log(`Teams: count=${teams.response.length}, missing ID=${countNull(teams.response, (row) => row.team?.id)}, ` +
    `missing name=${countNull(teams.response, (row) => row.team?.name)}, ` +
    `missing city=${countNull(teams.response, (row) => row.venue?.city)}`)

  const round = encodeURIComponent(`Regular Season - ${gameweek}`)
  const fixtures = await get(`fixtures?league=${leagueId}&season=${seasonYear}&round=${round}`)
  const statuses = fixtures.response.reduce((counts, row) => {
    const status = row.fixture?.status?.short ?? 'missing'
    counts[status] = (counts[status] ?? 0) + 1
    return counts
  }, {})
  console.log(`Fixtures gameweek ${gameweek}: count=${fixtures.response.length}, ` +
    `statuses=${JSON.stringify(statuses)}, ` +
    `missing round=${countNull(fixtures.response, (row) => row.league?.round)}, ` +
    `missing date=${countNull(fixtures.response, (row) => row.fixture?.date)}, ` +
    `missing score=${countNull(fixtures.response, (row) => row.goals?.home == null || row.goals?.away == null ? null : true)}`)

  const standings = await get(`standings?league=${leagueId}&season=${seasonYear}`)
  const standingRows = standings.response.flatMap((item) => item.league?.standings?.flat() ?? [])
  console.log(`Standings: rows=${standingRows.length}, ` +
    `missing rank=${countNull(standingRows, (row) => row.rank)}, ` +
    `missing team ID=${countNull(standingRows, (row) => row.team?.id)}`)

  const players = await get(`players?league=${leagueId}&season=${seasonYear}&page=1`)
  const playerStats = players.response.flatMap((row) => row.statistics?.filter(
    (entry) => entry.league?.id === leagueId) ?? [])
  console.log(`Players page 1: rows=${players.response.length}, total pages=${players.paging?.total ?? 'unknown'}, ` +
    `league stat blocks=${playerStats.length}, ` +
    `missing position=${countNull(playerStats, (row) => row.games?.position)}, ` +
    `null goals=${countNull(playerStats, (row) => row.goals?.total)}, ` +
    `null assists=${countNull(playerStats, (row) => row.goals?.assists)}`)

  const finished = fixtures.response
    .filter((row) => ['FT', 'AET', 'PEN'].includes(row.fixture?.status?.short))
    .sort((a, b) => (b.fixture?.date ?? '').localeCompare(a.fixture?.date ?? ''))
  if (finished.length === 0) throw new Error('Không tìm thấy trận đã kết thúc để kiểm tra thống kê cầu thủ.')

  let foundMatchStats = false
  for (const fixture of finished.slice(0, 3)) {
    const fixtureId = fixture.fixture.id
    const matchStats = await get(`fixtures/players?fixture=${fixtureId}`)
    const matchPlayers = matchStats.response.flatMap((team) => team.players ?? [])
    const statBlocks = matchPlayers.flatMap((row) => row.statistics ?? [])
    console.log(`Fixture ${fixtureId}: teams=${matchStats.response.length}, players=${matchPlayers.length}, ` +
      `stat blocks=${statBlocks.length}, null goals=${countNull(statBlocks, (row) => row.goals?.total)}, ` +
      `null assists=${countNull(statBlocks, (row) => row.goals?.assists)}`)
    if (matchPlayers.length > 0 && statBlocks.length > 0) {
      foundMatchStats = true
      break
    }
  }
  if (!foundMatchStats) throw new Error('Không có thống kê cầu thủ trong ba trận hoàn tất gần nhất đã kiểm tra.')
  console.log('Kiểm chứng đủ năm nhóm dữ liệu hoàn tất.')
} catch (error) {
  console.error(`Kiểm chứng dừng: ${error.message.replaceAll(key, '[REDACTED]')}`)
  process.exitCode = 1
} finally {
  console.log(`Requests used=${requestsUsed}/${maxRequests}, daily remaining=${dailyRemaining}`)
}
