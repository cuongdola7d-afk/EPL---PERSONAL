function ResultPanel({ status, error, count, itemName, emptyMessage, onRetry, onClear, children }) {
  return (
    <div className="results" aria-live="polite" aria-busy={status === 'loading'}>
      {status === 'loading' && (
        <div className="state-panel">
          <span className="spinner" aria-hidden="true" />
          <h3>Đang tải {itemName}...</h3>
          <p>PremierHub đang lấy dữ liệu từ API.</p>
        </div>
      )}

      {status === 'error' && (
        <div className="state-panel state-error" role="alert">
          <span className="state-symbol" aria-hidden="true">!</span>
          <h3>Không thể tải {itemName}</h3>
          <p>{error} Hãy kiểm tra backend tại localhost:8080 rồi thử lại.</p>
          <button type="button" onClick={onRetry}>Thử lại</button>
        </div>
      )}

      {status === 'success' && count === 0 && (
        <div className="state-panel">
          <span className="state-symbol" aria-hidden="true">?</span>
          <h3>Không có kết quả</h3>
          <p>{emptyMessage}</p>
          {onClear && <button type="button" onClick={onClear}>Xóa bộ lọc</button>}
        </div>
      )}

      {status === 'success' && count > 0 && children}
    </div>
  )
}

export default ResultPanel
