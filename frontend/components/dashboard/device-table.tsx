"use client";

import { useEffect, useMemo, useState } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowUp, ArrowDown, ArrowUpDown } from "lucide-react";

interface Device {
  id: number;
  name: string;
  type: string;
  location: string;
  energyConsumed: number;
}

type SortKey = "name" | "type" | "location" | "energyConsumed";
type SortDirection = "asc" | "desc";

const POLL_INTERVAL = 5000;

export function DeviceTable({ userId }: { userId: string }) {
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [sortKey, setSortKey] = useState<SortKey>("name");
  const [sortDirection, setSortDirection] = useState<SortDirection>("asc");

  function handleSort(key: SortKey) {
    if (sortKey !== key) {
      setSortKey(key);
      setSortDirection("asc");
    } else {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    }
  }

  const sortedDevices = useMemo(() => {
    if (!sortKey) return devices;
    return [...devices].sort((a, b) => {
      const aVal = a[sortKey];
      const bVal = b[sortKey];
      if (typeof aVal === "string" && typeof bVal === "string") {
        return sortDirection === "asc"
          ? aVal.localeCompare(bVal)
          : bVal.localeCompare(aVal);
      }
      return sortDirection === "asc"
        ? (aVal as number) - (bVal as number)
        : (bVal as number) - (aVal as number);
    });
  }, [devices, sortKey, sortDirection]);

  function SortIcon({ column }: { column: SortKey }) {
    const icon =
      sortKey !== column ? (
        <ArrowUpDown className="size-3.5 text-muted-foreground/50" />
      ) : sortDirection === "asc" ? (
        <ArrowUp className="size-3.5" />
      ) : (
        <ArrowDown className="size-3.5" />
      );
    return <span className="ml-1 inline-flex w-3.5 shrink-0 justify-center">{icon}</span>;
  }

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

  return (
    <Card className="animate-card-enter" style={{ animationDelay: "200ms" }}>
      <CardHeader>
        <CardTitle>Devices</CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? (
          <p className="text-muted-foreground">Loading...</p>
        ) : devices.length === 0 ? (
          <p className="text-muted-foreground">No devices found</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Status</TableHead>
                <TableHead className="cursor-pointer select-none" onClick={() => handleSort("name")}>
                  <span className="inline-flex items-center">Name <SortIcon column="name" /></span>
                </TableHead>
                <TableHead className="cursor-pointer select-none" onClick={() => handleSort("type")}>
                  <span className="inline-flex items-center">Type <SortIcon column="type" /></span>
                </TableHead>
                <TableHead className="cursor-pointer select-none" onClick={() => handleSort("location")}>
                  <span className="inline-flex items-center">Location <SortIcon column="location" /></span>
                </TableHead>
                <TableHead className="cursor-pointer select-none text-right" onClick={() => handleSort("energyConsumed")}>
                  <span className="inline-flex items-center">Energy (kWh) <SortIcon column="energyConsumed" /></span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody className="stagger-children">
              {sortedDevices.map((device) => (
                <TableRow key={device.id} className="animate-fade-up">
                  <TableCell>
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-green-500/10 px-2 py-0.5 text-xs font-medium text-green-600">
                      <span className="status-dot size-1.5 rounded-full bg-green-500" />
                      Online
                    </span>
                  </TableCell>
                  <TableCell className="font-medium">{device.name}</TableCell>
                  <TableCell>
                    <Badge variant="secondary">{device.type}</Badge>
                  </TableCell>
                  <TableCell>{device.location}</TableCell>
                  <TableCell className="text-right">
                    {((device.energyConsumed || 0) / 1000).toFixed(2)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
