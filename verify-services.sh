#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================================="
echo "Aegis Security Environment - Service Verification"
echo "=================================================="

# Function to check if a service is running
check_service() {
    local service_name=$1
    local port=$2
    local endpoint=$3
    
    echo -n "Checking $service_name on port $port... "
    
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port$endpoint" | grep -q "200\|301\|302"; then
        echo -e "${GREEN}✓ Running${NC}"
        return 0
    else
        echo -e "${RED}✗ Not running${NC}"
        return 1
    fi
}

# Function to check database
check_database() {
    local db_name=$1
    local db_user=$2
    local db_pass=$3
    
    echo -n "Checking PostgreSQL database $db_name... "
    
    if PGPASSWORD=$db_pass psql -h localhost -U $db_user -d $db_name -c '\q' 2>/dev/null; then
        echo -e "${GREEN}✓ Connected${NC}"
        return 0
    else
        echo -e "${RED}✗ Connection failed${NC}"
        return 1
    fi
}

# Function to check Redis
check_redis() {
    echo -n "Checking Redis... "
    
    if redis-cli ping 2>/dev/null | grep -q "PONG"; then
        echo -e "${GREEN}✓ Running${NC}"
        return 0
    else
        echo -e "${RED}✗ Not running${NC}"
        return 1
    fi
}

# Get local IP address
get_local_ip() {
    ip addr show | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | grep -v '127.0.0.1' | head -n 1
}

LOCAL_IP=$(get_local_ip)

echo ""
echo "1. System Information:"
echo "----------------------"
echo -e "${YELLOW}Local IP Address:${NC} $LOCAL_IP"
echo -e "${YELLOW}Hostname:${NC} $(hostname)"

echo ""
echo "2. Checking Prerequisites:"
echo "--------------------------"

# Check PostgreSQL
check_database "aegis_security" "aegis" "aegis_123"
check_database "ucobank_db" "ucobank_user" "ucobank_pass"

# Check Redis
check_redis

echo ""
echo "3. Checking Backend Services:"
echo "-----------------------------"

# Check Aegis API
check_service "Aegis Security API" 8080 "/api/v1/health"

# Check Bank Backend
check_service "UCO Bank Backend" 8081 "/api/v1/health"

echo ""
echo "4. Testing API Endpoints:"
echo "-------------------------"

# Test Aegis Admin endpoint (should return 401 without auth)
echo -n "Testing Aegis Admin API... "
status_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/admin/registration-keys")
if [ "$status_code" = "401" ]; then
    echo -e "${GREEN}✓ Protected (401 expected)${NC}"
else
    echo -e "${RED}✗ Unexpected status: $status_code${NC}"
fi

# Test Bank Backend accounts endpoint (should return 401 without signature)
echo -n "Testing Bank Backend API... "
status_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8081/api/v1/accounts/123456789012")
if [ "$status_code" = "401" ]; then
    echo -e "${GREEN}✓ Protected (401 expected)${NC}"
else
    echo -e "${RED}✗ Unexpected status: $status_code${NC}"
fi

echo ""
echo "5. Service URLs:"
echo "----------------"
echo -e "${YELLOW}From This Machine:${NC}"
echo "  - Aegis Security API: http://localhost:8080/api"
echo "  - UCO Bank Backend: http://localhost:8081/api/v1"
echo ""
echo -e "${YELLOW}From Mobile Device (Use these in Android app):${NC}"
echo "  - Aegis Security API: http://$LOCAL_IP:8080/api"
echo "  - UCO Bank Backend: http://$LOCAL_IP:8081/api/v1"

echo ""
echo "6. Quick Start Commands (with 0.0.0.0 binding):"
echo "------------------------------------------------"
echo "Start Aegis API:"
echo "  cd aegis && ./gradlew bootRun --args='--server.address=0.0.0.0'"
echo ""
echo "Start Bank Backend:"
echo "  cd backend-app && ./gradlew bootRun --args='--server.address=0.0.0.0'"
echo ""
echo "Install Android App:"
echo "  cd sfe && ./gradlew :app:installDebug"

echo ""
echo "7. Android App Configuration:"
echo "-----------------------------"
echo "Update the following files with your machine's IP ($LOCAL_IP):"
echo "  - sfe/app/src/main/java/com/aegis/sfe/UCOBankApplication.kt"
echo "  - sfe/app/src/main/java/com/aegis/sfe/data/api/ApiClientFactory.kt"

echo ""
echo "=================================================="