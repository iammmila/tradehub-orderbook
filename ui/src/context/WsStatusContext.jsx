import React, { createContext, useContext, useMemo, useState } from "react";

const WsStatusContext = createContext({ status: "disconnected", setStatus: () => { } });

export function WsStatusProvider({ children }) {
    const [status, setStatus] = useState("disconnected");

    const value = useMemo(() => ({ status, setStatus }), [status]);
    return <WsStatusContext.Provider value={value}>{children}</WsStatusContext.Provider>;
}

export function useWsStatus() {
    return useContext(WsStatusContext);
}