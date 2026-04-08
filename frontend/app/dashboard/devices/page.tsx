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
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Devices</h1>
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
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Location</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {devices.map((device) => (
                  <TableRow key={device.id}>
                    <TableCell>{device.id}</TableCell>
                    <TableCell className="font-medium">{device.name}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{device.type}</Badge>
                    </TableCell>
                    <TableCell>{device.location}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
