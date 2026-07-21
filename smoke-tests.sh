#!/bin/bash
# Smoke tests for the Clinic Appointment Backend
# Prerequisites: Docker containers running, WildFly datasource configured, EAR deployed
#
# Usage: ./smoke-tests.sh [base_url]

set -euo pipefail

BASE="${1:-http://localhost:8080/clinic-api/api}"
PASS=0
FAIL=0

green() { printf "\033[32m%s\033[0m\n" "$1"; }
red()   { printf "\033[31m%s\033[0m\n" "$1"; }
header(){ printf "\n\033[1m=== %s ===\033[0m\n" "$1"; }

assert_status() {
  local label="$1" expected="$2" actual="$3" body="$4"
  if [ "$actual" = "$expected" ]; then
    green "  PASS: $label (HTTP $actual)"
    PASS=$((PASS + 1))
  else
    red "  FAIL: $label — expected HTTP $expected, got $actual"
    echo "        $body"
    FAIL=$((FAIL + 1))
  fi
}

assert_json_field() {
  local label="$1" field="$2" expected="$3" body="$4"
  local actual
  actual=$(echo "$body" | python3 -c "import sys,json; print(json.load(sys.stdin).get('$field',''))" 2>/dev/null || true)
  if [ "$actual" = "$expected" ]; then
    green "  PASS: $label ($field=$actual)"
    PASS=$((PASS + 1))
  else
    red "  FAIL: $label — expected $field='$expected', got '$actual'"
    FAIL=$((FAIL + 1))
  fi
}

assert_not_empty() {
  local label="$1" body="$2"
  if [ -n "$body" ] && [ "$body" != "null" ]; then
    green "  PASS: $label (non-empty)"
    PASS=$((PASS + 1))
  else
    red "  FAIL: $label — empty response"
    FAIL=$((FAIL + 1))
  fi
}

# ─────────────────────────────────────────────────────────────────────────────
header "1. Health check (unauthenticated)"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" "$BASE/health")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /health" "200" "$CODE" "$BODY"
assert_json_field "Health status" "status" "UP" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "2. Readiness check (unauthenticated)"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" "$BASE/ready")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /ready" "200" "$CODE" "$BODY"
assert_json_field "Readiness status" "status" "READY" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "3. Login as admin"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"clinicId":1,"username":"admin","password":"Admin123"}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /auth/login" "200" "$CODE" "$BODY"
TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
AUTH="Authorization: Bearer $TOKEN"
green "  Token obtained: ${TOKEN:0:40}..."

# ─────────────────────────────────────────────────────────────────────────────
header "4. List users (seeded)"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/users?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /users" "200" "$CODE" "$BODY"
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
assert_not_empty "Users list" "$COUNT"

# ─────────────────────────────────────────────────────────────────────────────
header "5. List customers (seeded)"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/customers?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /customers" "200" "$CODE" "$BODY"
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
echo "  Info: $COUNT customers found"

# ─────────────────────────────────────────────────────────────────────────────
header "6. List doctors (seeded)"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/doctors?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /doctors" "200" "$CODE" "$BODY"
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
echo "  Info: $COUNT doctors found"

# ─────────────────────────────────────────────────────────────────────────────
header "7. List schedules for doctor 1"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/doctors/1/schedules?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /doctors/1/schedules" "200" "$CODE" "$BODY"
# Get first schedule details for booking
SCHED_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")
SCHED_DATE=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['availableDate'])")
echo "  Info: Using schedule id=$SCHED_ID date=$SCHED_DATE"

# ─────────────────────────────────────────────────────────────────────────────
header "8. Book an appointment"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/appointments?clinicId=1" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"clinicId\":1,\"customerId\":1,\"doctorId\":1,\"scheduleId\":$SCHED_ID,\"appointmentTime\":\"09:30\",\"notes\":\"Smoke test\"}")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /appointments" "201" "$CODE" "$BODY"
assert_json_field "Appointment status" "status" "BOOKED" "$BODY"
APPT_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "  Info: Appointment id=$APPT_ID created"

# ─────────────────────────────────────────────────────────────────────────────
header "9. Duplicate booking (same doctor + time) — should fail"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/appointments?clinicId=1" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"clinicId\":1,\"customerId\":2,\"doctorId\":1,\"scheduleId\":$SCHED_ID,\"appointmentTime\":\"09:30\",\"notes\":\"Duplicate\"}")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "Duplicate booking rejected" "400" "$CODE" "$BODY"
assert_json_field "Error code" "code" "REQUEST_ERROR" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "10. Book outside schedule window — should fail"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/appointments?clinicId=1" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"clinicId\":1,\"customerId\":1,\"doctorId\":1,\"scheduleId\":$SCHED_ID,\"appointmentTime\":\"12:00\",\"notes\":\"Outside window\"}")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "Outside window rejected" "400" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "11. List all appointments"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/appointments?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /appointments" "200" "$CODE" "$BODY"
COUNT=$(echo "$BODY" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))")
echo "  Info: $COUNT appointments found"

# ─────────────────────────────────────────────────────────────────────────────
header "12. Reschedule appointment to 10:00"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/appointments/$APPT_ID/reschedule?clinicId=1" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"newTime":"10:00"}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "PUT /reschedule" "200" "$CODE" "$BODY"
assert_json_field "Rescheduled time" "appointmentTime" "10:00" "$BODY"
assert_json_field "Reschedule count" "rescheduleCount" "1" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "13. Cancel appointment"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/appointments/$APPT_ID/cancel?clinicId=1" -H "$AUTH")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "PUT /cancel" "200" "$CODE" "$BODY"
assert_json_field "Cancelled status" "status" "CANCELLED" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "14. Cancel already-cancelled — should fail"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/appointments/$APPT_ID/cancel?clinicId=1" -H "$AUTH")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "Double-cancel rejected" "400" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "15. Reschedule cancelled appointment — should fail"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/appointments/$APPT_ID/reschedule?clinicId=1" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"newTime":"11:00"}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "Reschedule cancelled rejected" "400" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "16. Get appointment by ID"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/appointments/$APPT_ID?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /appointments/{id}" "200" "$CODE" "$BODY"
assert_json_field "Appointment ID" "id" "$APPT_ID" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "17. List appointments by customer"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/appointments/customer/1?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /appointments/customer/1" "200" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "18. List appointments by doctor"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/appointments/doctor/1?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /appointments/doctor/1" "200" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "19. Soft delete appointment"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/appointments/$APPT_ID?clinicId=1" -H "$AUTH")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "DELETE /appointments/{id}" "204" "$CODE" "$BODY"

# Verify deleted appointment is excluded from list
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/appointments?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
DELETED_VISIBLE=$(echo "$BODY" | python3 -c "
import sys,json
data = json.load(sys.stdin)
ids = [a['id'] for a in data if a.get('deleted')]
print(len(ids))
" 2>/dev/null || echo "0")
if [ "$DELETED_VISIBLE" = "0" ]; then
  green "  PASS: Deleted appointment excluded from list"
  PASS=$((PASS + 1))
else
  red "  FAIL: Deleted appointment still visible in list"
  FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
header "20. Unauthorized access (no token) — should fail"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" "$BASE/appointments?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "No token rejected" "401" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "21. OpenAPI endpoint (unauthenticated)"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" "$BASE/openapi")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /openapi" "200" "$CODE" "$BODY"
assert_json_field "OpenAPI version" "openapi" "3.0.3" "$BODY"
HAS_PATHS=$(echo "$BODY" | python3 -c "import sys,json; print('paths' in json.load(sys.stdin))" 2>/dev/null || echo "False")
if [ "$HAS_PATHS" = "True" ]; then
  green "  PASS: OpenAPI contains paths field"
  PASS=$((PASS + 1))
else
  red "  FAIL: OpenAPI missing paths field"
  FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
header "22. Booking wizard — start session"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/bookings" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"clinicId":1,"customerId":1}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /bookings" "200" "$CODE" "$BODY"
BOOKING_SESSION_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['sessionId'])")
echo "  Info: Booking session id=$BOOKING_SESSION_ID"

# ─────────────────────────────────────────────────────────────────────────────
header "23. Booking wizard — get summary"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" "$BASE/bookings/$BOOKING_SESSION_ID")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /bookings/{sessionId}" "200" "$CODE" "$BODY"
assert_json_field "Booking status" "status" "IN_PROGRESS" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "24. Booking wizard — select doctor"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/bookings/$BOOKING_SESSION_ID/doctor" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"doctorId":1}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "PUT /bookings/{sessionId}/doctor" "200" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "25. Booking wizard — select schedule"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/bookings/$BOOKING_SESSION_ID/schedule" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"scheduleId\":$SCHED_ID}")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "PUT /bookings/{sessionId}/schedule" "200" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "26. Booking wizard — select time"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/bookings/$BOOKING_SESSION_ID/time" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"appointmentTime":"10:30"}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "PUT /bookings/{sessionId}/time" "200" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "27. Booking wizard — confirm booking"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/bookings/$BOOKING_SESSION_ID/confirm" \
  -H "$AUTH" -H "Content-Type: application/json")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /bookings/{sessionId}/confirm" "200" "$CODE" "$BODY"
BOOKING_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
assert_not_empty "Booking has id" "$BOOKING_ID"
assert_json_field "Booking status" "status" "BOOKED" "$BODY"
assert_json_field "createdBy from JWT principal" "createdBy" "admin" "$BODY"
echo "  Info: Wizard-created appointment id=$BOOKING_ID"

# ─────────────────────────────────────────────────────────────────────────────
header "28. Booking wizard — cancel session"
# ─────────────────────────────────────────────────────────────────────────────
# Start a new session to cancel
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/bookings" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"clinicId":1,"customerId":2}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /bookings (new session)" "200" "$CODE" "$BODY"
CANCEL_SESSION_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['sessionId'])")

RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/bookings/$CANCEL_SESSION_ID" -H "$AUTH")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "DELETE /bookings/{sessionId}" "200" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "29. User creation"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/users" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"clinicId":1,"username":"testuser","password":"Test123456","roles":["USER"]}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /users" "200" "$CODE" "$BODY"
USER_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
assert_not_empty "User has id" "$USER_ID"
assert_json_field "Username" "username" "testuser" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "30. Customer creation"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/customers" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"clinicId":1,"fullName":"Test Customer","username":"testcust","email":"test@example.com","phone":"555-0100"}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /customers" "201" "$CODE" "$BODY"
CUST_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
assert_not_empty "Customer has id" "$CUST_ID"

# ─────────────────────────────────────────────────────────────────────────────
header "31. Doctor creation"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/doctors" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"clinicId":1,"fullName":"Test Doctor","username":"testdoc","specialty":"Neurology","slotMinutes":25}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /doctors" "201" "$CODE" "$BODY"
DOC_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
assert_not_empty "Doctor has id" "$DOC_ID"
assert_json_field "Specialty" "specialty" "Neurology" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "32. Empty body validation"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/bookings" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "Empty body rejected" "400" "$CODE" "$BODY"
assert_json_field "Error code" "code" "REQUEST_ERROR" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
header "33. Audit trail records the doctor creation"
# ─────────────────────────────────────────────────────────────────────────────
RESP=$(curl -s -w "\n%{http_code}" -H "$AUTH" \
  "$BASE/audit?clinicId=1&entityType=Doctor&entityId=$DOC_ID")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "GET /audit" "200" "$CODE" "$BODY"
AUDIT_ACTION=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['action'] if d else '')" 2>/dev/null || true)
if [ "$AUDIT_ACTION" = "DOCTOR_CREATED" ]; then
  green "  PASS: Audit entry recorded (action=DOCTOR_CREATED)"
  PASS=$((PASS + 1))
else
  red "  FAIL: Expected a DOCTOR_CREATED audit entry, got action='$AUDIT_ACTION'"
  FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
header "34. Role-forbidden access (403)"
# ─────────────────────────────────────────────────────────────────────────────
# Activate the USER-only account created in test 29, then confirm it cannot hit an ADMIN-only endpoint
RESP=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE/users/$USER_ID/activate" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"clinicId":1}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "PATCH /users/{id}/activate" "200" "$CODE" "$BODY"

RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"clinicId":1,"username":"testuser","password":"Test123456"}')
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "POST /auth/login (testuser)" "200" "$CODE" "$BODY"
USER_TOKEN=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

RESP=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $USER_TOKEN" "$BASE/users?clinicId=1")
BODY=$(echo "$RESP" | head -n-1)
CODE=$(echo "$RESP" | tail -1)
assert_status "USER role forbidden from GET /users" "403" "$CODE" "$BODY"

# ─────────────────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
printf "Results: \033[32m%d passed\033[0m, \033[31m%d failed\033[0m\n" "$PASS" "$FAIL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
