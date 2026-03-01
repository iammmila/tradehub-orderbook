import React, { useCallback, useMemo, useState } from "react";
import "./TradingPage.scss";

import OrderBookPanel from "../../../components/TradingPage/OrderBookPanel/OrderBookPanel";
import OrderEntryCard from "../../../components/TradingPage/OrderEntryCard/OrderEntryCard";
import OpenOrdersWidget from "../../../components/TradingPage/OpenOrdersWidget/OpenOrdersWidget";
import Select from "../../../components/Dashboard/Tables/Select/Select";

import useOrderBookPolling from "../../../hooks/useOrderBookPolling";

const DEFAULT_INSTRUMENTS = ["TST1", "BTC", "ETH", "AAPL", "TSLA"];

const TradingPage = () => {
    const [instrument, setInstrument] = useState("TST1");
    const [instrumentDraft, setInstrumentDraft] = useState("TST1");

    const [autoRefresh, setAutoRefresh] = useState(true);
    const [showLevels, setShowLevels] = useState(false);

    const [prefill, setPrefill] = useState(null);
    const [openOrdersKey, setOpenOrdersKey] = useState(0);

    const bumpOpenOrders = useCallback(() => {
        setOpenOrdersKey((k) => k + 1);
    }, []);

    const applyInstrument = useCallback(() => {
        const next = String(instrumentDraft || "").trim().toUpperCase();
        if (!next) return;
        setInstrument(next);
    }, [instrumentDraft]);

    const { book, loading, refreshing, error, refreshNow } = useOrderBookPolling({
        instrument,
        showLevels,
        pollMs: 2000,
        enabled: autoRefresh,
    });

    const subtitle = useMemo(() => {
        if (!instrument) return "Select an instrument to load orderbook";
        if (loading) return "Loading…";
        if (error) return `Error: ${String(error)}`;
        if (refreshing) return "Updating…";
        return "Live orderbook";
    }, [instrument, loading, refreshing, error]);

    const onManualRefresh = useCallback(() => {
        refreshNow?.();
        bumpOpenOrders();
    }, [refreshNow, bumpOpenOrders]);

    return (
        <div className="tradingPage">
            <div className="tradingHeader">
                <div className="tradingHeader__left">
                    <div className="tradingHeader__title">Trading</div>
                    <div className="tradingHeader__subtitle">{subtitle}</div>
                </div>

                <div className="tradingHeader__right">
                    <div className="instrumentPicker">
                        <div className="instrumentPicker__label">Instrument</div>

                        <div className="instrumentPicker__controls">
                            <Select
                                label={null}
                                width={110}
                                value={instrument}
                                onChange={(v) => {
                                    setInstrumentDraft(v);
                                    setInstrument(v);
                                }}
                                options={DEFAULT_INSTRUMENTS.map((x) => ({ label: x, value: x }))}
                            />

                            <input
                                className="input instrumentInput"
                                value={instrumentDraft}
                                onChange={(e) => setInstrumentDraft(e.target.value)}
                                onKeyDown={(e) => {
                                    if (e.key === "Enter") applyInstrument();
                                }}
                                placeholder="Type and press Enter…"
                            />

                            <button className="ordersBtn ordersBtn--secondary" type="button" onClick={applyInstrument}>
                                Apply
                            </button>
                        </div>

                        <div className="instrumentQuick">
                            {DEFAULT_INSTRUMENTS.map((x) => (
                                <button
                                    key={x}
                                    className={`chip ${instrument === x ? "chip--active" : ""}`}
                                    onClick={() => {
                                        setInstrumentDraft(x);
                                        setInstrument(x);
                                    }}
                                    type="button"
                                >
                                    {x}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="tradingHeader__actions">
                        <label className="toggle">
                            <input type="checkbox" checked={autoRefresh} onChange={(e) => setAutoRefresh(e.target.checked)} />
                            <span className="toggle__track" />
                            <span className="toggle__text">Auto</span>
                        </label>

                        <button className="ordersBtn ordersBtn--secondary" onClick={onManualRefresh} type="button">
                            Refresh
                        </button>
                    </div>
                </div>
            </div>

            <div className="tradingGrid">
                <OrderBookPanel
                    instrument={instrument}
                    book={book}
                    loading={loading}
                    refreshing={refreshing}
                    error={error}
                    showLevels={showLevels}
                    onToggleLevels={(v) => {
                        setShowLevels(v);
                        // immediately refresh when toggle changes (best UX)
                        refreshNow?.();
                    }}
                    onPickPrice={(side, price) => setPrefill({ side, price })}
                />

                <div className="tradingRight">
                    <OrderEntryCard
                        instrument={instrument}
                        prefill={prefill}
                        onSubmitted={() => {
                            refreshNow?.();
                            bumpOpenOrders();
                        }}
                    />

                    <OpenOrdersWidget
                        instrument={instrument}
                        refreshKey={openOrdersKey}
                        onChanged={() => {
                            refreshNow?.();
                            bumpOpenOrders();
                        }}
                    />
                </div>
            </div>
        </div>
    );
};

export default TradingPage;