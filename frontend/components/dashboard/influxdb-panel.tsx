"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function InfluxDBPanel() {
  return (
    <Card className="animate-card-enter">
      <CardHeader>
        <CardTitle>InfluxDB Explorer</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <iframe
          src="http://localhost:8072"
          className="h-[600px] w-full rounded-b-xl border-0"
          title="InfluxDB"
          tabIndex={-1}
        />
      </CardContent>
    </Card>
  );
}
