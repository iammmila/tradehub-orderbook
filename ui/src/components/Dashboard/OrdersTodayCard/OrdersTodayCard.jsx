import React, { useEffect, useState } from "react";
import { fetchOrders } from "../../../api/orders";
import { isSameLocalDay } from "../../../utils/helpers";
import Cards from "../Cards/Cards";

 const OrdersTodayCard = () => {
   const [loading, setLoading] = useState(true);
   const [error, setError] = useState(null);
   const [ordersToday, setOrdersToday] = useState(0);
   const [instrumentsToday, setInstrumentsToday] = useState(0);

   useEffect(() => {
     let alive = true;

     (async () => {
       try {
         setLoading(true);
         const orders = await fetchOrders(0, 200);
         if (!alive) return;

         const today = new Date();
         const todays = (orders || []).filter((o) => isSameLocalDay(o.createdAt, today));

         setOrdersToday(todays.length);
         setInstrumentsToday(new Set(todays.map((o) => o.instrument)).size);
         setError(null);
       } catch (e) {
         if (!alive) return;
         setError(e?.message || "error");
       } finally {
         if (alive) setLoading(false);
       }
     })();

     return () => { alive = false; };
   }, []);

   return (
     <Cards
       title="Orders Today"
       value={ordersToday}
       subtitle={`${instrumentsToday} instruments`}
       loading={loading}
       error={error}
     />
   );
 }
export default OrdersTodayCard;