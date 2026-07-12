UPDATE academy_assignments
SET course_id = '26341267',
    course_title = '创新工程实践',
    assignment_title = '创新实践项目报告',
    assignment_description = '本作业用于展示创新实践方案、原型验证过程和项目复盘。'
WHERE assignment_code = 'data-cleaning-report'
  AND course_resource_type = 'micro-major-courses';

UPDATE academy_course_enrollments
SET course_id = '26341267'
WHERE resource_type = 'micro-major-courses'
  AND course_id = 'micro-data-001';
