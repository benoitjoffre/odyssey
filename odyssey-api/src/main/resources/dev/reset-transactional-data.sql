-- DEVELOPMENT ONLY
-- Deletes transactional Odyssey data.
-- DO NOT RUN IN PRODUCTION.
--
-- This script is intentionally NOT wired to Spring Boot startup.
-- It preserves travelers, agents, experiences, and travel_events,
-- including every Auth0 auth0_subject association on travelers/agents.
--
-- Safety guard: the session must explicitly opt in before this script runs:
--   SET odyssey.allow_dev_reset = 'true';
-- See the project instructions/report for the full local psql command.

BEGIN;

DO $$
BEGIN
    IF current_setting('odyssey.allow_dev_reset', true) IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION
            'Development reset refused. Set odyssey.allow_dev_reset=true explicitly in this session.';
    END IF;

    IF current_database() <> 'odyssey' THEN
        RAISE EXCEPTION
            'Development reset refused: expected database odyssey, connected to %.',
            current_database();
    END IF;
END
$$;

-- Prevent the running application/outbox scheduler from mutating these tables
-- during the reset transaction. Stop the backend before running this script so
-- no fresh transactional data is created immediately after COMMIT.
LOCK TABLE
    outbox_events,
    agent_notifications,
    bookings,
    payments,
    quotes,
    booking_requests,
    accommodation_criteria,
    flight_criteria,
    transfer_criteria,
    needs,
    trips,
    intents
IN ACCESS EXCLUSIVE MODE;

-- Outbox payloads have no foreign key by design. Delete them first so no stale
-- development event remains available to the OutboxProcessor.
DELETE FROM outbox_events;

-- Depend on booking_requests (and agents, which are preserved).
DELETE FROM agent_notifications;

-- Depend on quotes.
DELETE FROM bookings;
DELETE FROM payments;

-- Depend on booking_requests.
DELETE FROM quotes;

-- Depends on needs (and optionally agents, which are preserved).
DELETE FROM booking_requests;

-- One-to-one criteria tables depend directly on needs.
DELETE FROM accommodation_criteria;
DELETE FROM flight_criteria;
DELETE FROM transfer_criteria;

-- Needs depend on trips.
DELETE FROM needs;

-- Trips depend on travelers and optionally travel_events; both are preserved.
DELETE FROM trips;

-- Intents depend on travelers, which are preserved.
DELETE FROM intents;

-- Verification inside the same transaction. Every count must be zero before
-- COMMIT; preserved catalog/user tables are shown separately.
SELECT 'agent_notifications' AS table_name, count(*) AS remaining_rows FROM agent_notifications
UNION ALL SELECT 'outbox_events', count(*) FROM outbox_events
UNION ALL SELECT 'bookings', count(*) FROM bookings
UNION ALL SELECT 'payments', count(*) FROM payments
UNION ALL SELECT 'quotes', count(*) FROM quotes
UNION ALL SELECT 'booking_requests', count(*) FROM booking_requests
UNION ALL SELECT 'accommodation_criteria', count(*) FROM accommodation_criteria
UNION ALL SELECT 'flight_criteria', count(*) FROM flight_criteria
UNION ALL SELECT 'transfer_criteria', count(*) FROM transfer_criteria
UNION ALL SELECT 'needs', count(*) FROM needs
UNION ALL SELECT 'trips', count(*) FROM trips
UNION ALL SELECT 'intents', count(*) FROM intents
ORDER BY table_name;

SELECT 'travelers' AS preserved_table, count(*) AS preserved_rows FROM travelers
UNION ALL SELECT 'agents', count(*) FROM agents
UNION ALL SELECT 'experiences', count(*) FROM experiences
UNION ALL SELECT 'travel_events', count(*) FROM travel_events
ORDER BY preserved_table;

COMMIT;
