# Network Mode Switching Guide
For **obs‑streaming (Ubuntu)** and **owlcms (Windows)**

Two supported modes:

- **Normal Mode** — LAN1 has Internet  
- **Fallback Mode** — LAN1 has *no* Internet → use venue Wi‑Fi  

Only **obs‑streaming** and **owlcms** need switching.  
All other machines stay wired‑only.

---

## 1. Overview

### Normal Mode
- LAN1 provides Internet  
- Ethernet carries LAN1 + Internet  
- Wi‑Fi is unused  

### Fallback Mode
- LAN1 has **no** Internet  
- Ethernet carries LAN1 only  
- Wi‑Fi provides Internet  
- Routing stays clean and predictable  

---

# 2. obs‑streaming (Ubuntu)
Uses **two Netplan profiles** and a **symlink switch**.

Directory layout:

```
/etc/netplan/01-normal.yaml
/etc/netplan/01-fallback.yaml
/etc/netplan/config.yaml   ← symlink to one of the above
```

---

## 2.1 Normal Mode (LAN1 has Internet)

`/etc/netplan/01-normal.yaml`:

```yaml
network:
  version: 2
  renderer: NetworkManager

  ethernets:
    eth0:
      dhcp4: no
      addresses:
        - 192.168.1.20/24
      gateway4: 192.168.1.1
      nameservers:
        addresses: [192.168.1.1]

  wifis:
    wlan0:
      dhcp4: no
```

### Activate Normal Mode

```
sudo ln -sf /etc/netplan/01-normal.yaml /etc/netplan/config.yaml
sudo netplan apply
```

---

## 2.2 Fallback Mode (LAN1 has NO Internet → use Wi‑Fi)

`/etc/netplan/01-fallback.yaml`:

```yaml
network:
  version: 2
  renderer: NetworkManager

  ethernets:
    eth0:
      dhcp4: no
      addresses:
        - 192.168.1.20/24
      gateway4: null
      nameservers:
        addresses: []

  wifis:
    wlan0:
      dhcp4: yes
      access-points:
        "VenueWiFi":
          password: "yourpassword"
```

### Activate Fallback Mode

```
sudo ln -sf /etc/netplan/01-fallback.yaml /etc/netplan/config.yaml
sudo netplan apply
```

---

# 3. owlcms (Windows)

Windows uses **two PowerShell scripts** to switch Ethernet behavior.

Assume the Ethernet interface is named `"Ethernet"`.

---

## 3.1 normal.ps1  
LAN1 has Internet → Ethernet gets gateway

```powershell
Set-NetIPAddress -InterfaceAlias "Ethernet" -IPAddress 192.168.1.100 -PrefixLength 24 -DefaultGateway 192.168.1.1
Set-DnsClientServerAddress -InterfaceAlias "Ethernet" -ServerAddresses 192.168.1.1
```

Run in **PowerShell as Administrator**.

---

## 3.2 fallback.ps1  
LAN1 has NO Internet → remove gateway → Wi‑Fi becomes default route

```powershell
Set-NetIPAddress -InterfaceAlias "Ethernet" -IPAddress 192.168.1.100 -PrefixLength 24 -DefaultGateway $null
Set-DnsClientServerAddress -InterfaceAlias "Ethernet" -ServerAddresses @()
```

Windows routing becomes:

- `192.168.1.x` → Ethernet  
- Everything else → Wi‑Fi  

---

# 4. How to Identify Your Ethernet Interface Name (Windows)

You must know the correct interface name for the scripts above.  
Here are the reliable ways to find it.

---

## 4.1 PowerShell (recommended)

Run:

```powershell
Get-NetAdapter
```

Example output:

```
Name                      InterfaceDescription               Status
----                      --------------------               ------
Ethernet                  Intel(R) I219-LM                    Up
Wi-Fi                     Intel(R) Dual Band Wireless-AC      Up
```

Use the **Name** column (e.g., `Ethernet`, `Ethernet 2`, `USB Ethernet`).

---

## 4.2 Detailed view

```powershell
Get-NetIPConfiguration
```

Look for:

```
InterfaceAlias : Ethernet
```

That’s the name you use in the scripts.

---

## 4.3 GUI method

1. Open **Control Panel**  
2. Go to **Network and Internet → Network and Sharing Center**  
3. Click **Change adapter settings**  
4. The interface names shown here match PowerShell’s names  

---

# 5. Summary Table

| Machine | Normal Mode | Fallback Mode |
|--------|-------------|----------------|
| **obs‑streaming (Ubuntu)** | Ethernet = LAN1 + Internet | Ethernet = LAN1 only, Wi‑Fi = Internet |
| **owlcms (Windows)** | Ethernet = LAN1 + Internet | Ethernet = LAN1 only, Wi‑Fi = Internet |
| **obs‑ledwall** | Wired only | Wired only |
| **owlcms‑replays** | Wired only | Wired only |
| **AI slurpers (LAN2)** | Optional | Optional |

---

# 6. Notes on Multicast vs Unicast

### Unicast Mode (simple)
- Everything on LAN1  
- No routing  
- No NAT  
- No port‑forwarding  
- No multicast  
- Best for small events  

### Multicast Mode (LAN2)
- LAN2 isolates multicast  
- Supports many slurpers / AI nodes  
- Requires LAN2 router  
- Requires port‑forwarding for owlcms → OBS  
- Best for advanced video workflows  

Both modes support the **fallback Wi‑Fi Internet** behavior.