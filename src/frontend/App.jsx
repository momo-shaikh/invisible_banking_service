import { useEffect, useState } from "react";

const DEMO_USERS = [
  { name: "Ada Wong", email: "ada@example.com", password: "demo123" },
  { name: "Ava Carter", email: "ava@example.com", password: "demo123" },
  { name: "Ben Ortiz", email: "ben@example.com", password: "demo123" },
  { name: "Leon Kennedy", email: "leon@example.com", password: "demo123" },
];

const emptyTransaction = {
  senderAccountId: "",
  recipientAccountId: "",
  amount: "",
  transactionType: "DEPOSIT",
  note: "",
};

const emptyCardForm = {
  type: "DEBIT",
  status: "ACTIVE",
};

const emptyAccountForm = {
  accountType: "CHECKING",
  balance: "0.00",
};

const emptyStatementFilters = {
  fromDate: "",
  toDate: "",
};

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json", ...(options.headers ?? {}) },
    ...options,
  });

  const text = await response.text();
  const body = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(body?.error ?? "Request failed");
  }

  return body;
}

function App() {
  const [auth, setAuth] = useState(() => {
    const raw = localStorage.getItem("banking-auth");
    return raw ? JSON.parse(raw) : null;
  });
  const [loginForm, setLoginForm] = useState({ email: DEMO_USERS[0].email, password: DEMO_USERS[0].password });
  const [accounts, setAccounts] = useState([]);
  const [selectedAccountId, setSelectedAccountId] = useState(null);
  const [statement, setStatement] = useState(null);
  const [cardStatus, setCardStatus] = useState(null);
  const [cardLimit, setCardLimit] = useState(null);
  const [cardForm, setCardForm] = useState(emptyCardForm);
  const [accountForm, setAccountForm] = useState(emptyAccountForm);
  const [statementFilters, setStatementFilters] = useState(emptyStatementFilters);
  const [transactionForm, setTransactionForm] = useState(emptyTransaction);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    localStorage.setItem("banking-auth", JSON.stringify(auth));
  }, [auth]);

  useEffect(() => {
    if (!auth) {
      return;
    }

    loadAccounts(auth.holderId);
  }, [auth]);

  useEffect(() => {
    if (!selectedAccountId) {
      return;
    }

    loadAccountDetails(selectedAccountId);
  }, [selectedAccountId]);

  const selectedAccount = accounts.find((account) => account.id === selectedAccountId);
  const supportedCardAccount = selectedAccount?.accountType === "CHECKING" || selectedAccount?.accountType === "CREDIT";
  const allowedCardType = selectedAccount?.accountType === "CREDIT" ? "CREDIT" : "DEBIT";
  const transferOptions = accounts.filter((account) => account.id !== selectedAccountId);

  useEffect(() => {
    if (!supportedCardAccount) {
      return;
    }
    setCardForm((current) => (current.type === allowedCardType ? current : { ...current, type: allowedCardType }));
  }, [allowedCardType, supportedCardAccount]);

  async function loadAccounts(holderId, preferredAccountId = selectedAccountId) {
    try {
      const result = await api(`/holders/${holderId}/accounts`);
      setAccounts(result);
      const nextSelectedAccountId =
        result.find((account) => account.id === preferredAccountId)?.id ?? result[0]?.id ?? null;
      setSelectedAccountId(nextSelectedAccountId);
    } catch (error) {
      if (error.message === "Account holder not found") {
        localStorage.removeItem("banking-auth");
        setAuth(null);
        setAccounts([]);
        setSelectedAccountId(null);
        setStatement(null);
        setCardStatus(null);
        setCardLimit(null);
        setMessage("Session expired. Please sign in again.");
        return;
      }
      setMessage(error.message);
    }
  }

  async function loadAccountDetails(accountId, filters = statementFilters) {
    try {
      const query = new URLSearchParams();
      if (filters.fromDate) {
        query.set("fromDate", filters.fromDate);
      }
      if (filters.toDate) {
        query.set("toDate", filters.toDate);
      }
      const statementPath = query.size > 0 ? `/accounts/${accountId}/statement?${query.toString()}` : `/accounts/${accountId}/statement`;
      const [statementResult, statusResult, limitResult] = await Promise.all([
        api(statementPath),
        api(`/accounts/${accountId}/cards/status`).catch(() => null),
        api(`/accounts/${accountId}/cards/limit`).catch(() => null),
      ]);

      setStatement(statementResult);
      setCardStatus(statusResult?.status ?? null);
      setCardLimit(limitResult?.cardLimit ?? null);
      setTransactionForm((current) => ({
        ...current,
        senderAccountId: current.transactionType === "WITHDRAWAL" ? accountId : current.senderAccountId,
        recipientAccountId: current.transactionType === "DEPOSIT" ? accountId : current.recipientAccountId,
      }));
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function handleStatementFilterSubmit(event) {
    event.preventDefault();
    if (!selectedAccountId) {
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      await loadAccountDetails(selectedAccountId, statementFilters);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function clearStatementFilters() {
    if (!selectedAccountId) {
      setStatementFilters(emptyStatementFilters);
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      setStatementFilters(emptyStatementFilters);
      await loadAccountDetails(selectedAccountId, emptyStatementFilters);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleLogin(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      const result = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify(loginForm),
      });
      setAuth(result);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleTransaction(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");

    const payload = {
      amount: Number(transactionForm.amount),
      transactionType: transactionForm.transactionType,
      note: transactionForm.note || null,
    };

    if (transactionForm.transactionType === "DEPOSIT") {
      payload.recipientAccountId = Number(selectedAccountId);
    } else if (transactionForm.transactionType === "WITHDRAWAL") {
      payload.senderAccountId = Number(selectedAccountId);
    } else {
      payload.senderAccountId = Number(selectedAccountId);
      payload.recipientAccountId = Number(transactionForm.recipientAccountId);
    }

    try {
      await api("/transactions", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      await loadAccounts(auth.holderId, selectedAccountId);
      await loadAccountDetails(selectedAccountId);
      setTransactionForm({
        ...emptyTransaction,
        transactionType: transactionForm.transactionType,
      });
      setMessage("Transaction recorded");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleAccountCreate(event) {
    event.preventDefault();
    if (!auth) {
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      const createdAccount = await api("/accounts", {
        method: "POST",
        body: JSON.stringify({
          holderId: auth.holderId,
          accountType: accountForm.accountType,
          balance: Number(accountForm.balance),
        }),
      });
      await loadAccounts(auth.holderId, createdAccount.id);
      setAccountForm(emptyAccountForm);
      setMessage(`${createdAccount.accountType} account created`);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function createCard(event) {
    event.preventDefault();
    if (!selectedAccountId) {
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      await api(`/accounts/${selectedAccountId}/cards`, {
        method: "POST",
        body: JSON.stringify(cardForm),
      });
      await loadAccountDetails(selectedAccountId);
      setCardForm(emptyCardForm);
      setMessage("Card created");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function updateCardStatus(status) {
    if (!selectedAccountId) {
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      await api(`/accounts/${selectedAccountId}/cards/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      });
      await loadAccountDetails(selectedAccountId);
      setMessage(`Card set to ${status}`);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleDeleteAccount() {
    if (!selectedAccountId || !auth) {
      return;
    }
    if (!window.confirm("Delete this account and all of its cards and transactions? This cannot be undone and your money will vanish 😈")) {
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      await api(`/accounts/${selectedAccountId}`, { method: "DELETE" });
      await loadAccounts(auth.holderId, null);
      setStatement(null);
      setCardStatus(null);
      setCardLimit(null);
      setMessage("Account deleted");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  if (!auth) {
    return (
      <main className="shell">
        <section className="panel auth-panel">
          <div className="eyebrow">Invisible Banking</div>
          <h1>Sign in to demo banking flows.</h1>
          <p className="muted">Use one of the seeded users below or enter credentials manually.</p>

          <div className="demo-grid">
            {DEMO_USERS.map((user) => (
              <button
                key={user.email}
                className="demo-card"
                type="button"
                onClick={() => setLoginForm({ email: user.email, password: user.password })}
              >
                <span>{user.name}</span>
                <small>{user.email}</small>
              </button>
            ))}
          </div>

          <form className="stack" onSubmit={handleLogin}>
            <label>
              <span>Email</span>
              <input
                value={loginForm.email}
                onChange={(event) => setLoginForm({ ...loginForm, email: event.target.value })}
              />
            </label>
            <label>
              <span>Password</span>
              <input
                type="password"
                value={loginForm.password}
                onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })}
              />
            </label>
            <button className="primary" type="submit" disabled={busy}>
              {busy ? "Signing in..." : "Sign in"}
            </button>
          </form>

          {message ? <p className="message error">{message}</p> : null}
        </section>
      </main>
    );
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <div className="eyebrow">Invisible Banking</div>
          <h1>Welcome!</h1>
        </div>
        <button
          className="ghost"
          type="button"
          onClick={() => {
            localStorage.removeItem("banking-auth");
            setAuth(null);
            setAccounts([]);
            setSelectedAccountId(null);
            setStatement(null);
            setCardStatus(null);
            setCardLimit(null);
            setMessage("");
          }}
        >
          Log out
        </button>
      </header>

      <section className="layout">
        <aside className="panel account-list">
          <div className="section-title">Accounts</div>
          <form className="stack create-account-form" onSubmit={handleAccountCreate}>
            <label>
              <span>Account type</span>
              <select
                value={accountForm.accountType}
                onChange={(event) => setAccountForm({ ...accountForm, accountType: event.target.value })}
              >
                <option value="CHECKING">CHECKING</option>
                <option value="SAVINGS">SAVINGS</option>
                <option value="CREDIT">CREDIT</option>
              </select>
            </label>
            <label>
              <span>Opening balance</span>
              <input
                inputMode="decimal"
                value={accountForm.balance}
                onChange={(event) => setAccountForm({ ...accountForm, balance: event.target.value })}
                placeholder="0.00"
              />
            </label>
            <button className="primary" type="submit" disabled={busy}>
              {busy ? "Creating..." : "Create account"}
            </button>
          </form>
          {accounts.map((account) => (
            <button
              key={account.id}
              type="button"
              className={`account-item ${account.id === selectedAccountId ? "active" : ""}`}
              onClick={() => setSelectedAccountId(account.id)}
            >
              <strong>{account.accountType}</strong>
              <span>{Number(account.balance).toFixed(2)}</span>
            </button>
          ))}
        </aside>

        <section className="content">
          <div className="panel hero">
            <div>
              <div className="section-title">Selected account</div>
              <h2>{selectedAccount?.accountType ?? "No account selected"}</h2>
              <p className="muted mono">{selectedAccount?.id}</p>
            </div>
            <div className="hero-actions">
              <div className="metric">{selectedAccount ? Number(selectedAccount.balance).toFixed(2) : "--"}</div>
              <button className="danger" type="button" disabled={busy || !selectedAccountId} onClick={handleDeleteAccount}>
                {busy ? "Working..." : "Delete account"}
              </button>
            </div>
          </div>

          <div className="panel card-panel">
            <div className="section-title">Card</div>
            <div className="row">
              <div>
                <span className="muted">Status</span>
                <div>{cardStatus ?? "No card"}</div>
              </div>
              <div>
                <span className="muted">Limit</span>
                <div>{cardLimit ?? "--"}</div>
              </div>
            </div>
            {cardStatus ? (
              <div className="button-row">
                <button
                  className={`ghost ${cardStatus === "ACTIVE" ? "status-button-active" : ""}`}
                  type="button"
                  disabled={busy}
                  onClick={() => updateCardStatus("ACTIVE")}
                >
                  Set ACTIVE
                </button>
                <button
                  className={`ghost ${cardStatus === "FROZEN" ? "status-button-active" : ""}`}
                  type="button"
                  disabled={busy}
                  onClick={() => updateCardStatus("FROZEN")}
                >
                  Set FROZEN
                </button>
              </div>
            ) : supportedCardAccount ? (
              <form className="stack create-card-form" onSubmit={createCard}>
                <div className="muted">This account does not have a card yet.</div>
                <div className="form-grid compact-grid">
                  <label>
                    <span>Card type</span>
                    <select value={cardForm.type} disabled>
                      <option value={allowedCardType}>{allowedCardType}</option>
                    </select>
                  </label>
                  <label>
                    <span>Status</span>
                    <select
                      value={cardForm.status}
                      onChange={(event) => setCardForm({ ...cardForm, status: event.target.value })}
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="FROZEN">FROZEN</option>
                    </select>
                  </label>
                </div>
                <button className="primary" type="submit" disabled={busy}>
                  {busy ? "Creating..." : "Create card"}
                </button>
              </form>
            ) : selectedAccount ? (
              <p className="muted">Cards are only available for CHECKING and CREDIT accounts.</p>
            ) : null}
          </div>

          <form className="panel form-panel" onSubmit={handleTransaction}>
            <div className="section-title">Transaction</div>
            <div className="form-grid">
              <label>
                <span>Type</span>
                <select
                  value={transactionForm.transactionType}
                  onChange={(event) =>
                    setTransactionForm({
                      ...emptyTransaction,
                      transactionType: event.target.value,
                    })
                  }
                >
                  <option value="DEPOSIT">DEPOSIT</option>
                  <option value="WITHDRAWAL">WITHDRAWAL</option>
                  <option value="TRANSFER">TRANSFER</option>
                </select>
              </label>

              {transactionForm.transactionType === "TRANSFER" ? (
                <label>
                  <span>Recipient account</span>
                  {transferOptions.length > 0 ? (
                    <select
                      value={transactionForm.recipientAccountId}
                      onChange={(event) =>
                        setTransactionForm({ ...transactionForm, recipientAccountId: event.target.value })
                      }
                    >
                      <option value="">Select account</option>
                      {transferOptions.map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.accountType} · {account.id}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      value={transactionForm.recipientAccountId}
                      onChange={(event) =>
                        setTransactionForm({ ...transactionForm, recipientAccountId: event.target.value })
                      }
                      placeholder="Account ID"
                    />
                  )}
                </label>
              ) : null}

              <label>
                <span>Amount</span>
                <input
                  value={transactionForm.amount}
                  onChange={(event) => setTransactionForm({ ...transactionForm, amount: event.target.value })}
                  placeholder="0.00"
                />
              </label>

              <label className="wide">
                <span>Note</span>
                <input
                  value={transactionForm.note}
                  onChange={(event) => setTransactionForm({ ...transactionForm, note: event.target.value })}
                  placeholder="Optional"
                />
              </label>
            </div>
            <button className="primary" type="submit" disabled={busy || !selectedAccountId}>
              {busy ? "Submitting..." : "Submit transaction"}
            </button>
          </form>

          <div className="panel statement-panel">
            <div className="section-title">Statement</div>
            <form className="statement-filter-row" onSubmit={handleStatementFilterSubmit}>
              <label>
                <span>From</span>
                <input
                  type="date"
                  value={statementFilters.fromDate}
                  onChange={(event) => setStatementFilters({ ...statementFilters, fromDate: event.target.value })}
                />
              </label>
              <label>
                <span>To</span>
                <input
                  type="date"
                  value={statementFilters.toDate}
                  onChange={(event) => setStatementFilters({ ...statementFilters, toDate: event.target.value })}
                />
              </label>
              <div className="statement-filter-actions">
                <button className="primary" type="submit" disabled={busy || !selectedAccountId}>
                  {busy ? "Applying..." : "Apply"}
                </button>
                <button className="ghost" type="button" disabled={busy} onClick={clearStatementFilters}>
                  Clear
                </button>
              </div>
            </form>
            <div className="statement-meta">
              <span>From {statement?.fromDate ?? "--"}</span>
              <span>To {statement?.toDate ?? "--"}</span>
            </div>
            <div className="statement-list">
              {statement?.transactions?.length ? (
                statement.transactions.map((transaction) => (
                  <div key={transaction.id} className="statement-item">
                    <div>
                      <strong>{transaction.transactionType}</strong>
                      <div className="muted">{transaction.note || "No note"}</div>
                    </div>
                    <div className="statement-side">
                      <span>{Number(transaction.amount).toFixed(2)}</span>
                      <small className="muted">{transaction.createdAt}</small>
                    </div>
                  </div>
                ))
              ) : (
                <div className="muted">No transactions yet.</div>
              )}
            </div>
          </div>

          {message ? <p className="message">{message}</p> : null}
        </section>
      </section>
    </main>
  );
}

export default App;
