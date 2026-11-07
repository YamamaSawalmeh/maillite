# 📨 MailLite – Java NIO Mail System (TCP + UDP)

> **Computer Networks 1 – course project, An-Najah National University**

MailLite is a lightweight, custom email system built with Java. It features a high-performance non-blocking I/O (NIO) server and a user-friendly Swing-based client.

## Features

*   **Custom Protocol**: Text-based TCP protocol for reliable command execution.
*   **Real-time Notifications**: UDP-based instant notifications for new emails.
*   **Non-Blocking Server**: Built with Java NIO `Selector` to handle multiple clients efficiently on a single thread.
*   **Rich Client UI**: Java Swing interface with:
    *   Inbox, Sent, and Archive tabs.
    *   Real-time online user roster.
    *   Status management (Active, Busy, Away).
    *   Search functionality.
    *   Color-coded emails by sender.
*   **Persistence**: JSON-based storage for user data and emails.
*   **Auto-Cleanup**: Background job to automatically delete archived emails older than 30 days.

## Prerequisites

*   **Java Development Kit (JDK) 17** or higher.
*   **Bash** (for running the provided scripts on Linux/macOS), or Gradle on Windows: `gradle runServer` / `gradle runClient`.

### Remote Access (ZeroTier)
To connect clients over the internet, we recommend using **ZeroTier**.
See [ZEROTIER_SETUP.md](ZEROTIER_SETUP.md) for detailed instructions.

## How to Run

## Usage
1. **Start Server**: `./run_server.sh`
*This script will automatically download necessary dependencies (Gson), compile the code, and start the server on port 11223.*
2. **Start Client**: `./run_client.sh`
   - Enter Server IP (default `127.0.0.1` or ZeroTier IP).
   - Login with `alice`/`123` or `bob`/`123`.

### 2. Start the Client
Open a new terminal window for each client you want to run.

```bash
./run_client.sh
```

### 3. Login
You can use the default test accounts:
*   **Username**: `alice` / **Password**: `123`
*   **Username**: `bob` / **Password**: `123`
*   **Username**: `charlie` / **Password**: `123`

## Project Structure

*   `src/main/java/com/maillite/server`: Server-side logic (NIO Server, ClientHandler, Storage).
*   `src/main/java/com/maillite/client`: Client-side logic (Swing GUI, NetworkClient).
*   `src/main/java/com/maillite/common`: Shared constants and utilities.
*   `storage/`: Directory where user data and emails are saved (created automatically).

## Architecture

*   **Server**: Uses a single-threaded `Selector` loop to manage TCP connections. A separate thread handles the cleanup job.
*   **Client**: Uses a background thread for network listening and SwingUtilities for UI updates to ensure responsiveness.
*   **Protocol**: Custom commands like `HELO`, `AUTH`, `SEND`, `LIST`, `RETR`, `DELE`.
