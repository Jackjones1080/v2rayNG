# My VPN — implementation scope

This document defines the user-facing scope for My VPN. It deliberately excludes features that require a MahsaNG account, MahsaNG-operated servers, or a separate VPN server owned by the user.

## Keep

- VLESS, VMess, and Trojan configuration support inherited from the v2rayNG/Xray client.
- Import a configuration or subscription from a pasted link.
- Import configurations from QR codes and local files.
- Keep the existing My VPN Cloudflare Worker subscription as the optional built-in subscription source.
- Refresh the Worker subscription every six hours, and also provide a manual refresh action.
- Show connection state, latency/ping checks, and per-profile configuration management.
- Keep the app free of advertisements and avoid sending usage or configuration data to third-party analytics.

## Add only after compatibility review and tests

- DNS over HTTPS: provide a user-selectable resolver setting, with a safe default and a way to restore system DNS.
- TLS fragmentation: expose only if the bundled Xray core supports the exact required transport options; do not add a non-functional toggle.
- Connection/profile health check: test candidates and show measured latency. Do not label a server as the "fastest" based on ping alone.
- Optional automatic profile failover: switch only after a configurable number of connection failures, and provide a clear off switch.

## Exclude

- MahsaNG account/login, paid plans, remote API, and MahsaNG-hosted configuration delivery.
- MahsaNG server lists or any feature that requires a server the user does not own.
- Random-subdomain or fake-host features unless they can be implemented locally and verified against the actual core protocol.
- Ads, third-party telemetry, and automatic sharing of imported configurations.

## Security and privacy

- Treat subscription URLs and imported configurations as secrets. Never print them in logs, commit them to Git, or send them to analytics.
- The built-in Worker URL may be public, but any access key or bearer token must remain outside the public source repository.
- Keep the upstream v2rayNG license and notices intact.
- Do not claim a feature is supported until the app builds and the feature is tested on an Android device.

## Delivery checklist

1. Audit the current fork and identify the existing Worker subscription integration and app entry points.
2. Implement the import and subscription UX without breaking the upstream profile formats.
3. Add DoH only after confirming the Xray core and Android DNS behavior.
4. Evaluate TLS fragmentation support in the exact bundled core; omit the control if unsupported.
5. Add latency-based checks and conservative failover with user controls.
6. Run unit tests and GitHub Actions; inspect the APK artifact.
7. Install and test on the user's Redmi Note 15 4G / Android 16. A successful CI build alone is not proof of device compatibility.
