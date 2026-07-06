from pathlib import Path

names = [
    "V50__harden_database_constraints.sql",
    "V51__merge_auth_users_into_users.sql",
    "V40__create_auth_users.sql",
    "V48__create_admin_user_and_management_fields.sql",
    "V49__create_coin_reward_records.sql",
]

base = Path("StudyPlatform-back/src/main/resources/db/migration")
for name in names:
    path = base / name
    print(f"\n--- {name} ---")
    print(path.read_text(encoding="utf-8", errors="replace")[:12000])
