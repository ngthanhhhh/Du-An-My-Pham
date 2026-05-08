import Header from "../common/Header";
import Footer from "../common/Footer";

function CustomerLayout({ children }) {
    return (
        <>
            <Header />
                <main>{children}</main>
            <Footer />
        </>
    );
}

export default CustomerLayout;