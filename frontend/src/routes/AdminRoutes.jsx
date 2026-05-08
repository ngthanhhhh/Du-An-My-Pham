import { Routes, Route } from "react-router-dom";
import AdminLayout from "../components/layout/AdminLayout";
import DashboardPage from "../pages/admin/DashboardPage";
import ProtectedRoute from "./ProtectedRoute";

function AdminRoutes() {
    return (
    <ProtectedRoute>
        <AdminLayout>
        <Routes>
            <Route path="/" element={<DashboardPage />} />
        </Routes>
        </AdminLayout>
    </ProtectedRoute>
    );
}

export default AdminRoutes;