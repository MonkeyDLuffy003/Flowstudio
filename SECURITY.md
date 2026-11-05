# Security model

- Android Keystore AES key, randomized GCM IV per save; no plaintext fallback.
- Device credential confirmation to open the vault. An unlocked app may use a saved key for requested generation without another prompt. The vault is hidden when the app leaves the foreground.
- Non-exported app-private files; backup and screenshots disabled.
- HTTPS only, fixed Gemini hostname, redirects disabled, network timeouts and bounded response sizes.
- No remote command execution, downloaded executable code, root features, accessibility services, or broad storage permissions.
- Image imports limited to four images and 20 MB per image; decoding is downsampled for rendering.
- Read-only export provider validates filenames and requires explicit Android URI sharing grants.
- No hardcoded API keys, telemetry SDKs, or logging of provider error bodies.
- Signing credentials belong in GitHub Actions Secrets; never in source control. Protect the GitHub account with two-factor authentication.

This is risk reduction, not a guarantee against a rooted OS, malicious keyboard, compromised account, or physical access to an unlocked phone. Device Keystore protection varies by hardware. Release signing and functional testing do not certify Play Protect acceptance. A debug APK is for evaluation; use a permanent release certificate for ongoing personal use.

Review third-party workflow actions before trusting CI with a release key. Workflows use version tags; pin them to audited commit SHAs for a stricter supply-chain policy.
