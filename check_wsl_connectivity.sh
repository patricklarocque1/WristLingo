#!/bin/bash
echo "=== WSL Connectivity Check ==="
echo "WSL Version: $(wsl --version 2>/dev/null || echo 'WSL 1')"
echo "Hostname: $(hostname)"
echo "IP Address: $(hostname -I)"
echo "Internet connectivity:"
ping -c 3 google.com > /dev/null 2>&1 && echo "✓ Internet accessible" || echo "✗ No internet"
echo "GitHub connectivity:"
ping -c 3 github.com > /dev/null 2>&1 && echo "✓ GitHub accessible" || echo "✗ GitHub not accessible"
echo "Windows host access:"
ping -c 3 $(cat /etc/resolv.conf | grep nameserver | awk '{print $2}') > /dev/null 2>&1 && echo "✓ Windows host accessible" || echo "✗ Windows host not accessible"
echo "=============================="
EOF