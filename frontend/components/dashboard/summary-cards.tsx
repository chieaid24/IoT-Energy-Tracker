"use client";

import { useEffect, useRef, useState } from "react";
import { Cpu, Zap, Bell } from "lucide-react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface SummaryData {
  totalDevices: number;
  totalEnergy: number;
  alertCount: number;
}

const ENERGY_POLL_INTERVAL = 5000;
const ALERT_POLL_INTERVAL = 1000;

export function SummaryCards({ userId }: { userId: string }) {
  const [data, setData] = useState<SummaryData | null>(null);
  const [bellRinging, setBellRinging] = useState(false);
  const prevAlertCount = useRef<number | null>(null);

  useEffect(() => {
    async function fetchEnergyAndDevices() {
      const [usageRes, devicesRes] = await Promise.allSettled([
        fetch(`/api/v1/usage/${userId}?days=7`),
        fetch(`/api/v1/device/user/${userId}`),
      ]);

      let totalEnergy = 0;
      let totalDevices = 0;

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

      setData((prev) => ({ alertCount: prev?.alertCount ?? 0, totalDevices, totalEnergy }));
    }

    async function fetchAlerts() {
      try {
        const res = await fetch(`/api/v1/alert/user/${userId}/count`);
        if (!res.ok) return;
        const alertCount = await res.json();

        if (prevAlertCount.current !== null && alertCount !== prevAlertCount.current) {
          setBellRinging(true);
          setTimeout(() => setBellRinging(false), 800);
        }
        prevAlertCount.current = alertCount;

        setData((prev) => prev ? { ...prev, alertCount } : { totalDevices: 0, totalEnergy: 0, alertCount });
      } catch {
        // silently fail
      }
    }

    fetchEnergyAndDevices();
    fetchAlerts();

    const energyIntervalId = setInterval(fetchEnergyAndDevices, ENERGY_POLL_INTERVAL);
    const alertIntervalId = setInterval(fetchAlerts, ALERT_POLL_INTERVAL);
    return () => {
      clearInterval(energyIntervalId);
      clearInterval(alertIntervalId);
    };
  }, [userId]);

  if (!data) {
    return (
      <div className="grid gap-4 sm:grid-cols-3 stagger-children">
        {[...Array(3)].map((_, i) => (
          <Card key={i} className="animate-card-enter">
            <CardHeader>
              <CardTitle className="text-sm text-muted-foreground">Loading...</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="h-8 w-20 animate-pulse rounded-md bg-muted" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  const cards = [
    { title: "Your Devices", value: data.totalDevices, icon: Cpu },
    {
      title: "Energy (7d)",
      value: `${(data.totalEnergy / 1000).toFixed(2)} kWh`,
      icon: Zap,
    },
    { title: "Alerts", value: data.alertCount, icon: Bell },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-3 stagger-children">
      {cards.map((card) => {
        const Icon = card.icon;
        return (
          <Card
            key={card.title}
            className={cn(
              "animate-card-enter",
              card.title === "Alerts" && data.alertCount > 0
                ? ""
                : ""
            )}
          >
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-sm text-muted-foreground">
                <Icon className={cn("size-4", card.title === "Alerts" && bellRinging && "bell-ring")} />
                {card.title}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold tracking-tight">{card.value}</p>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
