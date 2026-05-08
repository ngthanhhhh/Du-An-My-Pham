import "./Input.css";

function Input({ label, error, ...props }) {
    return (
        <div className="input-group">
            {label && <label className="input-label">{label}</label>}
            <input className="input" {...props} />
            {error && <p className="input-error">{error}</p>}
        </div>
    );
}

export default Input;