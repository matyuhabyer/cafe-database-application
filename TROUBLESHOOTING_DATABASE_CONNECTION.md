# Troubleshooting Database Connection Failed

## Quick Fixes

### 1. ✅ Check SSH Tunnel is Active (MOST COMMON ISSUE)

**Windows PowerShell:**
```powershell
# Check if port 3307 is listening
netstat -an | findstr 3307
```

**Linux/Mac:**
```bash
netstat -an | grep 3307
# or
lsof -i :3307
```

**If port 3307 is NOT listening, start SSH tunnel:**
```bash
ssh -L 3307:ccscloud.dlsu.edu.ph:21013 your_username@ccscloud.dlsu.edu.ph
```

**Important:** Keep the SSH tunnel terminal window open!

---

### 2. ✅ Run Diagnostic Servlet

1. **Start your server**
2. **Open browser**: `http://localhost:8080/cafedbapp/api/diagnostic`
3. **Check the JSON response** - it will show exactly what's wrong

The diagnostic will check:
- MySQL JDBC Driver availability
- Database connection
- Database metadata
- Query execution
- Table existence

---

### 3. ✅ Test Connection Directly

**In NetBeans:**
1. Right-click `TestConnection.java`
2. Select **Run File**
3. Check the output for detailed error messages

**Expected output if working:**
```
✅ SUCCESS: Connection established!
Connected to: jdbc:mysql://localhost:3307/cafe_db
Database: cafe_db
MySQL Version: [version]
✅ Query test successful! Database is accessible.
```

---

### 4. ✅ Check Server Logs

**In NetBeans:**
1. Go to **Window** → **Output**
2. Select **Server** tab
3. Look for error messages like:
   - "Database connection failed: ..."
   - "Connection refused"
   - "Communications link failure"

**Common error messages and solutions:**

| Error Message | Solution |
|--------------|----------|
| `Connection refused` | SSH tunnel not active |
| `Communications link failure` | SSH tunnel disconnected |
| `Access denied for user` | Wrong username/password |
| `Unknown database 'cafe_db'` | Database name incorrect |
| `MySQL JDBC Driver not found` | Driver not in classpath |

---

### 5. ✅ Verify Connection Settings

Check `DatabaseConfig.java`:
- **Host**: `localhost:3307` (SSH tunnel port)
- **User**: `student2`
- **Password**: `ITDBADM`
- **Database**: `cafe_db`

**Connection URL should be:**
```
jdbc:mysql://localhost:3307/cafe_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

---

## Step-by-Step Diagnostic Process

### Step 1: Verify SSH Tunnel
```powershell
# Windows
netstat -an | findstr 3307

# Should show something like:
# TCP    127.0.0.1:3307         0.0.0.0:0              LISTENING
```

**If not listening:**
```bash
ssh -L 3307:ccscloud.dlsu.edu.ph:21013 your_username@ccscloud.dlsu.edu.ph
```

### Step 2: Test with TestConnection.java
- Run `TestConnection.java` in NetBeans
- Check output for detailed errors

### Step 3: Check Diagnostic Endpoint
- Open: `http://localhost:8080/cafedbapp/api/diagnostic`
- Review JSON response for issues

### Step 4: Check Server Logs
- Look in NetBeans Output window (Server tab)
- Find detailed error messages

---

## Common Issues and Solutions

### Issue: "Connection refused" or "Communications link failure"

**Cause:** SSH tunnel is not active or disconnected

**Solution:**
1. Open a new terminal/PowerShell
2. Run: `ssh -L 3307:ccscloud.dlsu.edu.ph:21013 your_username@ccscloud.dlsu.edu.ph`
3. Keep terminal open
4. Verify with: `netstat -an | findstr 3307`
5. Retry your servlet request

---

### Issue: "Access denied for user 'student2'@'localhost'"

**Cause:** Wrong MySQL credentials

**Solution:**
1. Verify username: `student2`
2. Verify password: `ITDBADM`
3. Check if credentials changed (contact admin)

---

### Issue: "Unknown database 'cafe_db'"

**Cause:** Database name incorrect or database doesn't exist

**Solution:**
1. Verify database name in `DatabaseConfig.java`
2. Connect via MySQL Workbench to verify database exists
3. Check if database name changed

---

### Issue: "MySQL JDBC Driver not found"

**Cause:** MySQL connector JAR not in classpath

**Solution:**
1. Check `pom.xml` has MySQL dependency
2. In NetBeans: Right-click project → **Clean and Build**
3. Verify JAR is in `target/cafedbapp-1.0/WEB-INF/lib/mysql-connector-1.0.jar`

---

### Issue: Port 3307 already in use

**Cause:** Another application is using port 3307

**Solution:**
1. Find what's using the port:
   ```powershell
   netstat -ano | findstr 3307
   ```
2. Kill the process or use a different port
3. Update `DatabaseConfig.java` with new port

---

## Testing Checklist

Before testing servlets, verify:

- [ ] SSH tunnel is active (port 3307 listening)
- [ ] `TestConnection.java` runs successfully
- [ ] Diagnostic endpoint (`/api/diagnostic`) shows all green
- [ ] Server logs show no connection errors
- [ ] MySQL JDBC driver is in classpath
- [ ] Database credentials are correct

---

## Quick Test Commands

### Windows PowerShell:
```powershell
# Check SSH tunnel
netstat -an | findstr 3307

# Test connection (if you have curl)
curl http://localhost:8080/cafedbapp/api/diagnostic
```

### Test with TestConnection.java:
1. Right-click `TestConnection.java` in NetBeans
2. Select **Run File**
3. Check output

---

## Still Not Working?

1. **Check Diagnostic Endpoint**: `http://localhost:8080/cafedbapp/api/diagnostic`
   - This will show exactly what's failing

2. **Check Server Logs** in NetBeans Output window
   - Look for detailed error messages
   - Error messages include troubleshooting tips

3. **Verify SSH Access**:
   ```bash
   ssh your_username@ccscloud.dlsu.edu.ph
   ```
   - If this fails, you may not have SSH access

4. **Contact Support**:
   - Provide the diagnostic endpoint output
   - Include server log error messages
   - Include TestConnection.java output

---

## Expected Behavior

**When everything works:**
- SSH tunnel shows port 3307 listening
- TestConnection.java shows "✅ SUCCESS"
- Diagnostic endpoint shows all "✅"
- Servlets can connect to database
- No errors in server logs

**When something is wrong:**
- Diagnostic endpoint will show exactly what failed
- Server logs will have detailed error messages
- TestConnection.java will show specific error

