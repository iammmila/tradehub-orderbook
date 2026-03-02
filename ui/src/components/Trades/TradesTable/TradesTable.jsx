import React, { useMemo, useState } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import TableFilters from "../../Dashboard/Tables/TableFilters/TableFilters";
import Select from "../../Dashboard/Tables/Select/Select";
import "../../Dashboard/Tables/TableBase.scss";
import "./TradesTable.scss";

import { formatDate, formatTime } from "../../../utils/formatter";
import { useTradesPage } from "../../../hooks/useTradesPage";
import { useUsernamesMap } from "../../../hooks/useUsernamesMap";
import TradesGrid from "../TradesGrid/TradesGrid";

const TradesTable = () => {
  const [search, setSearch] = useState("");
  const [instrumentApi, setInstrumentApi] = useState("");

  const {
    loading,
    error,
    rows,
    page,
    size,
    pageInfo,
    lastUpdatedAt,
    sortBy,
    sortDir,
    toggleSort,
    setPage,
    setSize,
    resetSort,
    reload,
  } = useTradesPage({ instrumentApi });

  const usernames = useUsernamesMap(rows);

  const filteredRows = useMemo(() => {
    const q = search.trim().toUpperCase();
    if (!q) return rows;
    return (rows || []).filter((t) =>
      String(t.instrument || "").toUpperCase().includes(q)
    );
  }, [rows, search]);

  const totalElements = pageInfo.totalElements ?? 0;
  const lastPageIndex = totalElements > 0 ? Math.max(0, Math.ceil(totalElements / size) - 1) : 0;
  const isFirstPage = page <= 0;
  const isLastPage = totalElements > 0 ? page >= lastPageIndex : (rows?.length ?? 0) < size;

  const reset = () => {
    setSearch("");
    setInstrumentApi("");
    resetSort();
  };

  return (
    <TableCard
      title="My Trades"
      subtitle={
        loading
          ? "Loading..."
          : error
            ? "Error"
            : lastUpdatedAt
              ? `Last updated: ${formatDate(lastUpdatedAt)} ${formatTime(lastUpdatedAt)}`
              : ""
      }
    >
      <TableFilters
        search={search}
        onSearch={setSearch}
        placeholder="Search instrument (client-side)…"
        right={
          <div className="tradesTable__topActions">
            <button className="ordersBtn ordersBtn--secondary" onClick={reset}>
              Reset
            </button>
            <button className="ordersBtn ordersBtn--secondary" onClick={reload}>
              Refresh
            </button>
          </div>
        }
      />

      {error && (
        <div className="table-error">
          <div>{error}</div>
          <button className="ordersBtn ordersBtn--secondary" onClick={reload}>
            Retry
          </button>
        </div>
      )}

      <TradesGrid
        loading={loading}
        rows={filteredRows}
        sortBy={sortBy}
        sortDir={sortDir}
        onSort={toggleSort}
        usernames={usernames}
      />

      <div className="ordersTable__footer">
        <div className="ordersTable__pager">
          <button
            className="ordersBtn ordersBtn--secondary"
            disabled={loading || isFirstPage}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Prev
          </button>

          <div className="ordersTable__pageInfo">
            Page <b>{page + 1}</b> / <b>{Math.max(lastPageIndex + 1, 1)}</b>
          </div>

          <button
            className="ordersBtn ordersBtn--secondary"
            disabled={loading || isLastPage}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>

        <div className="ordersTable__pageSize">
          <Select
            label="Rows"
            value={String(size)}
            onChange={(v) => {
              setPage(0);
              setSize(Number(v));
            }}
            width={110}
            options={[
              { label: "10", value: "10" },
              { label: "20", value: "20" },
              { label: "50", value: "50" },
            ]}
          />
        </div>
      </div>
    </TableCard>
  );
};

export default TradesTable;