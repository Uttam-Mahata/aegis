#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=============================================="
echo "Starting Aegis Security Backend Services"
echo "=============================================="

# Function to start a service in a new terminal
start_service() {
    local service_name=$1
    local service_dir=$2
    local command=$3
    
    echo -e "${YELLOW}Starting $service_name...${NC}"
    
    # Check if gnome-terminal is available
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal --tab --title="$service_name" -- bash -c "cd $service_dir && $command; exec bash"
    # Check if xterm is available
    elif command -v xterm &> /dev/null; then
        xterm -T "$service_name" -e "cd $service_dir && $command; bash" &
    else
        # Fallback to running in background
        echo "No terminal emulator found. Running in background..."
        cd "$service_dir" && nohup $command > "$service_name.log" 2>&1 &
        echo "Log file: $service_dir/$service_name.log"
    fi
}

# Start Aegis Security API
start_service "Aegis Security API" "aegis" "./gradlew bootRun --args='--server.address=0.0.0.0'"

# Wait a bit before starting the next service
sleep 3

# Start UCO Bank Backend
start_service "UCO Bank Backend" "backend-app" "./gradlew bootRun --args='--server.address=0.0.0.0'"

echo ""
echo -e "${GREEN}Services are starting...${NC}"
echo ""
echo "Access URLs from your mobile device:"
echo "  - Aegis API: http://192.168.232.12:8080/api"
echo "  - Bank Backend: http://192.168.232.12:8081/api/v1"
echo ""
echo "To check if services are running, use: ./verify-services.sh"
echo "=============================================="