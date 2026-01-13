#!/bin/bash

# Banking Microservices - End-to-End Demo Script
# This script demonstrates the complete flow: Customer → Account → Deposit

set -e  # Exit on error

echo "=========================================="
echo "Banking Microservices - E2E Demo"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URLs
CUSTOMER_SERVICE="http://localhost:8081"
ACCOUNT_SERVICE="http://localhost:8082"
TRANSACTION_SERVICE="http://localhost:8083"

echo -e "${BLUE}Step 1: Create a Customer${NC}"
echo "POST $CUSTOMER_SERVICE/customers"
echo ""

CUSTOMER_RESPONSE=$(curl -s -X POST "$CUSTOMER_SERVICE/customers" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nguyen Van A",
    "email": "nguyenvana@example.com"
  }')

echo "$CUSTOMER_RESPONSE" | jq '.'
CUSTOMER_ID=$(echo "$CUSTOMER_RESPONSE" | jq -r '.id')

echo ""
echo -e "${GREEN}✓ Customer created with ID: $CUSTOMER_ID${NC}"
echo ""
sleep 2

# ==========================================

echo -e "${BLUE}Step 2: Create an Account for the Customer${NC}"
echo "POST $ACCOUNT_SERVICE/accounts"
echo ""

ACCOUNT_RESPONSE=$(curl -s -X POST "$ACCOUNT_SERVICE/accounts" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"initialBalance\": 0.00
  }")

echo "$ACCOUNT_RESPONSE" | jq '.'
ACCOUNT_ID=$(echo "$ACCOUNT_RESPONSE" | jq -r '.id')

echo ""
echo -e "${GREEN}✓ Account created with ID: $ACCOUNT_ID${NC}"
echo ""
sleep 2

# ==========================================

echo -e "${BLUE}Step 3: Make a Deposit${NC}"
echo "POST $TRANSACTION_SERVICE/transactions/deposit"
echo ""

DEPOSIT_RESPONSE=$(curl -s -X POST "$TRANSACTION_SERVICE/transactions/deposit" \
  -H "Content-Type: application/json" \
  -d "{
    \"accountId\": \"$ACCOUNT_ID\",
    \"amount\": 1000.00,
    \"currency\": \"USD\"
  }")

echo "$DEPOSIT_RESPONSE" | jq '.'
TRANSACTION_ID=$(echo "$DEPOSIT_RESPONSE" | jq -r '.id')

echo ""
echo -e "${GREEN}✓ Deposit transaction created with ID: $TRANSACTION_ID${NC}"
echo ""
sleep 3

# ==========================================

echo -e "${BLUE}Step 4: Check Account Balance${NC}"
echo "GET $ACCOUNT_SERVICE/accounts/$ACCOUNT_ID"
echo ""

ACCOUNT_DETAILS=$(curl -s -X GET "$ACCOUNT_SERVICE/accounts/$ACCOUNT_ID")
echo "$ACCOUNT_DETAILS" | jq '.'

BALANCE=$(echo "$ACCOUNT_DETAILS" | jq -r '.balance')
echo ""
echo -e "${GREEN}✓ Current Balance: $BALANCE USD${NC}"
echo ""

# ==========================================

echo -e "${BLUE}Step 5: Check Transaction Status${NC}"
echo "GET $TRANSACTION_SERVICE/transactions/$TRANSACTION_ID"
echo ""

TRANSACTION_DETAILS=$(curl -s -X GET "$TRANSACTION_SERVICE/transactions/$TRANSACTION_ID")
echo "$TRANSACTION_DETAILS" | jq '.'

TRANSACTION_STATUS=$(echo "$TRANSACTION_DETAILS" | jq -r '.status')
echo ""
echo -e "${GREEN}✓ Transaction Status: $TRANSACTION_STATUS${NC}"
echo ""

# ==========================================

echo "=========================================="
echo -e "${GREEN}Demo Complete!${NC}"
echo "=========================================="
echo ""
echo "Summary:"
echo "  Customer ID:    $CUSTOMER_ID"
echo "  Account ID:     $ACCOUNT_ID"
echo "  Transaction ID: $TRANSACTION_ID"
echo "  Final Balance:  $BALANCE USD"
echo "  Status:         $TRANSACTION_STATUS"
echo ""
echo -e "${YELLOW}Note: The transaction may take a few seconds to process via Kafka.${NC}"
echo -e "${YELLOW}If status is PENDING, wait a moment and check again.${NC}"
echo ""
