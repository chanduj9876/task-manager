import React from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import { store } from '@/app/store'
import { injectStore } from '@/api/axiosClient'
import App from './App'
import './index.css'

// Break circular dependency: inject store into axios client after store is created
injectStore(store)

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>
)
