/**
 * Compute per-user net balances and a minimal set of transfer instructions.
 * Positive net means the user is owed money; negative means they owe money.
 */
export function computeBalances(memberIds, expenses) {
  const net = new Map(memberIds.map((id) => [id, 0]));

  for (const expense of expenses) {
    for (const split of expense.splits) {
      if (split.userId === expense.paidById) continue;
      net.set(expense.paidById, (net.get(expense.paidById) ?? 0) + split.amountCents);
      net.set(split.userId, (net.get(split.userId) ?? 0) - split.amountCents);
    }
  }

  const balances = [...net.entries()].map(([userId, netCents]) => ({ userId, netCents }));

  const creditors = balances
    .filter((entry) => entry.netCents > 0)
    .map((entry) => ({ ...entry }))
    .sort((a, b) => b.netCents - a.netCents);
  const debtors = balances
    .filter((entry) => entry.netCents < 0)
    .map((entry) => ({ userId: entry.userId, owed: -entry.netCents }))
    .sort((a, b) => b.owed - a.owed);

  const debts = [];
  let creditorIndex = 0;
  for (const debtor of debtors) {
    let remaining = debtor.owed;
    while (remaining > 0 && creditorIndex < creditors.length) {
      const creditor = creditors[creditorIndex];
      const amount = Math.min(remaining, creditor.netCents);
      debts.push({ fromUserId: debtor.userId, toUserId: creditor.userId, amountCents: amount });
      remaining -= amount;
      creditor.netCents -= amount;
      if (creditor.netCents === 0) creditorIndex += 1;
    }
  }

  return { balances, debts };
}
