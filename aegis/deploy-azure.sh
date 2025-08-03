#!/bin/bash

# Azure deployment script for AEGIS Security Backend
# Southeast Asia deployment

set -e

# Configuration
RESOURCE_GROUP="aegis-security-rg"
LOCATION="southeastasia"
ACR_NAME="aegissecurityacr"
CONTAINER_NAME="aegis-backend"
IMAGE_NAME="aegis-backend"
TAG="latest"
REDIS_NAME="aegis-redis"

echo "🚀 Starting AEGIS Security Backend deployment to Azure Southeast Asia..."

# Check if logged in to Azure
echo "📋 Checking Azure login status..."
az account show > /dev/null 2>&1 || {
    echo "❌ Not logged in to Azure. Please run 'az login' first."
    exit 1
}

# Create resource group
echo "📦 Creating resource group..."
az group create \
    --name $RESOURCE_GROUP \
    --location $LOCATION \
    --output table

# Create Azure Container Registry
echo "🏗️ Creating Azure Container Registry..."
az acr create \
    --resource-group $RESOURCE_GROUP \
    --name $ACR_NAME \
    --sku Basic \
    --location $LOCATION \
    --admin-enabled true \
    --output table

# Get ACR login server
ACR_LOGIN_SERVER=$(az acr show --name $ACR_NAME --resource-group $RESOURCE_GROUP --query loginServer --output tsv)
echo "📍 ACR Login Server: $ACR_LOGIN_SERVER"

# Build and push Docker image
echo "🔨 Building Docker image..."
docker build -t $IMAGE_NAME:$TAG .

echo "🏷️ Tagging image for ACR..."
docker tag $IMAGE_NAME:$TAG $ACR_LOGIN_SERVER/$IMAGE_NAME:$TAG

echo "🔐 Logging in to ACR..."
az acr login --name $ACR_NAME

echo "📤 Pushing image to ACR..."
docker push $ACR_LOGIN_SERVER/$IMAGE_NAME:$TAG

# Create Redis Cache
echo "🔴 Creating Azure Redis Cache..."
az redis create \
    --resource-group $RESOURCE_GROUP \
    --name $REDIS_NAME \
    --location $LOCATION \
    --sku Basic \
    --vm-size c0 \
    --enable-non-ssl-port false \
    --output table

# Get Redis connection details
REDIS_HOST=$(az redis show --name $REDIS_NAME --resource-group $RESOURCE_GROUP --query hostName --output tsv)
REDIS_KEY=$(az redis list-keys --name $REDIS_NAME --resource-group $RESOURCE_GROUP --query primaryKey --output tsv)

echo "🔴 Redis Host: $REDIS_HOST"

# Create Container Instance
echo "🐳 Creating Azure Container Instance..."
az container create \
    --resource-group $RESOURCE_GROUP \
    --name $CONTAINER_NAME \
    --image $ACR_LOGIN_SERVER/$IMAGE_NAME:$TAG \
    --registry-login-server $ACR_LOGIN_SERVER \
    --registry-username $ACR_NAME \
    --registry-password $(az acr credential show --name $ACR_NAME --resource-group $RESOURCE_GROUP --query passwords[0].value --output tsv) \
    --dns-name-label aegis-backend-api \
    --ports 8080 \
    --cpu 2 \
    --memory 4 \
    --location $LOCATION \
    --environment-variables \
        SPRING_PROFILES_ACTIVE=prod \
        REDIS_HOST=$REDIS_HOST \
        REDIS_PORT=6380 \
        REDIS_PASSWORD=$REDIS_KEY \
        PORT=8080 \
        CORS_ALLOWED_ORIGINS=https://aegis-portal.azurewebsites.net \
    --output table

# Get container instance details
CONTAINER_FQDN=$(az container show --resource-group $RESOURCE_GROUP --name $CONTAINER_NAME --query ipAddress.fqdn --output tsv)
CONTAINER_IP=$(az container show --resource-group $RESOURCE_GROUP --name $CONTAINER_NAME --query ipAddress.ip --output tsv)

echo ""
echo "✅ Deployment completed successfully!"
echo ""
echo "📊 Deployment Summary:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌍 Region: Southeast Asia ($LOCATION)"
echo "📦 Resource Group: $RESOURCE_GROUP"
echo "🏗️ Container Registry: $ACR_LOGIN_SERVER"
echo "🐳 Container Instance: $CONTAINER_NAME"
echo "🔴 Redis Cache: $REDIS_HOST"
echo "🌐 Application URL: http://$CONTAINER_FQDN:8080/api"
echo "📍 Public IP: $CONTAINER_IP"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔍 To check application logs:"
echo "az container logs --resource-group $RESOURCE_GROUP --name $CONTAINER_NAME --follow"
echo ""
echo "🔄 To restart the container:"
echo "az container restart --resource-group $RESOURCE_GROUP --name $CONTAINER_NAME"
echo ""
echo "🗑️ To clean up resources:"
echo "az group delete --name $RESOURCE_GROUP --yes --no-wait"
echo ""
echo "🎉 AEGIS Security Backend is now running in Azure Southeast Asia!"
