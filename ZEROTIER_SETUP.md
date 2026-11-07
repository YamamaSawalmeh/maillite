# ZeroTier Setup Guide for MailLite

This guide explains how to use **MailLite** over the internet using **ZeroTier**, a virtual private network tool that allows devices to connect as if they were on the same local network.

## Prerequisites

1.  **ZeroTier Account**: Create a free account at [zerotier.com](https://www.zerotier.com/).
2.  **ZeroTier Client**: Installed on the **Server Computer** and all **Client Computers**.

## Step 1: Create a Network

1.  Log in to [ZeroTier Central](https://my.zerotier.com/).
2.  Click **Create A Network**.
3.  Note the **Network ID** (e.g., `a09acf0233...`).
4.  (Optional) Give it a name like "MailLite Network".
5.  Ensure access control is set to **Private** (recommended).

## Step 2: Join the Network (Server Side)

1.  Open the ZeroTier app on the computer running the **MailLite Server**.
2.  Select **Join Network**.
3.  Enter the **Network ID** from Step 1.
4.  **Authorize the device**:
    *   Go back to ZeroTier Central in your browser.
    *   Scroll down to the **Members** section.
    *   Check the box under **Auth?** next to your new device.
5.  Note the **Managed IP** assigned to this computer (e.g., `10.147.20.5`). This is your **Server IP**.

## Step 3: Join the Network (Client Side)

1.  Open the ZeroTier app on any computer that wants to run the **MailLite Client**.
2.  Select **Join Network**.
3.  Enter the same **Network ID**.
4.  **Authorize the device**:
    *   Go to ZeroTier Central.
    *   Authorize the new member.

## Step 4: Run MailLite

1.  **Server**:
    *   Run `./run_server.sh` on the server computer.
    *   The server automatically listens on all interfaces, including ZeroTier.

2.  **Client**:
    *   Run `./run_client.sh` on the client computer.
    *   In the **Server IP** field, enter the **Server's Managed IP** (e.g., `10.147.20.5`).
    *   Enter your username and password.
    *   Click **Login**.

## Troubleshooting

*   **Connection Refused**: Ensure the server is running and you authorized both devices in ZeroTier Central.
*   **Firewall**: Ensure your computer's firewall is not blocking Java or port `11223`. ZeroTier usually bypasses NAT, but local firewalls still apply.
*   **Ping Test**: Try to `ping <Server IP>` from the client computer to verify connectivity.
