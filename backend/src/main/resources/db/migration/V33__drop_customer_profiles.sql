-- V33: Remove customer_profiles table
-- All fields in this table (age, city, membership_type, total_spend, items_purchased,
-- average_rating, satisfaction_level, discount_applied) were either never collected
-- at registration or are derivable from orders/reviews/addresses at query time.
-- Analytics now computes these on demand instead of storing fabricated values.

DROP TABLE IF EXISTS customer_profiles CASCADE;
