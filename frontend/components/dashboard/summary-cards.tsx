"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Bell } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
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
  const router = useRouter();

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
            <CardContent className="pt-6">
              <div className="h-3 w-28 animate-pulse rounded bg-muted" />
              <div className="mt-3 h-9 w-16 animate-pulse rounded bg-muted" />
              <div className="mt-2 h-3 w-20 animate-pulse rounded bg-muted" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  const energyKwh = (data.totalEnergy / 1000).toFixed(2);
  const hasAlerts = data.alertCount > 0;

  return (
    <div className="grid gap-4 sm:grid-cols-3 stagger-children">

      {/* Devices — quiet, architectural */}
      <Card className="animate-card-enter">
        <CardContent className="pt-6">
          <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground">
            Connected Devices
          </p>
          <p className="mt-2 text-4xl font-bold tabular-nums tracking-tight">
            {data.totalDevices}
          </p>
          <p className="mt-1.5 text-xs text-muted-foreground">in your home</p>
        </CardContent>
      </Card>

      {/* Energy — number + unit split, data-forward */}
      <Card className="animate-card-enter" style={{ animationDelay: "50ms" }}>
        <CardContent className="pt-6">
          <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground">
            Energy Used
          </p>
          <div className="mt-2 flex items-baseline gap-1.5">
            <p className="text-4xl font-bold tabular-nums tracking-tight">{energyKwh}</p>
            <span className="text-sm font-medium text-muted-foreground">kWh</span>
          </div>
          <p className="mt-1.5 text-xs text-muted-foreground">past 7 days</p>
        </CardContent>
      </Card>

      {/* Alerts — state-driven, navigable */}
      <Card
        role="button"
        tabIndex={0}
        aria-label={
          hasAlerts
            ? `${data.alertCount} active ${data.alertCount === 1 ? "alert" : "alerts"}. Go to alerts.`
            : "No active alerts. Go to alerts."
        }
        className={cn(
          "animate-card-enter cursor-pointer transition-colors hover:bg-muted/50",
          hasAlerts && "border-destructive/25"
        )}
        style={{ animationDelay: "100ms" }}
        onClick={() => router.push("/dashboard/alerts")}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            router.push("/dashboard/alerts");
          }
        }}
      >
        <CardContent className="pt-6">
          <div className="flex items-center justify-between">
            <p className={cn(
              "text-xs font-medium uppercase tracking-widest",
              hasAlerts ? "text-destructive/70" : "text-muted-foreground"
            )}>
              Alerts
            </p>
            <Bell className={cn(
              "size-3.5",
              bellRinging && "bell-ring",
              hasAlerts ? "text-destructive" : "text-muted-foreground"
            )} />
          </div>
          {hasAlerts ? (
            <>
              <p className="mt-2 text-4xl font-bold tabular-nums tracking-tight text-destructive">
                {data.alertCount}
              </p>
              <p className="mt-1.5 text-xs text-muted-foreground">
                active {data.alertCount === 1 ? "alert" : "alerts"}
              </p>
            </>
          ) : (
            <>
              <p className="mt-2 text-2xl font-medium text-muted-foreground">All clear</p>
              <p className="mt-1.5 text-xs text-muted-foreground">no active alerts</p>
            </>
          )}
        </CardContent>
      </Card>

    </div>
  );
}
