# Running Backend in IDE

## Prerequisites

1. **Java 21** - Make sure you have JDK 21 installed
2. **Maven** - The project uses Maven for dependency management
3. **Docker Services Running** - You need PostgreSQL, Redis, and Elasticsearch running

## Quick Start

### Option A: Using Maven Wrapper (Recommended)

This is the simplest way to run the backend:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Or use the helper script:
```bash
cd backend
./run-local.sh
```

### Option B: Using IDE

### Step 1: Start Docker Services (Required)

The backend needs PostgreSQL, Redis, and Elasticsearch. You can start just these services using:

```bash
# Start only the required services
docker compose -f docker-compose.yml up -d postgres redis elasticsearch
```

Wait for services to be ready (about 30 seconds).

### Step 2: Run in IDE (Skip if using Maven Wrapper)

#### IntelliJ IDEA:
1. Open the project in IntelliJ IDEA
2. Navigate to `src/main/java/com/bpdb/dms/DmsApplication.java`
3. Right-click on `DmsApplication` class
4. Select "Run 'DmsApplication.main()'"
5. In the run configuration, add VM options or environment variables:
   - **Active profiles**: `local`
   - Or add `-Dspring.profiles.active=local` to VM options

#### VS Code:
1. Install the "Extension Pack for Java" extension
2. Open `src/main/java/com/bpdb/dms/DmsApplication.java`
3. Click the "Run" button above the main method
4. Add to `.vscode/settings.json`:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-21",
         "path": "/path/to/jdk-21"
       }
     ]
   }
   ```
5. Set active profile: `spring.profiles.active=local`

#### Eclipse:
1. Right-click on the project → Run As → Spring Boot App
2. Go to Run Configurations → Arguments
3. Add: `-Dspring.profiles.active=local`

### Step 3: Verify

Once running, you should see:
- `Started DmsApplication in X seconds`
- Application available at `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`

## Configuration

The `application-local.properties` file uses:
- **Database**: `localhost:5432` (instead of `postgres:5432`)
- **Redis**: `localhost:6379` (instead of `redis:6379`)
- **Elasticsearch**: `http://localhost:9200` (instead of `http://elasticsearch:9200`)

## Troubleshooting

### Connection Refused Errors
- Make sure Docker services are running: `docker compose ps`
- Check if services are accessible on localhost ports

### Port Already in Use
- Stop the Docker backend container: `docker compose stop backend`
- Or change the port in `application-local.properties`: `server.port=8081`

### Database Connection Issues
- Verify PostgreSQL is running: `docker compose ps postgres`
- Check if port 5432 is accessible: `telnet localhost 5432`

### Elasticsearch Connection Issues
- Verify Elasticsearch is running: `curl http://localhost:9200`
- Check Elasticsearch logs: `docker compose logs elasticsearch`

## Notes

- The `local` profile uses `localhost` instead of Docker service names
- OCR is disabled by default (`app.ocr.enabled=false`) to prevent crashes
- File uploads will be saved to `./uploads` directory (relative to project root)
- Make sure to create the uploads directory if it doesn't exist

## IDE Run Configuration Examples

### IntelliJ IDEA Run Configuration:
```
Main class: com.bpdb.dms.DmsApplication
VM options: -Dspring.profiles.active=local
Working directory: $PROJECT_DIR$/backend
```

### VS Code launch.json:
```json
{
  "type": "java",
  "name": "Launch DmsApplication",
  "request": "launch",
  "mainClass": "com.bpdb.dms.DmsApplication",
  "projectName": "dms-backend",
  "args": "--spring.profiles.active=local",
  "vmArgs": ""
}
```

