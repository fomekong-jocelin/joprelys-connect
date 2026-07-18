-- Revoke every active professional refresh session created before OTP became mandatory.
-- Patient-only sessions use a separate authentication channel and remain unaffected.
UPDATE auth_sessions
SET revoked_at = CURRENT_TIMESTAMP,
    revocation_reason = 'MFA_POLICY_CHANGE',
    revocation_source = 'SYSTEM',
    revoked_by_user_id = NULL,
    version = version + 1
WHERE revoked_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM users
      WHERE users.id = auth_sessions.user_id
        AND UPPER(TRIM(users.role)) <> 'PATIENT'
  );
