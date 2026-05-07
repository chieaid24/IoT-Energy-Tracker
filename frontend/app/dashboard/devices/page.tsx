"use client";

import { useSession } from "next-auth/react";
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
import { InfluxDBPanel } from "@/components/dashboard/influxdb-panel";

interface Device {
  id: number;
  name: string;
  type: string;
  location: string;
  userId: number;
}

export default function DevicesPage() {
  const { data: session } = useSession();
  const userId = (session as any)?.userId || "1";
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchDevices() {
      try {
        const res = await fetch(`/api/v1/device/user/${userId}`);
        if (res.ok) {
          setDevices(await res.json());
        }
      } catch {
        // silently fail
      } finally {
        setLoading(false);
      }
    }

    fetchDevices();
  }, [userId]);

  return (
    <div className="space-y-4 sm:space-y-6">
      <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Devices</h1>
      <InfluxDBPanel />
      <Card>
        <CardHeader>
          <CardTitle>All Devices</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="text-muted-foreground">Loading...</p>
          ) : devices.length === 0 ? (
            <p className="text-muted-foreground">No devices registered</p>
          ) : (
            <div className="-mx-2 overflow-x-auto sm:mx-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="hidden sm:table-cell">ID</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead className="hidden md:table-cell">Location</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {devices.map((device) => (
                    <TableRow key={device.id}>
                      <TableCell className="hidden tabular-nums sm:table-cell">
                        {device.id}
                      </TableCell>
                      <TableCell className="font-medium">
                        {device.name}
                        <span className="mt-0.5 block text-xs text-muted-foreground md:hidden">
                          {device.location}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge variant="secondary">{device.type}</Badge>
                      </TableCell>
                      <TableCell className="hidden md:table-cell">{device.location}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
