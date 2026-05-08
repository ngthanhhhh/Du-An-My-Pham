import "./Modal.css";

function Modal({ open, title, children, onClose }) {
    if (!open) return null;

    return (
        <div className="modal-backdrop">
            <div className="modal">
                <div className="modal-header">
                    <h3>{title}</h3>
                    <button className="modal-close" onClick={onClose}>
                        ×
                    </button>
                </div>

                <div className="modal-body">{children}</div>
            </div>
        </div>
    );
}

export default Modal;