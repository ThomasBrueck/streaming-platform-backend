-- The category list was trimmed from 10 values to 5 (JUST_CHATTING, GAMING,
-- SOFTWARE, SPORTS, OTHER). Remap any existing rows before the app enum
-- stops recognizing the old values, since `category` has no DB-level check
-- constraint (Hibernate would throw on read otherwise).
UPDATE stream SET category = 'SOFTWARE' WHERE category = 'TECHNOLOGY';
UPDATE stream SET category = 'OTHER' WHERE category IN ('MUSIC', 'ART', 'COOKING', 'TRAVEL', 'EDUCATION');
