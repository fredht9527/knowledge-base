/**
 * [FIX]: 头像代理 composable
 * 解决 wx.qlogo.cn 等外部 CDN 防盗链导致的 ERR_CONNECTION_RESET
 *
 * 原理：当检测到头像 URL 是外部 CDN (如 wx.qlogo.cn) 时，
 * 通过后端代理端点 /api/user/avatar-proxy 转发请求，
 * 携带正确的 Referer 和 User-Agent 头，绕过防盗链检测。
 */

// 需要代理的外部 CDN 域名列表
const PROXY_DOMAINS = [
  'wx.qlogo.cn',
  'thirdwx.qlogo.cn',
  'wx.qq.com',
]

/**
 * 判断 URL 是否需要通过后端代理加载
 */
function needsProxy(url) {
  if (!url || typeof url !== 'string') return false
  try {
    const hostname = new URL(url).hostname
    return PROXY_DOMAINS.some(domain => hostname === domain || hostname.endsWith('.' + domain))
  } catch {
    return false
  }
}

/**
 * 将外部头像 URL 转为后端代理 URL
 * 如果是本地上传的头像或不需要代理的 URL，则原样返回
 */
export function proxyAvatarUrl(url) {
  if (!url) return ''
  if (!needsProxy(url)) return url
  return `/api/user/avatar-proxy?url=${encodeURIComponent(url)}`
}

/**
 * 头像加载失败时的兜底处理
 * 隐藏失败的 img，让父容器的文字占位符显示出来
 */
export function onAvatarError(event) {
  const img = event.target
  img.style.display = 'none'
  const parent = img.parentElement
  if (!parent) return
  const placeholder = parent.querySelector(
    '.user-avatar-text, .user-msg-avatar-text, .avatar-placeholder, .avatar-text'
  )
  if (placeholder) {
    placeholder.style.display = 'flex'
  }
}
