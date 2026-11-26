CREATE TABLE IF NOT EXISTS friends (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    date_of_birth TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_friends_dob ON friends(date_of_birth);

CREATE TABLE IF NOT EXISTS birthday_job_runs (
    run_date TEXT PRIMARY KEY,
    executed_at TEXT NOT NULL
);
