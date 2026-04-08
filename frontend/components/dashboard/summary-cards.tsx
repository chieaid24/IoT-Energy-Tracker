"use client";

import { useEffect, useState } from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

interface SummaryData {
  totalDevices: number;
  totalEnergy: number;
  alertCount: number;
}

export function SummaryCards({ userId }: { userId: string }) {
  const [data, setData] = useState<SummaryData | null>(null);

  useEffect(() => {
    async function fetchData() {
      const [usageRes, alertRes, devicesRes] = await Promise.allSettled([
        fetch(`/api/v1/usage/${userId}?days=7`),
        fetch(`/api/v1/alert/user/${userId}/count`),
        fetch(`/api/v1/device/user/${userId}`),
      ]);

      let totalEnergy = 0;
      let totalDevices = 0;
      let alertCount = 0;

      if (usageRes.status === "fulfilled" && usageRes.value.ok) {
        const usage = await usageRes.value.json();
        totalEnergy = usage.devices?.reduce(
          (sum: number, d: any) => sum + (d.energyConsumed || 0),
          0
        ) ?? 0;
      }

      if (devicesRes.status === "fulfilled" && devicesRes.value.ok) {
        const devices = await devicesRes.value.json();
        totalDevices = devices.length;
      }

      if (alertRes.status === "fulfilled" && alertRes.value.ok) {
        alertCount = await alertRes.value.json();
      }

      setData({ totalDevices, totalEnergy, alertCount });
    }

    fetchData();
  }, [userId]);

  if (!data) {
    return (
      <div className="grid gap-4 sm:grid-cols-3">
        {[...Array(3)].map((_, i) => (
          <Card key={i}>
            <CardHeader>
              <CardTitle className="text-sm text-muted-foreground">Loading...</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-8 w-20 animate-pulse rounded bg-muted" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  const cards = [
    { title: "Your Devices", value: data.totalDevices },
    {
      title: "Energy (7d)",
      value: `${data.totalEnergy.toFixed(2)} kWh`,
    },
    { title: "Alerts", value: data.alertCount },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-3">
      {cards.map((card) => (
        <Card key={card.title}>
          <CardHeader>
            <CardTitle className="text-sm text-muted-foreground">
              {card.title}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">{card.value}</p>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
