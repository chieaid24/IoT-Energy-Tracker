"use client";

import { useEffect, useState } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface DeviceUsage {
  id: number;
  name: string;
  type: string;
  energyConsumed: number;
}

const CHART_COLORS = [
  "var(--chart-1)",
  "var(--chart-2)",
  "var(--chart-3)",
  "var(--chart-4)",
  "var(--chart-5)",
];

const POLL_INTERVAL = 5000;

export function EnergyChart({ userId }: { userId: string }) {
  const [devices, setDevices] = useState<DeviceUsage[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchData() {
      try {
        const res = await fetch(`/api/v1/usage/${userId}?days=7`);
        if (res.ok) {
          const data = await res.json();
          setDevices(data.devices || []);
        }
      } catch {
        // silently fail
      } finally {
        setLoading(false);
      }
    }

    fetchData();
    const intervalId = setInterval(fetchData, POLL_INTERVAL);
    return () => clearInterval(intervalId);
  }, [userId]);

  const chartData = devices.map((d, i) => ({
    name: d.name || `Device ${d.id}`,
    energy: Number(((d.energyConsumed || 0) / 1000).toFixed(2)),
    fill: CHART_COLORS[i % CHART_COLORS.length],
  }));

  return (
    <Card className="animate-card-enter" style={{ animationDelay: "100ms" }}>
      <CardHeader>
        <CardTitle>Energy Consumption by Device (7 days)</CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex h-64 items-center justify-center">
            <p className="text-muted-foreground">Loading...</p>
          </div>
        ) : chartData.length === 0 ? (
          <div className="flex h-64 items-center justify-center">
            <p className="text-muted-foreground">
              No energy data available
            </p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" vertical={false} />
              <XAxis
                dataKey="name"
                className="text-xs"
                tick={{ fill: "var(--muted-foreground)" }}
              />
              <YAxis
                className="text-xs"
                tick={{ fill: "var(--muted-foreground)" }}
                label={{
                  value: "kWh",
                  angle: -90,
                  position: "insideLeft",
                  fill: "var(--muted-foreground)",
                }}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: "var(--card)",
                  border: "1px solid var(--border)",
                  borderRadius: "10px",
                  boxShadow: "0 4px 12px oklch(0.3 0.05 55 / 8%)",
                  fontSize: "0.8125rem",
                }}
                cursor={{ fill: "var(--muted)", opacity: 0.4 }}
              />
              <Bar dataKey="energy" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  );
}
