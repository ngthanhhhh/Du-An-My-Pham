import "./EmptyState.css";

function EmptyState({ message = "Không có dữ liệu" }) {
    return <div className="empty-state">{message}</div>;
}

export default EmptyState;