-- V2 — Corrigir hash da senha do admin (admin123 em BCrypt válido)
UPDATE usuario SET senha = '$2b$10$rmrUl9Hr0gwJa0zn6U56k.y2fFjsfX0VKrN6CJy8QGUHyOuT4pVey'
WHERE username = 'admin';
