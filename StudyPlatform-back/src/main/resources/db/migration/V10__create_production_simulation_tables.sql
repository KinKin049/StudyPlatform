-- 抽油机仿真记录表：保存示功图模块的参数与前端生成的示功图 JSON 数据。
CREATE TABLE IF NOT EXISTS production_pump_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  stroke DOUBLE NOT NULL,
  stroke_times DOUBLE NOT NULL,
  pump_diameter DOUBLE NOT NULL,
  work_condition VARCHAR(32) NOT NULL,
  indicator_chart_data JSON NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_production_pump_user_time (user_id, create_time),
  CONSTRAINT fk_production_pump_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 油藏动态仿真记录表：保存前端计算后的日产油与日产水结果。
CREATE TABLE IF NOT EXISTS production_reservoir_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  formation_pressure DOUBLE NOT NULL,
  permeability DOUBLE NOT NULL,
  water_saturation DOUBLE NOT NULL,
  viscosity DOUBLE NOT NULL,
  daily_oil DOUBLE NOT NULL,
  daily_water DOUBLE NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_production_reservoir_user_time (user_id, create_time),
  CONSTRAINT fk_production_reservoir_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 注水开发仿真记录表：保存关键节点和完整产量曲线 JSON。
CREATE TABLE IF NOT EXISTS production_waterflood_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  injection_rate DOUBLE NOT NULL,
  effect_day INT NOT NULL,
  water_breakthrough_day INT NOT NULL,
  peak_oil DOUBLE NOT NULL,
  production_curve JSON NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_production_waterflood_user_time (user_id, create_time),
  CONSTRAINT fk_production_waterflood_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 压裂酸化仿真记录表：保存施工参数与前端计算后的增产结果。
CREATE TABLE IF NOT EXISTS production_stimulation_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  type VARCHAR(32) NOT NULL,
  sand_volume DOUBLE NULL,
  displacement DOUBLE NOT NULL,
  acid_volume DOUBLE NULL,
  fracture_length DOUBLE NOT NULL,
  stimulation_ratio DOUBLE NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_production_stimulation_user_time (user_id, create_time),
  CONSTRAINT fk_production_stimulation_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
