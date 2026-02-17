import { Outlet } from "react-router-dom";
import "./PublicRoot.scss";
import Navbar from "../../../components/Navbar/Navbar";
import Footer from "../../../components/Footer/Footer";

export default function PublicRoot() {
    return (
        <div className='app-shell'>
            <Navbar />
            <main className='main-content'>
                <Outlet />
            </main>
            <Footer />
        </div>
    );
}