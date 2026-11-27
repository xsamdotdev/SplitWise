// End-to-end check against a running API and migrated database.
// Usage: npm run smoke  (optionally set API_BASE_URL)
import assert from "node:assert/strict";

const base = process.env.API_BASE_URL ?? "http://localhost:3000";

async function request(method, path, body, token) {
  const response = await fetch(`${base}${path}`, {
    method,
    headers: {
      ...(body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await response.json();
  assert.ok(response.ok, `${method} ${path} failed (${response.status}): ${JSON.stringify(json)}`);
  return json;
}

const suffix = Math.random().toString(36).slice(2, 8);
const username = (name) => `${name}_${suffix}`;
const tokens = {};
for (const name of ["alice", "bob", "carol"]) {
  tokens[name] = (
    await request("POST", "/auth/signup", {
      username: username(name),
      password: "password123",
    })
  ).token;
}

const created = await request(
  "POST",
  "/groups",
  { name: `Trip ${suffix}`, memberUsernames: [username("bob"), username("carol")] },
  tokens.alice,
);
const groupId = created.group.id;
assert.equal(created.group.members.length, 3);

const equalExpense = await request(
  "POST",
  `/groups/${groupId}/expenses`,
  {
    description: "Dinner",
    amount: "30.00",
    paidByUsername: username("alice"),
    splitType: "equal",
  },
  tokens.alice,
);
assert.equal(equalExpense.group.expenses[0].splits.reduce((sum, split) => sum + split.amountCents, 0), 3000);

const customExpense = await request(
  "POST",
  `/groups/${groupId}/expenses`,
  {
    description: "Taxi",
    amount: "10.00",
    paidByUsername: username("bob"),
    splitType: "custom",
    splits: [
      { username: username("alice"), amount: "2.50" },
      { username: username("bob"), amount: "5.00" },
      { username: username("carol"), amount: "2.50" },
    ],
  },
  tokens.alice,
);

const netByUsername = Object.fromEntries(
  customExpense.group.balances.map((balance) => [balance.username, balance.netCents]),
);
assert.equal(netByUsername[username("alice")], 1750);
assert.equal(netByUsername[username("bob")], -500);
assert.equal(netByUsername[username("carol")], -1250);
assert.equal(customExpense.group.simplifiedDebts.length, 2);

const restored = await request(
  "DELETE",
  `/groups/${groupId}/expenses/${customExpense.group.expenses[0].id}`,
  undefined,
  tokens.alice,
);
assert.deepEqual(
  restored.group.simplifiedDebts.map((debt) => [debt.fromUsername, debt.toUsername, debt.amountCents]),
  [
    [username("bob"), username("alice"), 1000],
    [username("carol"), username("alice"), 1000],
  ],
);

const groups = await request("GET", "/groups", undefined, tokens.bob);
assert.equal(groups.groups[0].myBalanceCents, -1000);

console.log("Smoke test passed");
