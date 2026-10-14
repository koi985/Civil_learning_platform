ALTER TABLE classroom
  ADD COLUMN invite_code VARCHAR(6) NULL AFTER is_public,
  ADD UNIQUE KEY uk_classroom_invite_code (invite_code);
