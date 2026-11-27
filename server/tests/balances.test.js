import test from "node:test";
import assert from "node:assert/strict";
import { computeBalances } from "../src/balances.js";

const expense = (paidById, splits) => ({ paidById, splits });

test("computes net balances from expenses", () => {
  const { balances } = computeBalances(
    ["alice", "bob", "carol"],
    [
      expense("alice", [
        { userId: "alice", amountCents: 1000 },
        { userId: "bob", amountCents: 1000 },
        { userId: "carol", amountCents: 1000 },
      ]),
      expense("bob", [
        { userId: "alice", amountCents: 500 },
        { userId: "bob", amountCents: 500 },
      ]),
    ],
  );

  assert.deepEqual(
    Object.fromEntries(balances.map((balance) => [balance.userId, balance.netCents])),
    { alice: 1500, bob: -500, carol: -1000 },
  );
});

test("simplifies debts into direct transfers between debtors and creditors", () => {
  const { debts } = computeBalances(
    ["alice", "bob", "carol"],
    [
      expense("alice", [
        { userId: "alice", amountCents: 1000 },
        { userId: "bob", amountCents: 1000 },
      ]),
      expense("bob", [
        { userId: "alice", amountCents: 500 },
        { userId: "bob", amountCents: 500 },
      ]),
      expense("carol", [
        { userId: "alice", amountCents: 250 },
        { userId: "carol", amountCents: 250 },
      ]),
    ],
  );

  assert.deepEqual(debts, [
    { fromUserId: "bob", toUserId: "alice", amountCents: 250 },
    { fromUserId: "bob", toUserId: "carol", amountCents: 250 },
  ]);
});
