INSERT INTO rbac_role (id, code, name)
SELECT 1, 'ADMIN', '管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_role WHERE code = 'ADMIN');

INSERT INTO rbac_permission (id, code, http_method, path_pattern)
SELECT 1, 'ALL_INTERNAL', '*', '/internal/**' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_permission WHERE code = 'ALL_INTERNAL');

INSERT INTO rbac_user (id, user_id, username, password_hash, status)
SELECT 1, 'admin', 'admin', '120000$c22b60f3add6d1a293713a3875ae534d$fef896047fb8c0445e63c9349489fd9d99c23acd50b65a5e46ee33b63aa8f19a', 'ACTIVE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_user WHERE username = 'admin');

INSERT INTO rbac_user_role (user_id, role_code)
SELECT 'admin', 'ADMIN' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_user_role WHERE user_id = 'admin' AND role_code = 'ADMIN');

INSERT INTO rbac_role_permission (role_code, permission_code)
SELECT 'ADMIN', 'ALL_INTERNAL' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_role_permission WHERE role_code = 'ADMIN' AND permission_code = 'ALL_INTERNAL');
