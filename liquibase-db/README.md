# DMS Database Migrations

This project contains database migrations for the Document Management System (DMS) using Liquibase.

## Structure

- `src/main/resources/db/changelog/` - Contains all database migration files
- `src/main/resources/liquibase.properties` - Liquibase configuration
- `pom.xml` - Maven configuration with Liquibase plugin

## Migration Files

1. `001-create-initial-tables.xml` - Creates users and documents tables
2. `002-create-roles-and-permissions.xml` - Creates roles, permissions, and role_permissions tables
3. `003-create-user-management-and-audit.xml` - Creates audit_logs table
4. `004-create-notification-and-expiry-tracking.xml` - Creates notifications, expiry_tracking, and notification_preferences tables
5. `005-create-reporting-and-analytics.xml` - Creates reports, analytics, and dashboards tables
6. `006-create-advanced-features.xml` - Creates workflows and document_versions tables
7. `007-create-enterprise-features.xml` - Creates integration_configs and system_health_checks tables
8. `008-create-testing-and-optimization.xml` - Creates backup_records and tenants tables

## Usage

### Using Docker Compose

```bash
cd liquibase-db
docker-compose up -d
```

This will:
1. Start PostgreSQL database
2. Run Liquibase migrations to create all tables

### Using Maven

```bash
cd liquibase-db
mvn liquibase:update
```

### Manual Liquibase Commands

```bash
# Update database
liquibase update

# Rollback changes
liquibase rollback-count 1

# Generate changelog from existing database
liquibase generateChangeLog

# Validate changelog
liquibase validate
```

## Configuration

The database connection is configured in `liquibase.properties`:

```properties
driver=org.postgresql.Driver
url=jdbc:postgresql://localhost:5432/dms_db
username=dms_user
password=dms_password
```

## Benefits of Using Liquibase

1. **Version Control**: All database changes are tracked in XML files
2. **Rollback Support**: Easy rollback of changes
3. **Cross-Database**: Works with multiple database types
4. **Change Tracking**: Automatic tracking of applied changes
5. **Validation**: Built-in validation of changelog files
6. **Flexibility**: Supports various change types (SQL, XML, YAML)

## Integration with Main DMS Project

The main DMS backend project can now:
1. Remove Flyway dependencies
2. Use Liquibase for database management
3. Focus on business logic without database migration concerns
4. Have cleaner separation of concerns

