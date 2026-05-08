import { BrowserRouter, Routes, Route } from "react-router-dom";
import CustomerRoutes from "./CustomerRoutes";
import AdminRoutes from "./AdminRoutes";

function AppRoutes() {
    return (
    <BrowserRouter>
        <Routes>
        <Route path="/*" element={<CustomerRoutes />} />
        <Route path="/admin/*" element={<AdminRoutes />} />
        </Routes>
    </BrowserRouter>
    );
}

export default AppRoutes;