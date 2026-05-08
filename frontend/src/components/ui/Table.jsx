import "./Table.css";

function Table({ columns = [], data = [], renderRow }) {
    return (
        <table className="table">
            <thead>
                <tr>
                    {columns.map((col) => (
                        <th key={col}>{col}</th>
                    ))}
                </tr>
            </thead>

            <tbody>
                {data.length === 0 ? (
                    <tr>
                        <td colSpan={columns.length} className="table-empty">
                            Không có dữ liệu
                        </td>
                    </tr>
                ) : (
                    data.map(renderRow)
                )}
            </tbody>
        </table>
    );
}

export default Table;