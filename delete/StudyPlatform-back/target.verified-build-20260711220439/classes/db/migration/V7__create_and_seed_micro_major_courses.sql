CREATE TABLE IF NOT EXISTS micro_major_courses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  external_course_id VARCHAR(128) NOT NULL,
  course_name VARCHAR(255) NOT NULL,
  teacher_name VARCHAR(255) NULL,
  category VARCHAR(64) NULL,
  school_name VARCHAR(255) NULL,
  cover_url VARCHAR(1024) NULL,
  cover_file_path VARCHAR(512) NULL,
  start_time VARCHAR(64) NULL,
  participant_count INT NULL,
  course_comment TEXT NULL,
  course_description TEXT NULL,
  source_url VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_micro_major_courses_external_id (external_course_id),
  KEY idx_micro_major_courses_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO micro_major_courses
  (external_course_id, course_name, teacher_name, category, school_name, cover_url, cover_file_path, start_time, participant_count, course_comment, course_description, source_url)
VALUES
  ('26341412','思辨与创新','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/09e5f4738abc908e83de856e2961a829.jpg','storage/micro_major_course/创新创业微专业/26341412.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000006411#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341267','创新工程实践','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/03edd283051db735b0996576b69dbba7.jpg','storage/micro_major_course/创新创业微专业/26341267.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000006173#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341444','人工智能导论','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/d04f23768ca6604297265d5631488e0c.jpg','storage/micro_major_course/创新创业微专业/26341444.jpg','',1,'','','https://coursehome.zhihuishu.com/courseHome/1000009025#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341424','互联网与营销创新','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/b73249a00f61653d6e183e97bd0f28b6.jpg','storage/micro_major_course/创新创业微专业/26341424.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000006381#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341437','人力资源管理-基于创新创业视角','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/b0d28c0e806cfbd212804c937c6c2d0d.jpg','storage/micro_major_course/创新创业微专业/26341437.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000006818#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341415','创造性思维与创新方法','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/1dd6d1f7e4c788e4cf435670facb52d6.jpg','storage/micro_major_course/创新创业微专业/26341415.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000006163#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341419','创造性思维与创新方法(Triz版）','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/4a133a12a6858d7caa3d20f1b04c25da.jpg','storage/micro_major_course/创新创业微专业/26341419.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000007204#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341531','大学生创新创业基础','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/489082812cefee647027863b9c9f23b7.jpg','storage/micro_major_course/创新创业微专业/26341531.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000007584#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341465','元宇宙技术与应用','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/543c3d4df5036c85fbd64ef282711194.jpg','storage/micro_major_course/创新创业微专业/26341465.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000076369#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('26341456','大数据解析与应用导论','','创新创业微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/c8f14e2ffe3032a2ce157f15834bbc09.jpg','storage/micro_major_course/创新创业微专业/26341456.jpg','',0,'','','https://coursehome.zhihuishu.com/courseHome/1000062238#teachTeam?pageId=939959&wfwfid=999&websiteId=489006&pageId=939959&wfwfid=999&websiteId=489006'),
  ('69249806','公文写作规范与技巧','','卓越领导力微专业','中国石油大学（北京）','https://p.cldisk.com/star3/origin/6f98b6587ba7b1a8e175cd6fbd15d0bf.png','storage/micro_major_course/卓越领导力微专业/69249806.png','',0,'','','https://mooc1.chaoxing.com/course-ans/courseportal/252000559.html?edit=true&pageId=939959&wfwfid=999&websiteId=489006'),
  ('69249805','职业技能','','卓越领导力微专业','中国石油大学（北京）','https://p.cldisk.com/star3/origin/215258b4ea801572177fb93a2e8dfba8.png','storage/micro_major_course/卓越领导力微专业/69249805.png','',0,'','','https://mooc1.chaoxing.com/course-ans/courseportal/252000468.html?edit=true&pageId=939959&wfwfid=999&websiteId=489006'),
  ('69249802','商务礼仪','','卓越领导力微专业','中国石油大学（北京）','https://p.cldisk.com/star3/origin/128472a9e6f7246f6064e5a303316e2c.png','storage/micro_major_course/卓越领导力微专业/69249802.png','',0,'','','https://mooc1.chaoxing.com/course-ans/courseportal/251991900.html?edit=true&pageId=939959&wfwfid=999&websiteId=489006'),
  ('44229057','领导者特质','','卓越领导力微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/218af248bbb46d8f5d26dbfe2b023088.png','storage/micro_major_course/卓越领导力微专业/44229057.png','',0,'','','https://mooc1.chaoxing.com/mooc-ans/course/250499822.html?pageId=939959&wfwfid=999&websiteId=489006'),
  ('44228930','领导技巧','','卓越领导力微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/0bb338dc1443715324e1bfa1611ba209.png','storage/micro_major_course/卓越领导力微专业/44228930.png','',0,'','','https://mooc1.chaoxing.com/mooc-ans/course/250694776.html?pageId=939959&wfwfid=999&websiteId=489006'),
  ('44228067','团队管理','','卓越领导力微专业','中国石油大学（北京）','https://p.ananas.chaoxing.com/star3/origin/0df997417fe7af982d4c81d893d54e9b.png','storage/micro_major_course/卓越领导力微专业/44228067.png','',0,'','','https://mooc1.chaoxing.com/mooc-ans/course/250700669.html?pageId=939959&wfwfid=999&websiteId=489006')
ON DUPLICATE KEY UPDATE
  course_name = VALUES(course_name),
  teacher_name = VALUES(teacher_name),
  category = VALUES(category),
  school_name = VALUES(school_name),
  cover_url = VALUES(cover_url),
  cover_file_path = VALUES(cover_file_path),
  start_time = VALUES(start_time),
  participant_count = VALUES(participant_count),
  course_comment = VALUES(course_comment),
  course_description = VALUES(course_description),
  source_url = VALUES(source_url);
