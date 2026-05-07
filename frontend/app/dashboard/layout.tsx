"use client";

import { signOut, useSession } from "next-auth/react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/dashboard/devices", label: "Devices" },
  { href: "/dashboard/alerts", label: "Alerts" },
] as const;

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { data: session } = useSession();
  const pathname = usePathname();
  const userLabel = session?.user?.name || session?.user?.email;

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="sticky top-0 z-30 border-b border-border/60 bg-card/80 backdrop-blur-sm">
        <div className="mx-auto max-w-7xl px-4 sm:px-6">
          <div className="flex h-14 items-center justify-between gap-3">
            <div className="flex min-w-0 items-center gap-6">
              <Link
                href="/dashboard"
                className="shrink-0 text-base font-semibold tracking-tight sm:text-lg"
              >
                <span className="text-primary">IoT</span> Energy Tracker
              </Link>
              <nav className="hidden items-center gap-1 sm:flex">
                {NAV_ITEMS.map((item) => {
                  const active = pathname === item.href;
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      aria-current={active ? "page" : undefined}
                      className={cn(
                        "inline-flex min-h-9 items-center rounded-md px-2.5 text-sm transition-colors duration-150",
                        active
                          ? "font-medium text-foreground"
                          : "text-muted-foreground hover:text-foreground"
                      )}
                    >
                      {item.label}
                    </Link>
                  );
                })}
              </nav>
            </div>
            <div className="flex min-w-0 items-center gap-2 sm:gap-4">
              {userLabel && (
                <span className="hidden max-w-[20ch] truncate text-sm text-muted-foreground md:block">
                  {userLabel}
                </span>
              )}
              <Button
                variant="outline"
                size="sm"
                onClick={() => signOut({ callbackUrl: "/login" })}
                className="h-9 cursor-pointer px-3 sm:h-7"
              >
                Sign out
              </Button>
            </div>
          </div>
          <nav
            aria-label="Primary"
            className="-mx-4 flex border-t border-border/40 sm:hidden"
          >
            {NAV_ITEMS.map((item) => {
              const active = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "relative flex min-h-11 flex-1 items-center justify-center px-3 text-sm transition-colors duration-150",
                    active
                      ? "font-medium text-foreground"
                      : "text-muted-foreground hover:text-foreground"
                  )}
                >
                  {item.label}
                  {active && (
                    <span
                      aria-hidden
                      className="absolute inset-x-3 -bottom-px h-0.5 rounded-full bg-primary"
                    />
                  )}
                </Link>
              );
            })}
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-4 sm:px-6 sm:py-6 lg:px-8">
        {children}
      </main>
      <footer className="mt-auto border-t border-border/60 bg-card/80 backdrop-blur-sm">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <p className="text-xs text-muted-foreground sm:text-sm">
            &copy; {new Date().getFullYear()} Aidan Chien. All rights reserved.
          </p>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
            <a
              href="https://www.linkedin.com/in/aidanchien/"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex min-h-9 items-center text-sm text-muted-foreground transition-colors duration-150 hover:text-foreground"
            >
              LinkedIn
            </a>
            <a
              href="https://github.com/chieaid24"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex min-h-9 items-center text-sm text-muted-foreground transition-colors duration-150 hover:text-foreground"
            >
              GitHub
            </a>
            <a
              href="https://aidanchien.com"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex min-h-9 items-center text-sm text-muted-foreground transition-colors duration-150 hover:text-foreground"
            >
              Website
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
