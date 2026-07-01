-- 测井基础模板表：保存可复用的深度序列和基础 GR 曲线。
-- 仿真计算仍由前端完成，后端只提供模板数据持久化。
CREATE TABLE IF NOT EXISTS well_log_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  template_name VARCHAR(128) NOT NULL,
  depth_array JSON NOT NULL,
  gr_base JSON NOT NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测井仿真记录表：保存用户参数和前端生成的解释报告 JSON。
-- user_id 允许为空，便于未登录状态下进行毕设演示和本地测试。
CREATE TABLE IF NOT EXISTS well_log_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  porosity DOUBLE NOT NULL,
  oil_saturation DOUBLE NOT NULL,
  report_json JSON NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_well_log_record_user_time (user_id, create_time),
  CONSTRAINT fk_well_log_record_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 默认砂泥岩互层模板：用于前端打开页面后无需后端录入即可测试模板接口。
-- WHERE NOT EXISTS 保证重复执行迁移或修复数据时不会插入同名模板。
INSERT INTO well_log_template (template_name, depth_array, gr_base, remark)
SELECT
  '默认砂泥岩互层模板',
  JSON_ARRAY(0, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240, 260, 280, 300, 320, 340, 360, 380, 400, 420, 440, 460, 480, 500, 520, 540, 560, 580, 600, 620, 640, 660, 680, 700, 720, 740, 760, 780, 800, 820, 840, 860, 880, 900, 920, 940, 960, 980, 1000, 1020, 1040, 1060, 1080, 1100, 1120, 1140, 1160, 1180, 1200, 1220, 1240, 1260, 1280, 1300, 1320, 1340, 1360, 1380, 1400, 1420, 1440, 1460, 1480, 1500, 1520, 1540, 1560, 1580, 1600, 1620, 1640, 1660, 1680, 1700, 1720, 1740, 1760, 1780, 1800, 1820, 1840, 1860, 1880, 1900, 1920, 1940, 1960, 1980, 2000),
  JSON_ARRAY(95, 99, 102, 103, 102, 99, 95, 91, 88, 87, 88, 91, 94, 42, 44, 46, 48, 49, 49, 47, 44, 41, 38, 36, 35, 36, 39, 42, 45, 48, 49, 88, 86, 84, 82, 81, 82, 85, 88, 91, 94, 96, 97, 96, 36, 39, 42, 44, 45, 45, 44, 42, 39, 37, 35, 34, 35, 37, 40, 43, 45, 44, 92, 89, 87, 85, 85, 87, 90, 93, 96, 99, 100, 99, 96, 48, 51, 54, 56, 57, 56, 53, 50, 47, 44, 41, 40, 41, 44, 47, 50, 53, 86, 89, 91, 93, 92, 90, 87, 84, 81),
  '前端仿真可直接使用的默认深度与GR基础数据'
WHERE NOT EXISTS (
  SELECT 1 FROM well_log_template WHERE template_name = '默认砂泥岩互层模板'
);
