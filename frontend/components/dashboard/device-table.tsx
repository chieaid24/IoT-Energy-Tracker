"use client";

import { useEffect, useState } from "react";
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

interface Device {
  id: number;
  name: string;
  type: string;
  location: string;
  energyConsumed: number;
}

const POLL_INTERVAL = 5000;

export function DeviceTable({ userId }: { userId: string }) {
  const [devices, setDevices] = useState<Device[]>([]);
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
                <TableHead>Name</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Location</TableHead>
                <TableHead className="text-right">Energy (kWh)</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody className="stagger-children">
              {devices.map((device) => (
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
