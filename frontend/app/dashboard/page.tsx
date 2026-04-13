"use client";

import { useSession } from "next-auth/react";
import { redirect } from "next/navigation";
import { SummaryCards } from "@/components/dashboard/summary-cards";
import { EnergyChart } from "@/components/dashboard/energy-chart";
import { DeviceTable } from "@/components/dashboard/device-table";
import { InsightsPanel } from "@/components/dashboard/insights-panel";


export default function DashboardPage() {
  const { data: session, status } = useSession();
  // 
  if (status === "loading") {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <p className="text-muted-foreground">Loading...</p>
      </div>
    );
  }

  if (status === "unauthenticated") {
    redirect("/login");
  }

  const userId = (session as any)?.userId || "1";

  return (
    <div className="space-y-6">
      <h1 className="animate-fade-up text-2xl font-bold tracking-tight">Dashboard</h1>
      <SummaryCards userId={userId} />
      <div className="grid gap-6 lg:grid-cols-2">
        <EnergyChart userId={userId} />
        <InsightsPanel userId={userId} />
      </div>
      <DeviceTable userId={userId} />
    </div>
  );
}
