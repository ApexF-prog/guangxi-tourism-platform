// 路由切换顶部进度条：纯 DOM，无第三方依赖。
// 目的只有一个——懒加载后首次进入某页会多一次网络请求，没有反馈会让人以为点空了。
let el = null
let ticker = null
let hideTimer = null
let watchdog = null
let value = 0

function ensure() {
  if (typeof document === 'undefined') return null
  if (el && el.parentNode) return el
  el = document.createElement('div')
  el.id = 'gx-route-progress'
  el.style.cssText =
    'position:fixed;left:0;top:0;height:3px;width:0%;z-index:99999;pointer-events:none;' +
    'background:linear-gradient(90deg,#38bdf8,#818cf8,#34d399);' +
    'box-shadow:0 0 12px rgba(56,189,248,.85);opacity:0;' +
    'transition:width .2s ease,opacity .35s ease;border-radius:0 3px 3px 0'
  document.body.appendChild(el)
  return el
}

function paint(p, opacity) {
  const node = ensure()
  if (!node) return
  node.style.width = p + '%'
  node.style.opacity = String(opacity)
}

export default {
  start() {
    if (typeof document === 'undefined') return
    const node = ensure()
    if (!node) return
    if (hideTimer) { clearTimeout(hideTimer); hideTimer = null }
    if (ticker) { clearInterval(ticker); ticker = null }
    value = 8
    paint(value, 1)
    // 看门狗：导航被守卫中断（next(false)）或分片加载异常时 afterEach 不会触发，
    // 12 秒后强制收条，避免进度条永久挂在页面顶部。
    if (watchdog) clearTimeout(watchdog)
    watchdog = setTimeout(() => { this.done() }, 12000)
    ticker = setInterval(function () {
      // 越接近 90 走得越慢，模拟真实加载手感
      if (value < 90) {
        value += value < 50 ? 9 : value < 75 ? 4 : 1.2
        paint(Math.min(value, 90), 1)
      }
    }, 180)
  },

  done() {
    if (typeof document === 'undefined') return
    if (watchdog) { clearTimeout(watchdog); watchdog = null }
    if (ticker) { clearInterval(ticker); ticker = null }
    paint(100, 1)
    if (hideTimer) clearTimeout(hideTimer)
    hideTimer = setTimeout(function () {
      paint(0, 0)
      value = 0
      hideTimer = null
    }, 260)
  },
}
