"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function InfluxDBPanel() {
  return (
    <Card className="gap-0 overflow-hidden py-0">
      <CardHeader className="py-4">
        <CardTitle>InfluxDB Explorer</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <iframe
          src="http://localhost:8072"
          className="block h-[600px] w-full border-0"
          title="InfluxDB"
          tabIndex={-1}
          loading="lazy"
          sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
        />
      </CardContent>
    </Card>
  );
}
