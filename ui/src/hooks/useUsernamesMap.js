import { useCallback, useEffect, useRef, useState } from "react";
import { fetchUserById } from "../api/users";

export function useUsernamesMap(trades) {
  const cacheRef = useRef({}); // no rerender on every cache update
  const [usernames, setUsernames] = useState({});

  const resolve = useCallback(async (list) => {
    const ids = new Set();
    (list || []).forEach((t) => {
      if (t?.buyerUserId != null) ids.add(t.buyerUserId);
      if (t?.sellerUserId != null) ids.add(t.sellerUserId);
    });

    const missing = [...ids].filter((id) => cacheRef.current[id] == null);
    if (missing.length === 0) return;

    const updates = {};
    await Promise.all(
      missing.map(async (id) => {
        try {
          const u = await fetchUserById(id);
          updates[id] = u?.username || `User ${id}`;
        } catch {
          updates[id] = `User ${id}`;
        }
      }),
    );

    cacheRef.current = { ...cacheRef.current, ...updates };
    setUsernames(cacheRef.current);
  }, []);

  useEffect(() => {
    resolve(trades);
  }, [trades, resolve]);

  return usernames;
}
