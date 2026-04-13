"use client";

import { useCallback, useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import { RefreshCw } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface Insight {
  tips: string;
  energyUsage: number;
  confidence: number;
}

export function InsightsPanel({ userId }: { userId: string }) {
  const [insight, setInsight] = useState<Insight | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const storageKey = `ai-insight-${userId}`;

  const fetchInsight = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const res = await fetch(`/api/v1/insight/overview/${userId}?days=7`);
      if (res.ok) {
        const data = await res.json();
        setInsight(data);
        localStorage.setItem(storageKey, JSON.stringify(data));
      } else {
        setError(true);
      }
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [userId, storageKey]);

  useEffect(() => {
    const cached = localStorage.getItem(storageKey);
    if (cached) {
      setInsight(JSON.parse(cached));
      setLoading(false);
    } else {
      fetchInsight();
    }
  }, [storageKey, fetchInsight]);

  const handleRegenerate = () => {
    localStorage.removeItem(storageKey);
    setInsight(null);
    fetchInsight();
  };

  return (
    <Card
      className="animate-card-enter card-accent-border"
      style={{ animationDelay: "150ms" }}
    >
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            AI Insights
            <Button
              variant="ghost"
              size="icon-xs"
              onClick={handleRegenerate}
              disabled={loading}
            >
              <RefreshCw
                className={cn("size-3.5", loading && "animate-spin")}
              />
            </Button>
          </div>
          {loading && !insight ? (
            <Badge variant="secondary" className="bg-muted text-muted-foreground animate-pulse">
              Regenerating...
            </Badge>
          ) : insight ? (
            <Badge variant="secondary" className="bg-primary/10 text-primary">
              {insight.confidence}% confidence
            </Badge>
          ) : null}
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
