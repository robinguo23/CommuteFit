import { Component, StrictMode } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'

class AppErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null }

  static getDerivedStateFromError(error: Error) {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Commute Fit render error', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return <main role="alert" style={{ maxWidth: 640, margin: '12vh auto', padding: 24, fontFamily: 'system-ui, sans-serif', color: '#1f2927' }}>
        <h1>页面暂时无法显示</h1>
        <p>页面发生了意外错误。请刷新后重试；问题已记录在浏览器控制台。</p>
      </main>
    }
    return this.props.children
  }
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppErrorBoundary><App /></AppErrorBoundary>
  </StrictMode>,
)
