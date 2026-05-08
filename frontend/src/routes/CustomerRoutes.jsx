import { Routes, Route } from "react-router-dom";
import CustomerLayout from "../components/layout/CustomerLayout";
import HomePage from "../pages/customer/HomePage";
import ProductListPage from "../pages/customer/ProductListPage";

function CustomerRoutes() {
    return (
    <CustomerLayout>
        <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/products" element={<ProductListPage />} />
        </Routes>
    </CustomerLayout>
    );
}

export default CustomerRoutes;