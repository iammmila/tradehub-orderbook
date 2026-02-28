import React, { useCallback, useEffect, useMemo, useState } from "react";
import "./TradingPage.scss";

import OrderBookPanel from "../../../components/TradingPage/OrderBookPanel/OrderBookPanel";
import OrderEntryCard from "../../../components/TradingPage/OrderEntryCard/OrderEntryCard";
import OpenOrdersWidget from "../../../components/TradingPage/OpenOrdersWidget/OpenOrdersWidget";
import Select from "../../../components/Dashboard/Tables/Select/Select";

import { fetchOrderBook } from "../../../api/orderbook";
import { formatDate, formatTime } from "../../../utils/formatter";

const DEFAULT_INSTRUMENTS = ["TST1", "BTC", "ETH", "AAPL", "TSLA"];

const TradingPage = () => {
    const [instrument, setInstrument] = useState("TST1");        // active instrument
    const [instrumentDraft, setInstrumentDraft] = useState("TST1"); // typed value

    const [autoRefresh, setAutoRefresh] = useState(true);

    const [book, setBook] = useState(null);
    const [bookLoading, setBookLoading] = useState(true);
    const [bookError, setBookError] = useState(null);
    const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

    const [prefill, setPrefill] = useState(null);
    const [openOrdersKey, setOpenOrdersKey] = useState(0);

    const bumpOpenOrders = useCallback(() => {
        setOpenOrdersKey((k) => k + 1);
    }, []);

    const loadBook = useCallback(
        async ({ showSpinner } = { showSpinner: false }) => {
            if (!instrument) return;

            try {
                if (showSpinner) setBookLoading(true);
                setBookError(null);

                const data = await fetchOrderBook(instrument.trim());
                setBook(data);
                setLastUpdatedAt(new Date());
            } catch (e) {
                setBookError(e?.response?.data?.message || e?.message || "Failed to load orderbook");
            } finally {
                if (showSpinner) setBookLoading(false);
            }
        },
        [instrument]
    );

    const applyInstrument = useCallback(() => {
        const next = instrumentDraft.trim().toUpperCase();
        if (!next) return;
        setInstrument(next);
    }, [instrumentDraft]);

    useEffect(() => {
        loadBook({ showSpinner: true });
        bumpOpenOrders();
    }, [instrument, loadBook, bumpOpenOrders]);

    useEffect(() => {
        if (!autoRefresh) return;
        const id = setInterval(() => loadBook({ showSpinner: false }), 2000);
        return () => clearInterval(id);
    }, [autoRefresh, loadBook]);

    const subtitle = useMemo(() => {
        if (bookLoading) return "Loading…";
        if (bookError) return "Error";
        if (!lastUpdatedAt) return "";
        return `Last updated: ${formatDate(lastUpdatedAt)} ${formatTime(lastUpdatedAt)}`;
    }, [bookLoading, bookError, lastUpdatedAt]);

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
                                    setInstrument(v); // triggers effect above
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

                        <button className="ordersBtn ordersBtn--secondary" onClick={() => loadBook({ showSpinner: true })} type="button">
                            Refresh
                        </button>
                    </div>
                </div>
            </div>

            <div className="tradingGrid">
                <OrderBookPanel
                    instrument={instrument}
                    book={book}
                    loading={bookLoading}
                    error={bookError}
                    onPickPrice={(side, price) => setPrefill({ side, price })}
                />

                <div className="tradingRight">
                    <OrderEntryCard
                        instrument={instrument}
                        prefill={prefill}
                        onSubmitted={() => {
                            loadBook({ showSpinner: false });
                            bumpOpenOrders();
                        }}
                    />

                    <OpenOrdersWidget
                        instrument={instrument}
                        refreshKey={openOrdersKey}
                        onChanged={() => {
                            loadBook({ showSpinner: false });
                            bumpOpenOrders();
                        }}
                    />
                </div>
            </div>
        </div>
    );
};

export default TradingPage;