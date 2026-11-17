// 全局运行时兜底：组件渲染异常 / 断网 / Promise 未捕获都给用户明确反馈，
// 并把最近 30 条留在 localStorage，便于「运行诊断」排查，绝不整页白屏无提示。
const LOG_KEY = 'gx_runtime_errors'
const MAX = 30

function pushLog(kind, msg) {
  try {
    const raw = JSON.parse(localStorage.getItem(LOG_KEY) || '[]')
    const rows = Array.isArray(raw) ? raw : []
    rows.unshift({
      t: new Date().toISOString(),
      kind: kind,
      msg: String(msg || '').slice(0, 300),
    })
    localStorage.setItem(LOG_KEY, JSON.stringify(rows.slice(0, MAX)))
  } catch (e) { /* 存储不可用不影响主流程 */ }
}

export function readErrorLogs() {
  try {
    const rows = JSON.parse(localStorage.getItem(LOG_KEY) || '[]')
    return Array.isArray(rows) ? rows : []
  } catch (e) { return [] }
}

export function clearErrorLogs() {
  try { localStorage.removeItem(LOG_KEY) } catch (e) { /* noop */ }
}

const TONE = {
  info: { color: '#38bdf8', bg: 'rgba(9,22,44,.96)' },
  warn: { color: '#fbbf24', bg: 'rgba(35,26,9,.96)' },
  ok: { color: '#34d399', bg: 'rgba(9,32,25,.96)' },
  error: { color: '#fb7185', bg: 'rgba(38,12,22,.96)' },
}

let current = null
let timer = null

/** 顶部非阻塞提示条，支持自定义操作按钮（刷新 / 回首页）。 */
export function notify(text, tone = 'info', actions = []) {
  if (typeof document === 'undefined') return
  if (current && current.parentNode) current.remove()
  if (timer) { clearTimeout(timer); timer = null }

  const c = TONE[tone] || TONE.info
  const box = document.createElement('div')
  box.setAttribute('role', 'status')
  box.style.cssText =
    'position:fixed;top:14px;left:50%;transform:translateX(-50%);z-index:2147483647;' +
    'display:flex;align-items:center;gap:12px;max-width:min(92vw,720px);' +
    'padding:12px 16px;border-radius:14px;color:#eaf6ff;font-size:13px;line-height:1.5;' +
    'background:' + c.bg + ';border:1px solid ' + c.color + '66;' +
    'box-shadow:0 12px 36px rgba(0,0,0,.45);backdrop-filter:blur(12px);'

  const label = document.createElement('span')
  label.textContent = text
  label.style.cssText = 'flex:1;min-width:0;'
  box.appendChild(label)

  actions.forEach(function (a) {
    const b = document.createElement('button')
    b.type = 'button'
    b.textContent = a.label
    b.style.cssText =
      'flex:none;padding:6px 12px;border-radius:9px;cursor:pointer;font-size:12px;' +
      'color:' + c.color + ';background:transparent;border:1px solid ' + c.color + '88;'
    b.onclick = a.onClick
    box.appendChild(b)
  })

  document.body.appendChild(box)
  current = box
  timer = setTimeout(function () {
    if (box.parentNode) box.remove()
    if (current === box) current = null
    timer = null
  }, 6500)
}

export function installRuntimeGuard(app) {
  app.config.errorHandler = function (err, _instance, info) {
    pushLog('vue', (err && err.message) || err)
    console.error('[GX] 组件渲染异常', err, info)
    notify('页面局部渲染异常，可刷新或返回首页', 'error', [
      { label: '刷新', onClick: () => location.reload() },
      { label: '回首页', onClick: () => { location.href = '/' } },
    ])
  }

  window.addEventListener('error', function (e) {
    pushLog('window', e.message || (e.error && e.error.message) || '')
  })
  window.addEventListener('unhandledrejection', function (e) {
    const r = e.reason
    pushLog('promise', (r && r.message) || r)
  })
  window.addEventListener('offline', function () {
    pushLog('network', 'offline')
    notify('网络已断开，部分数据可能不是最新', 'warn')
  })
  window.addEventListener('online', function () {
    notify('网络已恢复', 'ok')
  })
}

export default { installRuntimeGuard, notify, readErrorLogs, clearErrorLogs }
