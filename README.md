# Document Management System (DMS)

## Phase 1: Foundation and Core Infrastructure ✅

This phase implements the foundation and core infrastructure for the DMS system including:
- ✅ Spring Boot 3.2.x backend with Java 25 LTS
- ✅ React.js 18.2.x frontend with TypeScript
- ✅ Docker-based local development environment
- ✅ PostgreSQL 16.x database
- ✅ Basic authentication and authorization
- ✅ REST API structure
- ✅ CI/CD pipeline with Jenkins
- ✅ Monitoring and logging setup
- ✅ Health check endpoints
- ✅ Comprehensive test suite

## 🚀 Quick Start

### Prerequisites
- Docker Desktop installed and running
- Git installed
- IDE (IntelliJ IDEA or VS Code)

### Option 1: Using Startup Script (Recommended)
```bash
# Make script executable (Linux/Mac)
chmod +x start-dev.sh

# Start development environment
./start-dev.sh
```

### Option 2: Start with Monitoring (Full Stack)
```bash
# Start development environment with monitoring
chmod +x start-dev-full.sh
./start-dev-full.sh
```

### Option 3: Manual Docker Compose
```bash
# Start core services only
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Start with monitoring
docker-compose -f docker-compose.full.yml up -d

# View logs
docker-compose logs -f
```

### Access Applications
- 🌐 **Frontend**: http://localhost:3000
- 🔧 **Backend API**: http://localhost:8080
- 📚 **API Documentation**: http://localhost:8080/swagger-ui.html
- 🔍 **Health Check**: http://localhost:8080/actuator/health
- 📈 **Prometheus Metrics**: http://localhost:9090
- 📊 **Grafana Dashboard**: http://localhost:3001 (admin/admin123)
- 📊 **Node Exporter**: http://localhost:9100
- 🗄️ **Database**: localhost:5432
- ⚡ **Redis**: localhost:6379
- 🔍 **Elasticsearch**: http://localhost:9200

### Default Login Credentials
- **Username**: `admin`
- **Password**: `admin123`
- **Grafana**: `admin` / `admin123`

## 📁 Project Structure

```
dms/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/bpdb/dms/
│   │   ├── controller/        # REST controllers
│   │   ├── entity/           # JPA entities
│   │   ├── repository/       # Data repositories
│   │   ├── security/        # Security configuration
│   │   ├── service/         # Business logic
│   │   └── dto/            # Data transfer objects
│   ├── src/main/resources/
│   │   ├── db/migration/    # Database migrations
│   │   └── application.yml   # Configuration
│   ├── Dockerfile           # Production build
│   ├── Dockerfile.dev       # Development build
│   └── pom.xml              # Maven dependencies
├── frontend/                 # React.js application
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── pages/          # Page components
│   │   ├── services/       # API services
│   │   ├── store/          # Redux store
│   │   └── hooks/          # Custom hooks
│   ├── Dockerfile           # Production build
│   ├── Dockerfile.dev       # Development build
│   └── package.json         # NPM dependencies
├── nginx/                   # Reverse proxy configuration
├── docker-compose.yml       # Multi-service Docker setup
├── docker-compose.dev.yml   # Development overrides
├── start-dev.sh            # Development startup script
└── README.md               # This file
```

## 🛠️ Technology Stack

### Backend
- **Java 25 LTS** - Latest Long Term Support version
- **Spring Boot 3.2.x** - Application framework
- **PostgreSQL 16.x** - Primary database
- **Redis 7.2.x** - Caching and session storage
- **Elasticsearch 8.11.x** - Search engine
- **JWT** - Authentication tokens
- **Flyway** - Database migrations

### Frontend
- **React.js 18.2.x** - UI framework
- **TypeScript 5.3.x** - Type safety
- **Material-UI 5.15.x** - Component library
- **Redux Toolkit 2.0.x** - State management
- **React Query 5.0.x** - Data fetching
- **React Router 6.20.x** - Routing

### DevOps
- **Docker 24.0.x** - Containerization
- **Docker Compose** - Multi-service orchestration
- **Nginx** - Reverse proxy
- **Maven** - Backend build tool
- **npm** - Frontend package manager

## 🔧 Development Commands

### Backend Development
```bash
# Run backend locally (requires Java 25)
cd backend
./mvnw spring-boot:run

# Build backend
./mvnw clean package

# Run tests
./mvnw test
```

### Frontend Development
```bash
# Run frontend locally (requires Node.js 20)
cd frontend
npm start

# Build frontend
npm run build

# Run tests
npm test
```

### Docker Commands
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# Restart specific service
docker-compose restart backend

# Rebuild and start
docker-compose up --build -d
```

## 🔐 Security Features

- **JWT Authentication** - Secure token-based authentication
- **Role-based Access Control** - Admin, Officer, Viewer, Auditor roles
- **Password Encryption** - BCrypt password hashing
- **CORS Configuration** - Cross-origin request handling
- **Input Validation** - Request validation and sanitization

## 📊 Database Schema

### Users Table
- User authentication and profile information
- Role-based access control
- Department assignment

### Documents Table
- Document metadata and file information
- User relationships and permissions
- Document type classification

## 🚦 Health Checks

All services include health check endpoints:
- **Backend**: http://localhost:8080/actuator/health
- **Database**: Built-in PostgreSQL health check
- **Redis**: Built-in Redis health check
- **Elasticsearch**: http://localhost:9200/_cluster/health

## 🐛 Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 3000, 8080, 5432, 6379, 9200 are available
2. **Docker not running**: Start Docker Desktop
3. **Services not starting**: Check logs with `docker-compose logs`
4. **Database connection issues**: Wait for PostgreSQL to fully initialize

### Useful Commands
```bash
# Check service status
docker-compose ps

# View specific service logs
docker-compose logs backend
docker-compose logs frontend

# Reset everything
docker-compose down -v
docker-compose up --build -d
```

## 📈 Next Steps (Phase 2)

Phase 2 will implement:
- Document upload and management
- OCR integration with Tesseract
- Document classification system
- Basic search functionality
- File validation and security scanning

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is proprietary software for BPDB (Bangladesh Power Development Board).
