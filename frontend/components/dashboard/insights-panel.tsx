"use client";

import { useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface Insight {
  tips: string;
  energyUsage: number;
  confidence: number;
}

export function InsightsPanel({ userId }: { userId: string }) {
  const [insight, setInsight] = useState<Insight | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    async function fetchData() {
      try {
        const res = await fetch(`/api/v1/insight/overview/${userId}?days=7`);
        if (res.ok) {
          setInsight(await res.json());
        } else {
          setError(true);
        }
      } catch {
        setError(true);
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, [userId]);

  return (
    <Card
      className="animate-card-enter card-accent-border"
      style={{ animationDelay: "150ms" }}
    >
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          AI Insights
          {insight && (
            <Badge variant="secondary" className="bg-primary/10 text-primary">
              {insight.confidence}% confidence
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? (
          <p className="text-muted-foreground">Generating insights...</p>
        ) : error ? (
          <p className="text-muted-foreground">
            AI insights unavailable — Ollama may be offline
          </p>
        ) : insight ? (
          <div className="prose prose-sm dark:prose-invert max-w-none">
            <ReactMarkdown>{insight.tips}</ReactMarkdown>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
