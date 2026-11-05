# Privacy

Flowstudio has no ads, analytics, account system, or developer-operated server.

Drafts, imported scene images and exports are stored inside Android app-private storage. API keys are encrypted with AES-GCM; the encryption key is managed by Android Keystore. Cloud backup is disabled. Shared exports are provided read-only to apps you explicitly select.

When you request story generation or test a connection, the app sends your Gemini API key, story idea, and a fixed Foxy character description to Google's Gemini API over HTTPS. Google's terms and data handling apply. Imported scene images and finished videos are not sent to Google by this app. Provider errors are reduced to safe messages; no key logging is implemented.

The app uses INTERNET permission. It does not request access to your contacts, location, camera, microphone, or broad file storage. Scene access uses Android's document picker.

Delete your key in Vault. Delete videos in Queue. Use Plan → Delete all local studio data to erase everything, or uninstall the app. Erasing app data cannot revoke copies you shared with another app or provider. Revoke a compromised API key directly with its provider.

This is a personal app. The repository owner controls distribution and may amend this notice when functionality changes.
