1.  **Secure Identity Provisioning:**
    * On the app's first launch, the SDK performs a secure handshake with the backend to establish a unique, trusted identity for the device. This results in a `Secret Key` that is locked away in the device's secure hardware (the Keystore).

2.  **API Request Integrity and Authenticity (HMAC Signing):**
    * For every sensitive network call, the SDK uses the device's unique `Secret Key` to create a cryptographic signature (HMAC). This mathematically proves to the backend that the request is coming from the legitimate device and that the data has not been tampered with in transit.

3.  **Secure Data Storage at Rest (The "Secure Vault"):**
    * The SDK provides a feature to securely store sensitive information (like user settings or cached data) on the phone's disk. It uses AES-256 envelope encryption, where the data is encrypted, and the key used for encryption is itself protected by the hardware Keystore. This makes the data unreadable even if an attacker gains access to the phone's files.

4.  **Runtime Environment Checks:**
    * The SDK performs checks at startup to determine if it is running in a compromised environment. This includes looking for evidence of the device being rooted or running on an emulator. In a production scenario, this is powerfully enhanced by using Google's Play Integrity API for a definitive verdict.