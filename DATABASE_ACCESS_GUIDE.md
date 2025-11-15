# MySQL Database Access Guide

## Overview
This application connects to a MySQL database through an SSH tunnel. The database is hosted remotely at `ccscloud.dlsu.edu.ph:21013` and accessed locally via `localhost:3307`.

## Connection Details
- **Local Port**: `3307` (SSH tunnel endpoint)
- **Remote Server**: `ccscloud.dlsu.edu.ph:21013`
- **Database Name**: `cafe_db`
- **Username**: `student2`
- **Password**: `ITDBADM`

## Step-by-Step Setup

### 1. Set Up SSH Tunnel

**On Windows (PowerShell or Command Prompt):**
```bash
ssh -L 3307:ccscloud.dlsu.edu.ph:21013 your_username@ccscloud.dlsu.edu.ph
```

**On Linux/Mac:**
```bash
ssh -L 3307:ccscloud.dlsu.edu.ph:21013 your_username@ccscloud.dlsu.edu.ph
```

**Important Notes:**
- Replace `your_username` with your actual SSH username
- Keep this terminal window open while working with the database
- The tunnel forwards local port 3307 to the remote MySQL server

### 2. Verify SSH Tunnel is Active

**Check if port 3307 is listening:**
- **Windows**: `netstat -an | findstr 3307`
- **Linux/Mac**: `netstat -an | grep 3307` or `lsof -i :3307`

You should see `localhost:3307` in the LISTENING state.

### 3. Test Database Connection

**Option A: Using TestConnection.java (Recommended)**

1. Make sure your SSH tunnel is active (Step 1)
2. In NetBeans or your IDE, run `TestConnection.java`
3. You should see:
   ```
   ✅ SUCCESS: Connection established!
   Connected to: jdbc:mysql://localhost:3307/cafe_db
   Database: cafe_db
   MySQL Version: [version number]
   ✅ Query test successful! Database is accessible.
   ```

**Option B: Using MySQL Command Line Client**

If you have MySQL client installed:
```bash
mysql -h 127.0.0.1 -P 3307 -u student2 -p
# Enter password: ITDBADM
```

**Option C: Using MySQL Workbench**

1. Open MySQL Workbench
2. Create a new connection:
   - **Connection Name**: Cafe DB (or any name)
   - **Hostname**: `127.0.0.1` or `localhost`
   - **Port**: `3307`
   - **Username**: `student2`
   - **Password**: `ITDBADM`
   - **Default Schema**: `cafe_db`
3. Click "Test Connection" to verify
4. Click "OK" to save

### 4. Verify Connection in Your Application

The application uses `DatabaseConfig.getDBConnection()` which:
- Automatically loads the MySQL JDBC driver
- Connects using the configured credentials
- Returns a Connection object or null on failure

**Test in your application:**
```java
Connection conn = DatabaseConfig.getDBConnection();
if (conn != null) {
    System.out.println("Database connected successfully!");
    // Use the connection...
    DatabaseConfig.closeDBConnection(conn);
} else {
    System.out.println("Database connection failed!");
}
```

## Troubleshooting

### Problem: "Connection refused" or "Cannot connect"

**Solutions:**
1. ✅ **Check SSH tunnel is active**
   - Make sure the SSH tunnel terminal is still open
   - Verify with: `netstat -an | findstr 3307` (Windows) or `netstat -an | grep 3307` (Linux/Mac)

2. ✅ **Verify SSH credentials**
   - Make sure you're using the correct SSH username
   - Test SSH connection: `ssh your_username@ccscloud.dlsu.edu.ph`

3. ✅ **Check port availability**
   - Ensure port 3307 is not used by another application
   - Try a different local port if needed (update DatabaseConfig.java)

### Problem: "Access denied for user"

**Solutions:**
1. ✅ **Verify MySQL credentials**
   - Username: `student2`
   - Password: `ITDBADM`
   - Database: `cafe_db`

2. ✅ **Check database permissions**
   - Contact your database administrator if credentials don't work

### Problem: "MySQL JDBC Driver not found"

**Solutions:**
1. ✅ **Verify MySQL Connector is in classpath**
   - Check `pom.xml` has the MySQL connector dependency
   - Rebuild the project: `mvn clean install`
   - In NetBeans: Right-click project → Clean and Build

2. ✅ **Check JAR file location**
   - Verify `mysql-connector-1.0.jar` is in `target/cafedbapp-1.0/WEB-INF/lib/`

### Problem: "Communications link failure"

**Solutions:**
1. ✅ **SSH tunnel may have disconnected**
   - Restart the SSH tunnel
   - Check network connectivity

2. ✅ **Firewall blocking connection**
   - Ensure local firewall allows connections on port 3307
   - Check if antivirus is blocking the connection

## Quick Test Checklist

- [ ] SSH tunnel is active and running
- [ ] Port 3307 is listening (check with netstat)
- [ ] MySQL credentials are correct (student2/ITDBADM)
- [ ] MySQL JDBC driver is in classpath
- [ ] TestConnection.java runs successfully
- [ ] Application can connect using DatabaseConfig.getDBConnection()

## Connection String Details

The application uses this connection string:
```
jdbc:mysql://localhost:3307/cafe_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

**Parameters explained:**
- `useSSL=false`: Disables SSL for local tunnel connection
- `serverTimezone=UTC`: Sets timezone to avoid timezone issues
- `allowPublicKeyRetrieval=true`: Allows public key retrieval for authentication

## Using MySQL Workbench

### Setting Up Connection in MySQL Workbench:

1. **Open MySQL Workbench**
2. **Click the "+" icon** next to "MySQL Connections"
3. **Fill in connection details:**
   ```
   Connection Name: Cafe DB
   Hostname: 127.0.0.1
   Port: 3307
   Username: student2
   Password: [Click "Store in Keychain" and enter: ITDBADM]
   Default Schema: cafe_db
   ```
4. **Click "Test Connection"**
5. **If successful, click "OK"**

### Important Notes for MySQL Workbench:
- ⚠️ **SSH tunnel must be active** before connecting
- ⚠️ Use `127.0.0.1` or `localhost` (not the remote server address)
- ⚠️ Port must be `3307` (your local tunnel port)

## Security Notes

- 🔒 Keep your SSH credentials secure
- 🔒 Don't commit passwords to version control
- 🔒 The SSH tunnel encrypts the connection
- 🔒 Close SSH tunnel when not in use

## Need Help?

If you continue to have issues:
1. Check the error message in TestConnection.java output
2. Verify all steps in the troubleshooting section
3. Ensure your network allows SSH connections
4. Contact your system administrator if SSH access is restricted

