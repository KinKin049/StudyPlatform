UPDATE online_open_courses
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';

UPDATE general_courses
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';

UPDATE micro_major_courses
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';

UPDATE excellent_textbooks
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';

UPDATE course_question_bank_sets
SET cover_url = CONCAT('/files/', CASE
  WHEN cover_file_path LIKE 'storage/%' THEN SUBSTRING(cover_file_path, 9)
  ELSE cover_file_path
END)
WHERE cover_url LIKE 'http%' AND cover_file_path IS NOT NULL AND cover_file_path <> '';
