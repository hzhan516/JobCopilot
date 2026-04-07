import React from 'react'
import ReactDOM from 'react-dom/client'

// 占位应用，后续替换为实际前端代码
function App() {
    return (
        <div style={{padding: '20px', fontFamily: 'Arial'}}>
            <h1>智能求职助手</h1>
            <p>前端服务正在开发中...</p>
            <p>Backend API: <a href="http://localhost:8080/api">http://localhost:8080/api</a></p>
        </div>
    )
}

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <App/>
    </React.StrictMode>,
)
