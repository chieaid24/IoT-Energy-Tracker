"use client";

import { signOut, useSession } from "next-auth/react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { data: session } = useSession();
  const pathname = usePathname();

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border/60 bg-card/80 backdrop-blur-sm">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
          <div className="flex items-center gap-6">
            <span className="text-lg font-semibold tracking-tight">
              <span className="text-primary">IoT</span> Energy Tracker
            </span>
            <nav className="flex gap-4">
              <Link
                href="/dashboard"
                className={`text-sm transition-colors duration-150 ${
                  pathname === "/dashboard"
                    ? "text-foreground font-medium"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                Dashboard
              </Link>
              <Link
                href="/dashboard/devices"
                className={`text-sm transition-colors duration-150 ${
                  pathname === "/dashboard/devices"
                    ? "text-foreground font-medium"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                Devices
              </Link>
              <Link
                href="/dashboard/alerts"
                className={`text-sm transition-colors duration-150 ${
                  pathname === "/dashboard/alerts"
                    ? "text-foreground font-medium"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                Alerts
              </Link>
            </nav>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-muted-foreground">
              {session?.user?.name || session?.user?.email}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => signOut({ callbackUrl: "/login" })}
              className="cursor-pointer"
            >
              Sign out
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl p-4">{children}</main>
      <footer className="border-t border-border/60 bg-card/80 backdrop-blur-sm mt-auto">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
          <p className="text-sm text-muted-foreground">
            &copy; {new Date().getFullYear()} Aidan Chien. All rights reserved.
          </p>
          <div className="flex items-center gap-4">
            <a
              href="https://www.linkedin.com/in/aidanchien/"
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-muted-foreground hover:text-foreground transition-colors duration-150"
            >
              LinkedIn
            </a>
            <a
              href="https://github.com/chieaid24"
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-muted-foreground hover:text-foreground transition-colors duration-150"
            >
              GitHub
            </a>
            <a
              href="https://aidanchien.com"
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-muted-foreground hover:text-foreground transition-colors duration-150"
            >
              Website
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
