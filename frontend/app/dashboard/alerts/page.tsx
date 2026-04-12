"use client";

import { Card, CardContent } from "@/components/ui/card";

export default function AlertsPage() {
  return (
    <Card className="gap-0 overflow-hidden py-0">
      <CardContent className="p-0">
        <iframe
          src="http://localhost:8025"
          className="block h-[1000px] w-full border-0"
          title="Mailpit"
          tabIndex={-1}
          loading="lazy"
          sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
        />
      </CardContent>
    </Card>
  );
}
