import { ref, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

/**
 * 文章详情页评论实时推送 WebSocket Hook
 *
 * 用法:
 *   const { newComment, connect, disconnect } = useCommentWebSocket(articleId)
 *   onMounted(() => connect())
 *   onUnmounted(() => disconnect())
 *   watch(newComment, (comment) => { comments.value.unshift(comment) })
 */
export function useCommentWebSocket(articleId) {
  const connected = ref(false)
  const newComment = ref(null)

  let stompClient = null

  const connect = () => {
    if (!articleId) return
    // 防重：已激活的连接不重复建立（onMounted 与 watch(isLoggedIn) 都可能触发 connect）
    if (stompClient && stompClient.active) return

    try {
      const socket = new SockJS('/ws/notify')
      stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          connected.value = true
          const destination = `/topic/article/${articleId}/comments`
          stompClient.subscribe(destination, (message) => {
            try {
              newComment.value = JSON.parse(message.body)
            } catch (e) {
              console.error('[WebSocket] 消息解析失败:', e)
            }
          })
        },
        onDisconnect: () => {
          connected.value = false
        },
        onStompError: (frame) => {
          console.warn('[WebSocket] STOMP 错误:', frame.headers?.['message'] || '未知错误')
          connected.value = false
        },
        // 处理 WebSocket 连接失败（如后端未启动、403 拒绝等）
        onWebSocketError: (event) => {
          console.warn('[WebSocket] 连接失败，实时评论功能不可用（后端未放行 /ws 路径）')
          connected.value = false
        }
      })

      stompClient.activate()
    } catch (e) {
      console.warn('[WebSocket] 初始化失败:', e)
      connected.value = false
    }
  }

  const disconnect = () => {
    if (stompClient) {
      try {
        stompClient.deactivate()
      } catch (e) {
        // ignore
      }
      stompClient = null
      connected.value = false
    }
  }

  onUnmounted(() => disconnect())

  return { connected, newComment, connect, disconnect }
}