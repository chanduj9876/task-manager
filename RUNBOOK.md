# Task Manager — Start / Stop / Monitor Runbook

## Prerequisites

| Tool | Required version | Notes |
|---|---|---|
| Java | 17+ | Must be on `PATH` |
| Maven | 3.9+ | Must be on `PATH` (no wrapper — use system `mvn`) |
| Node.js | 18+ | Must be on `PATH` |
| Kafka | 3.6+ (KRaft) | Installed at `C:\kafka` |
| PostgreSQL | 17 | Windows service `postgresql-x64-17` |
| Redis | 7+ | Windows service, port 6379 |

---

## Service Overview

```
PostgreSQL  :5432  ──┐
Redis       :6379  ──┤──> Spring Boot backend :8080 ──> React/Vite frontend :3000
Kafka       :9092  ──┘
```

---

## 1. PostgreSQL & Redis (Windows Services — auto-start)

These run as Windows services and start automatically on boot.

### Check status
```powershell
Get-Service postgresql-x64-17, Redis* | Select-Object Name, Status
```

### Start manually if needed
```powershell
Start-Service postgresql-x64-17
Start-Service Redis   # adjust name to match output above
```

### Stop
```powershell
Stop-Service postgresql-x64-17
Stop-Service Redis
```

### Test connectivity
```powershell
# PostgreSQL on 5432
Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet

# Redis on 6379
Test-NetConnection -ComputerName localhost -Port 6379 -InformationLevel Quiet
```

---

## 2. Kafka (Manual — KRaft mode, no Zookeeper)

> **Must be started before the backend.**

### Start (minimized window — recommended)
```powershell
Start-Process "C:\kafka\bin\windows\kafka-server-start.bat" `
    -ArgumentList "C:\kafka\config\kraft\server.properties" `
    -WindowStyle Minimized
```

### Start (foreground — see logs directly)
```powershell
C:\kafka\bin\windows\kafka-server-start.bat C:\kafka\config\kraft\server.properties
```

### Stop (graceful)
```powershell
C:\kafka\bin\windows\kafka-server-stop.bat
```

### Stop (force-kill all Java processes — use only if graceful stop fails)
```powershell
Get-Process | Where-Object { $_.Name -like "*java*" } | Stop-Process -Force
```

> ⚠️ Force-killing Java processes will also kill the backend. Restart the backend afterwards.

### Verify Kafka is up
```powershell
Test-NetConnection -ComputerName localhost -Port 9092 -InformationLevel Quiet
# Expected: True
```

---

## 3. Backend — Spring Boot (port 8080)

### Start
```powershell
cd "c:\Users\cjaripit\OneDrive - Cisco\Documents\project\task-manager-backend"
mvn spring-boot:run
```

To run in the background and capture logs:
```powershell
cd "c:\Users\cjaripit\OneDrive - Cisco\Documents\project\task-manager-backend"
mvn spring-boot:run > backend.log 2>&1 &
```

### Stop
Press **Ctrl+C** in the terminal where the backend is running.

Or kill by port:
```powershell
$pid = (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess
Stop-Process -Id $pid -Force
```

### Verify backend is up
```powershell
# Port check
Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet

# Health endpoint (returns {"status":"UP"} when healthy)
Invoke-RestMethod http://localhost:8080/actuator/health
```

---

## 4. Frontend — Vite dev server (port 3000)

### Start
```powershell
cd "c:\Users\cjaripit\OneDrive - Cisco\Documents\project\task-manager-frontend"
npm run dev
```

### Stop
Press **Ctrl+C** in the terminal where the frontend is running.

### Access
Open **http://localhost:3000/** in a browser.

---

## 5. Full Start Sequence (correct order)

```
1. PostgreSQL & Redis  →  auto-started (verify with Get-Service)
2. Kafka               →  start manually (step 2 above)
3. Backend             →  mvn spring-boot:run  (wait for "Started TaskManagerApplication")
4. Frontend            →  npm run dev          (wait for "VITE ready")
```

---

## 6. Full Stop Sequence (correct order)

```
1. Frontend  →  Ctrl+C
2. Backend   →  Ctrl+C
3. Kafka     →  kafka-server-stop.bat
4. Redis / PostgreSQL  →  leave running (services) or Stop-Service
```

---

## 7. Monitoring & Logs

### Backend log (file)
```powershell
# Tail live log (if started with > backend.log redirect)
Get-Content "c:\Users\cjaripit\OneDrive - Cisco\Documents\project\task-manager-backend\backend.log" -Wait -Tail 50

# Filter for errors only
Get-Content "...\backend.log" | Select-String "ERROR|WARN|Exception"
```

### Backend log (console)
When running with `mvn spring-boot:run` directly, logs stream to the terminal. Look for:
- ✅ `Started TaskManagerApplication in X seconds` — fully up
- ✅ `partitions assigned: [task-events-0, task-events-1, task-events-2]` — Kafka consumer active
- ❌ `Port 8080 was already in use` — free the port first (see section 3)
- ❌ `Broker may not be available` — Kafka is not running (start it first)

### Health endpoints
```powershell
# Overall health (UP / DOWN)
Invoke-RestMethod http://localhost:8080/actuator/health

# All actuator links
Invoke-RestMethod http://localhost:8080/actuator
```

### Kafka topic inspection
```powershell
# List topics
C:\kafka\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --list

# Describe task-events topic (partitions, replicas)
C:\kafka\bin\windows\kafka-topics.bat --bootstrap-server localhost:9092 --describe --topic task-events

# Consume messages from task-events (live tail)
C:\kafka\bin\windows\kafka-console-consumer.bat `
    --bootstrap-server localhost:9092 `
    --topic task-events `
    --from-beginning
```

### Redis inspection
```powershell
# Connect to Redis CLI
C:\path\to\redis-cli.exe

# Inside redis-cli:
PING                        # expect PONG
KEYS blacklist:*            # see blacklisted JWT tokens
TTL blacklist:<token>       # check remaining TTL on a blacklisted token
```

### PostgreSQL inspection
```powershell
# Connect via psql
psql -U postgres -d taskmanager -h localhost

# Inside psql:
\dt                         # list all tables
SELECT count(*) FROM users;
SELECT count(*) FROM tasks;
```

### WebSocket (STOMP)
The backend exposes a STOMP endpoint at `ws://localhost:8080/ws`.
Notifications are pushed to `/user/{userId}/queue/notifications`.
The frontend connects automatically on login via `useWebSocket.ts`.

---

## 8. Port Reference

| Service | Port | Protocol |
|---|---|---|
| PostgreSQL | 5432 | TCP |
| Redis | 6379 | TCP |
| Kafka broker | 9092 | TCP |
| Spring Boot backend | 8080 | HTTP / WebSocket |
| Vite frontend | 3000 | HTTP |
| Spring DevTools LiveReload | 35729 | TCP (dev only) |

---

## 9. Quick Diagnostics Checklist

Run this block to get an instant status snapshot:

```powershell
$services = @{PostgreSQL=5432; Redis=6379; Kafka=9092; Backend=8080; Frontend=3000}
foreach ($s in $services.GetEnumerator()) {
    $r = Test-NetConnection -ComputerName localhost -Port $s.Value -WarningAction SilentlyContinue
    "$($s.Key.PadRight(12)) :$($s.Value)  $(if ($r.TcpTestSucceeded) {'✅ UP'} else {'❌ DOWN'})"
}
Write-Host ""
try {
    $h = Invoke-RestMethod http://localhost:8080/actuator/health -TimeoutSec 3
    "Actuator health : $($h.status)"
} catch {
    "Actuator health : UNREACHABLE"
}
```
