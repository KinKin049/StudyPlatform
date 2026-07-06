DELETE duplicate_null
FROM academy_course_enrollments duplicate_null
JOIN academy_course_enrollments kept_null
  ON kept_null.resource_type = duplicate_null.resource_type
 AND kept_null.course_id = duplicate_null.course_id
 AND kept_null.user_id IS NULL
 AND duplicate_null.user_id IS NULL
 AND kept_null.id < duplicate_null.id;

DELETE null_enrollment
FROM academy_course_enrollments null_enrollment
JOIN academy_course_enrollments default_enrollment
  ON default_enrollment.resource_type = null_enrollment.resource_type
 AND default_enrollment.course_id = null_enrollment.course_id
 AND default_enrollment.user_id = 1
WHERE null_enrollment.user_id IS NULL;

UPDATE academy_course_enrollments
SET user_id = 1
WHERE user_id IS NULL;

ALTER TABLE academy_course_enrollments
  MODIFY user_id BIGINT NOT NULL DEFAULT 1;
