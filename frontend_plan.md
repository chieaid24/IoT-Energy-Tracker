# IoT Energy Tracker — Frontend Implementation Plan

## Agent Instructions

### Before each phase (Phases 1–6):
1. **Stop and ask the user to provide a screenshot of the desired design** for that phase. Do not proceed until a screenshot is received.
2. Analyze the screenshot and describe what you see in the design.
3. **Ask the user if they would like any changes** to the design before you start coding. Wait for explicit confirmation ("looks good", "go ahead", or a list of changes).
4. If changes are requested, acknowledge them, list what will be adjusted, and confirm once more before coding.
5. Only begin writing code after the user has approved the design.

### After each phase:
1. Mark the completed phase with `✅` in its heading
2. Update the **Current State** section to reflect what was actually built (files created, any deviations from the plan, issues encountered)
3. Check off any **Final Checklist** items that are now complete

Do not proceed to the next phase until both the screenshot has been approved AND this file has been updated.

---

## Current State
- Next.js 16, App Router, files live in `frontend/app/` (no `src/`)
- Tailwind v4 + shadcn installed, components in `frontend/components/ui/`
- shadcn components ready: `button`, `card`, `badge`, `input`, `select`, `switch`, `table`
- `lib/utils.ts` exists with `cn()` helper
- All pages are boilerplate — nothing implemented yet

## Design System

### Stack
- **Tailwind v4** for all styling — utility classes only, no custom CSS
- **shadcn/ui** components for all UI elements — do not build custom primitives when a shadcn component exists
- **lucide-react** for all icons (already installed)
- **Inter** font (already configured in layout.tsx)

### Principles
- Every component should do one thing only
- No abstraction until something is used 3+ times
- No loading skeletons, error boundaries, or toast notifications — a plain `<p>Error</p>` is sufficient for MVP
- Use Framer animations when possible. Use expressive and bubbly animations.
- MVP should be in dark mode by default.

### Color Usage
Use shadcn semantic color tokens exclusively:
| Token | Use |
|---|---|
| `bg-background` | Page background |
| `bg-card` | Card surfaces |
| `text-foreground` | Primary text |
| `text-muted-foreground` | Secondary/label text |
| `border` | All borders |
| `bg-primary` / `text-primary-foreground` | Primary actions |
| `bg-destructive` | Delete actions |

### Badge Colors for Status
- Running: `bg-green-100 text-green-800`
- Stopped: `bg-zinc-100 text-zinc-600`
- Confidence ≥85: `bg-green-100 text-green-800`
- Confidence ≥70: `bg-yellow-100 text-yellow-800`
- Confidence <70: `bg-red-100 text-red-700`

### Layout
- Max content width: `max-w-6xl mx-auto px-6`
- Page top padding: `py-8`
- Section gap: `space-y-6`
- Card padding: use shadcn `CardContent` defaults

---

## Phase 0 — Foundation [ ]
*No UI. Set up the data and routing skeleton everything else depends on.*

### Step 1: Create types

**File:** `frontend/types/index.ts` (create new file)

```typescript
export interface UserDto {
  id: number
  name: string
  surname: string
  email: string
  address: string
  alerting: boolean
  energyAlertingThreshold: number
}

export interface DeviceDto {
  id: number
  name: string
  type: string
  location: string
  userId: number
  energyConsumed?: number
}

export interface UsageDto {
  userId: number
  devices: DeviceDto[]
}

export interface InsightDto {
  userId: number
  tips: string
  energyUsage: number
  confidence: number
}

export interface SimulationStatus {
  running: boolean
}
```

### Step 2: Create API client

**File:** `frontend/lib/api.ts` (create new file)

One function per backend endpoint. All functions use `fetch()` with `cache: "no-store"`. Base URL from env var with localhost fallback.

Endpoints to implement:
```
const BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost/api/v1"
```

| Function | Method | Endpoint |
|---|---|---|
| `getSimulationStatus()` | GET | `/ingestion/simulation/status` |
| `startSimulation()` | POST | `/ingestion/simulation/start` |
| `stopSimulation()` | POST | `/ingestion/simulation/stop` |
| `getTotalUsers()` | GET | `/user/total` |
| `getTotalDevices()` | GET | `/device/total` |
| `getUserIds()` | GET | `/user/ids` |
| `getUser(id)` | GET | `/user/{id}` |
| `updateUser(id, data)` | PUT | `/user/{id}` |
| `deleteUser(id)` | DELETE | `/user/{id}` |
| `deleteAllUsers()` | DELETE | `/user/all` |
| `seedUsers(n)` | POST | `/user/create/dummy?users={n}` |
| `getDevicesByUser(userId)` | GET | `/device/user/{userId}` |
| `deleteDevice(id)` | DELETE | `/device/{id}` |
| `deleteAllDevices()` | DELETE | `/device/all` |
| `seedDevices(n)` | POST | `/device/create/dummy?devices={n}` |
| `getUsage(userId, days)` | GET | `/usage/{userId}?days={days}` |
| `getInsight(userId, days)` | GET | `/insight/overview/{userId}?days={days}` |

### Step 3: Create `.env.local`

**File:** `frontend/.env.local`
```
NEXT_PUBLIC_API_BASE=http://localhost/api/v1
```

### Step 4: Update root layout with nav

**File:** `frontend/app/layout.tsx`

Replace boilerplate with:
- Nav bar: app title left, links right (`Dashboard`, `Users`, `Devices`, `Usage`, `Insights`)
- `SimulationBar` below nav (import from `@/components/simulation-bar` — will be created in Phase 1)
- `<main className="max-w-6xl mx-auto px-6 py-8">{children}</main>`

**No screenshot needed for Phase 0 — it is purely structural.**

> ### ✏️ After completing Phase 0: update the Current State section and mark this phase `✅` before continuing.

---

## Phase 1 — Simulation Bar [ ]

> ### ⏸ STOP — Design approval required before coding
> 1. Ask the user: *"Please share a screenshot of your desired SimulationBar design before I begin."*
> 2. Wait for the screenshot. Do not write any code yet.
> 3. Describe what you see in the screenshot.
> 4. Ask: *"Would you like any changes to this design before I start coding?"*
> 5. Wait for the user to confirm or request changes. Only proceed once they explicitly approve.

**File:** `frontend/components/simulation-bar.tsx`

**Requirements:**
- `"use client"` — needs polling + button handlers
- On mount, call `getSimulationStatus()` and set state
- Poll every 5 seconds with `setInterval` in `useEffect` (clear on unmount)
- Show a `Badge` with "Running" (green) or "Stopped" (gray)
- Show a "Start" button (disabled when running) — calls `startSimulation()` then refreshes status
- Show a "Stop" button (disabled when stopped) — calls `stopSimulation()` then refreshes status
- Not sticky — lives inside the left sidebar nav, renders as a normal block element
- Layout: vertical stack (label, badge, then buttons below) to fit a sidebar column

**Props:** none — self-contained

**State:** `{ running: boolean, loading: boolean }`

> ### ✏️ After completing Phase 1: update the Current State section and mark this phase `✅` before continuing.

---

## Phase 2 — Dashboard Page [ ]

> ### ⏸ STOP — Design approval required before coding
> 1. Ask the user: *"Please share a screenshot of your desired Dashboard design before I begin."*
> 2. Wait for the screenshot. Do not write any code yet.
> 3. Describe what you see in the screenshot.
> 4. Ask: *"Would you like any changes to this design before I start coding?"*
> 5. Wait for the user to confirm or request changes. Only proceed once they explicitly approve.

**File:** `frontend/app/page.tsx`

**Requirements:**
- Server Component — fetch totals at render time (no `"use client"`)
- Fetch in parallel: `getTotalUsers()`, `getTotalDevices()`
- Display 2 stat cards: "Total Users" and "Total Devices"
- Action buttons row (these need client-side handlers — extract to a small `<DashboardActions />` client component):
  - "Seed 5 Users" → `seedUsers(5)`
  - "Seed 20 Devices" → `seedDevices(20)`
  - "Delete All Users" → `deleteAllUsers()`
  - "Delete All Devices" → `deleteAllDevices()`
- After any action, `router.refresh()` to re-fetch server data

**Layout:** page title → 2 stat cards in a row → action buttons row

**Components used:** `Card`, `CardContent`, `CardHeader`, `CardTitle`, `Button`

> ### ✏️ After completing Phase 2: update the Current State section and mark this phase `✅` before continuing.

---

## Phase 3 — Users Page [ ]

> ### ⏸ STOP — Design approval required before coding
> 1. Ask the user: *"Please share a screenshot of your desired Users page design before I begin."*
> 2. Wait for the screenshot. Do not write any code yet.
> 3. Describe what you see in the screenshot.
> 4. Ask: *"Would you like any changes to this design before I start coding?"*
> 5. Wait for the user to confirm or request changes. Only proceed once they explicitly approve.

**File:** `frontend/app/users/page.tsx`

**Requirements:**
- Server Component shell — fetch user IDs + all user data at render time
- Fetch: `getUserIds()` → map to `getUser(id)` for each (parallel with `Promise.all`)
- Render a `<UsersTable users={users} />` client component (handles edit/delete interactions)
- Seed input above table: number input (default 5) + "Seed Users" button → `seedUsers(n)`

**File:** `frontend/components/users-table.tsx` (`"use client"`)

- Display columns: Name, Email, Alerting (Switch), Threshold (editable Input), Actions (Delete button)
- Alerting toggle: `Switch` → on change call `updateUser(id, { ...user, alerting: !user.alerting })`
- Threshold edit: `Input` (type number) — on blur, call `updateUser(id, { ...user, energyAlertingThreshold: value })`
- Delete: `Button` variant destructive → `deleteUser(id)` → remove from local state
- After seed: `router.refresh()`

**Components used:** `Table`, `TableHeader`, `TableBody`, `TableRow`, `TableHead`, `TableCell`, `Switch`, `Input`, `Button`, `Badge`

> ### ✏️ After completing Phase 3: update the Current State section and mark this phase `✅` before continuing.

---

## Phase 4 — Devices Page [ ]

> ### ⏸ STOP — Design approval required before coding
> 1. Ask the user: *"Please share a screenshot of your desired Devices page design before I begin."*
> 2. Wait for the screenshot. Do not write any code yet.
> 3. Describe what you see in the screenshot.
> 4. Ask: *"Would you like any changes to this design before I start coding?"*
> 5. Wait for the user to confirm or request changes. Only proceed once they explicitly approve.

**File:** `frontend/app/devices/page.tsx`

**Requirements:**
- Server Component — fetch all user IDs, then for each user fetch their devices via `getDevicesByUser(userId)`, flatten into one list
- Render `<DevicesTable devices={devices} />` client component
- Seed input above table: number input (default 20) + "Seed Devices" button → `seedDevices(n)`

**File:** `frontend/components/devices-table.tsx` (`"use client"`)

- Display columns: Name, Type (Badge), Location, Owner (userId), Actions (Delete)
- Delete: `deleteDevice(id)` → remove from local state
- After seed: `router.refresh()`

**Components used:** `Table`, `Badge`, `Button`

> ### ✏️ After completing Phase 4: update the Current State section and mark this phase `✅` before continuing.

---

## Phase 5 — Usage Page [ ]

> ### ⏸ STOP — Design approval required before coding
> 1. Ask the user: *"Please share a screenshot of your desired Usage page design before I begin."*
> 2. Wait for the screenshot. Do not write any code yet.
> 3. Describe what you see in the screenshot.
> 4. Ask: *"Would you like any changes to this design before I start coding?"*
> 5. Wait for the user to confirm or request changes. Only proceed once they explicitly approve.

**File:** `frontend/app/usage/page.tsx`

**Requirements:**
- `"use client"` — user selection and days are interactive state
- On load, fetch user IDs with `getUserIds()` for the dropdown
- User selector: shadcn `Select` populated with user IDs
- Days selector: shadcn `Select` with options 1, 3, 7, 30 (default: 3)
- When both are selected, fetch `getUsage(userId, days)` and display results
- Display table: Device Name, Type, Location, Energy (kWh) — sorted descending by energy
- Total kWh row at the bottom of the table (`font-semibold`)
- No auto-refresh for MVP

**State:** `{ userIds, selectedUser, days, usage, loading }`

**Components used:** `Select`, `Table`, `Card`

> ### ✏️ After completing Phase 5: update the Current State section and mark this phase `✅` before continuing.

---

## Phase 6 — Insights Page [ ]

> ### ⏸ STOP — Design approval required before coding
> 1. Ask the user: *"Please share a screenshot of your desired Insights page design before I begin."*
> 2. Wait for the screenshot. Do not write any code yet.
> 3. Describe what you see in the screenshot.
> 4. Ask: *"Would you like any changes to this design before I start coding?"*
> 5. Wait for the user to confirm or request changes. Only proceed once they explicitly approve.

**File:** `frontend/app/insights/page.tsx`

**Requirements:**
- `"use client"` — user selection is interactive
- On load, fetch user IDs for dropdown
- User selector: shadcn `Select`
- Days selector: shadcn `Select` with options 1, 3, 7, 30 (default: 3)
- "Get Insights" button → calls `getInsight(userId, days)` (can be slow — show "Loading..." on button while pending)
- "Regenerate" button (same as Get Insights — re-calls the same endpoint)
- Display:
  - Stat row: "Total Usage: X kWh" and confidence `Badge` (color-coded per design system above)
  - Markdown block: render `tips` string — install `react-markdown` (`npm install react-markdown`) and use `<ReactMarkdown>{insight.tips}</ReactMarkdown>`

**State:** `{ userIds, selectedUser, days, insight, loading }`

**Components used:** `Select`, `Button`, `Badge`, `Card`, `CardContent`
**Extra dependency:** `react-markdown`

> ### ✏️ After completing Phase 6: update the Current State section, mark this phase `✅`, and complete the Final Checklist.

---

## Final Checklist

Before considering MVP complete:
- [ ] SimulationBar shows correct status on every page
- [ ] Dashboard stat cards display live counts
- [ ] Seed buttons produce visible results (refresh updates the count)
- [ ] Users table shows all users with working alerting toggle and threshold edit
- [ ] Devices table shows all devices grouped across users
- [ ] Usage page returns device-level energy breakdown
- [ ] Insights page renders AI markdown tips with confidence badge
- [ ] `.env.local` has `NEXT_PUBLIC_API_BASE` set correctly
- [ ] `npm run dev` has no TypeScript errors
