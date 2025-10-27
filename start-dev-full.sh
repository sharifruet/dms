#!/bin/bash

# DMS Development Environment Startup Script
# This script starts the complete DMS development environment with monitoring

set -e

echo "🚀 Starting DMS Development Environment with Monitoring..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop first."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed or not in PATH."
    exit 1
fi

# Create necessary directories
print_status "Creating necessary directories..."
mkdir -p monitoring/grafana/dashboards
mkdir -p monitoring/grafana/datasources
mkdir -p monitoring/logstash/pipeline
mkdir -p monitoring/logstash/config

# Check if monitoring configuration files exist
if [ ! -f "monitoring/prometheus.yml" ]; then
    print_warning "Prometheus configuration not found. Creating default configuration..."
    # The monitoring files should already be created, but just in case
fi

# Start the services
print_status "Starting DMS services..."

# Start core services first
print_status "Starting core services (PostgreSQL, Redis, Elasticsearch)..."
docker-compose up -d postgres redis elasticsearch

# Wait for core services to be healthy
print_status "Waiting for core services to be ready..."
sleep 30

# Check if services are healthy
print_status "Checking service health..."

# Check PostgreSQL
if docker-compose exec postgres pg_isready -U dms_user -d dms_db > /dev/null 2>&1; then
    print_success "PostgreSQL is ready"
else
    print_error "PostgreSQL is not ready"
    exit 1
fi

# Check Redis
if docker-compose exec redis redis-cli ping > /dev/null 2>&1; then
    print_success "Redis is ready"
else
    print_error "Redis is not ready"
    exit 1
fi

# Check Elasticsearch
if curl -f http://localhost:9200/_cluster/health > /dev/null 2>&1; then
    print_success "Elasticsearch is ready"
else
    print_error "Elasticsearch is not ready"
    exit 1
fi

# Start backend and frontend
print_status "Starting backend and frontend services..."
docker-compose up -d backend frontend nginx

# Wait for backend to be ready
print_status "Waiting for backend to be ready..."
sleep 20

# Check backend health
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    print_success "Backend is ready"
else
    print_warning "Backend health check failed, but continuing..."
fi

# Start monitoring services
print_status "Starting monitoring services..."
docker-compose -f docker-compose.full.yml up -d prometheus grafana node-exporter

# Wait for monitoring services
print_status "Waiting for monitoring services to be ready..."
sleep 15

# Display service URLs
echo ""
print_success "🎉 DMS Development Environment is ready!"
echo ""
echo "📋 Service URLs:"
echo "=================="
echo "🌐 Frontend Application:    http://localhost:3000"
echo "🔧 Backend API:            http://localhost:8080"
echo "📊 API Documentation:      http://localhost:8080/swagger-ui.html"
echo "🔍 Health Check:           http://localhost:8080/actuator/health"
echo "📈 Prometheus Metrics:     http://localhost:9090"
echo "📊 Grafana Dashboard:      http://localhost:3001 (admin/admin123)"
echo "📊 Node Exporter:          http://localhost:9100"
echo "🗄️  PostgreSQL:            localhost:5432"
echo "⚡ Redis:                  localhost:6379"
echo "🔍 Elasticsearch:          http://localhost:9200"
echo ""

# Display default credentials
echo "🔐 Default Credentials:"
echo "======================="
echo "Grafana: admin / admin123"
echo "Database: dms_user / dms_password"
echo ""

# Check if all services are running
print_status "Checking all services status..."
docker-compose ps

echo ""
print_success "✅ All services are running successfully!"
print_status "You can now start developing your DMS application."
echo ""
print_status "To stop all services, run: docker-compose -f docker-compose.full.yml down"
print_status "To view logs, run: docker-compose logs -f [service-name]"
