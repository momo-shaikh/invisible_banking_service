PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS account_holders (
    id INTEGER PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY,
    holder_id INTEGER NOT NULL,
    type TEXT NOT NULL,
    balance NUMERIC NOT NULL,
    FOREIGN KEY (holder_id) REFERENCES account_holders(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY,
    sender_account_id INTEGER,
    recipient_account_id INTEGER,
    amount NUMERIC NOT NULL,
    type TEXT NOT NULL,
    note TEXT,
    FOREIGN KEY (sender_account_id) REFERENCES accounts(id),
    FOREIGN KEY (recipient_account_id) REFERENCES accounts(id)
);
