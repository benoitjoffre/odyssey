-- DEVELOPMENT ONLY
-- Allows Auth0 JIT provisioning before the Traveler profile is completed.
-- DO NOT RUN IN PRODUCTION WITHOUT A PROPER MIGRATION.

ALTER TABLE travelers
    ALTER COLUMN first_name DROP NOT NULL,
    ALTER COLUMN last_name DROP NOT NULL;