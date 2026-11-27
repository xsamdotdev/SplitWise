import { Router } from "express";
import { prisma } from "../prisma.js";
import { requireAuth } from "../auth.js";
import { ApiError, asyncHandler, notFound } from "../errors.js";
import { computeBalances } from "../balances.js";
import { parseAmountCents, splitEqually } from "../money.js";
import { validate, createGroupSchema, addMemberSchema, createExpenseSchema } from "../validation.js";

export const groupsRouter = Router();
groupsRouter.use(requireAuth);

const groupInclude = {
  members: { include: { user: true }, orderBy: { joinedAt: "asc" } },
  expenses: {
    include: { paidBy: true, splits: true },
    orderBy: { createdAt: "desc" },
  },
};

async function requireGroup(groupId, userId) {
  const group = await prisma.group.findFirst({
    where: { id: groupId, members: { some: { userId } } },
    include: groupInclude,
  });
  if (!group) throw notFound("Group not found");
  return group;
}

function unique(values) {
  return [...new Set(values)];
}

function serializeGroup(group) {
  const usernameById = new Map(group.members.map((m) => [m.userId, m.user.username]));
  const memberIds = group.members.map((m) => m.userId);
  const { balances, debts } = computeBalances(memberIds, group.expenses);

  return {
    id: group.id,
    name: group.name,
    createdAt: group.createdAt,
    createdByUsername: usernameById.get(group.createdById) ?? null,
    members: group.members.map((m) => ({ id: m.userId, username: m.user.username })),
    expenses: group.expenses.map((expense) => ({
      id: expense.id,
      description: expense.description,
      amountCents: expense.amountCents,
      createdAt: expense.createdAt,
      paidBy: { id: expense.paidById, username: usernameById.get(expense.paidById) },
      splits: expense.splits.map((split) => ({
        userId: split.userId,
        username: usernameById.get(split.userId),
        amountCents: split.amountCents,
      })),
    })),
    balances: balances.map((balance) => ({
      userId: balance.userId,
      username: usernameById.get(balance.userId),
      netCents: balance.netCents,
    })),
    simplifiedDebts: debts.map((debt) => ({
      fromUserId: debt.fromUserId,
      fromUsername: usernameById.get(debt.fromUserId),
      toUserId: debt.toUserId,
      toUsername: usernameById.get(debt.toUserId),
      amountCents: debt.amountCents,
    })),
  };
}

async function findUsersOrThrow(usernames) {
  const users = await prisma.user.findMany({ where: { username: { in: usernames } } });
  const found = new Set(users.map((user) => user.username));
  const missing = usernames.filter((username) => !found.has(username));
  if (missing.length) throw new ApiError(404, `Unknown username(s): ${missing.join(", ")}`);
  return users;
}

groupsRouter.get(
  "/",
  asyncHandler(async (req, res) => {
    const groups = await prisma.group.findMany({
      where: { members: { some: { userId: req.user.id } } },
      include: groupInclude,
      orderBy: { createdAt: "desc" },
    });

    res.json({
      groups: groups.map((group) => {
        const detail = serializeGroup(group);
        const mine = detail.balances.find((balance) => balance.userId === req.user.id);
        return {
          id: detail.id,
          name: detail.name,
          createdAt: detail.createdAt,
          memberCount: detail.members.length,
          myBalanceCents: mine?.netCents ?? 0,
        };
      }),
    });
  }),
);

groupsRouter.post(
  "/",
  asyncHandler(async (req, res) => {
    const { name, memberUsernames } = validate(createGroupSchema, req.body);
    const usernames = unique([req.user.username, ...memberUsernames]);
    const users = await findUsersOrThrow(usernames);

    const group = await prisma.group.create({
      data: {
        name,
        createdById: req.user.id,
        members: { create: users.map((user) => ({ userId: user.id })) },
      },
      include: groupInclude,
    });

    res.status(201).json({ group: serializeGroup(group) });
  }),
);

groupsRouter.get(
  "/:groupId",
  asyncHandler(async (req, res) => {
    const group = await requireGroup(req.params.groupId, req.user.id);
    res.json({ group: serializeGroup(group) });
  }),
);

groupsRouter.post(
  "/:groupId/members",
  asyncHandler(async (req, res) => {
    const group = await requireGroup(req.params.groupId, req.user.id);
    const { username } = validate(addMemberSchema, req.body);
    const [user] = await findUsersOrThrow([username]);

    if (group.members.some((member) => member.userId === user.id)) {
      throw new ApiError(409, "That user is already in this group");
    }

    await prisma.groupMember.create({ data: { groupId: group.id, userId: user.id } });
    const updated = await requireGroup(group.id, req.user.id);
    res.status(201).json({ group: serializeGroup(updated) });
  }),
);

groupsRouter.post(
  "/:groupId/expenses",
  asyncHandler(async (req, res) => {
    const group = await requireGroup(req.params.groupId, req.user.id);
    const body = validate(createExpenseSchema, req.body);
    const memberIds = new Set(group.members.map((member) => member.userId));
    const usernameToId = new Map(group.members.map((member) => [member.userId, member.user.username]));
    const idByUsername = new Map([...usernameToId].map(([id, username]) => [username, id]));

    if (!idByUsername.has(body.paidByUsername)) {
      throw new ApiError(400, "The payer must be a member of the group");
    }
    const paidById = idByUsername.get(body.paidByUsername);
    const totalCents = parseAmountCents(body.amount, "amount");

    let splits;
    if (body.splitType === "equal") {
      const participantUsernames = unique(body.participantUsernames ?? [...idByUsername.keys()]);
      const participantIds = participantUsernames.map((username) => {
        const id = idByUsername.get(username);
        if (!id) throw new ApiError(400, `Participant ${username} is not in this group`);
        return id;
      });
      splits = splitEqually(totalCents, participantIds);
    } else {
      if (!body.splits) throw new ApiError(400, "Custom splits require an amount for each participant");
      const seen = new Set();
      splits = body.splits.map((split) => {
        if (seen.has(split.username)) throw new ApiError(400, `Duplicate participant: ${split.username}`);
        seen.add(split.username);
        const userId = idByUsername.get(split.username);
        if (!userId) throw new ApiError(400, `Participant ${split.username} is not in this group`);
        return { userId, amountCents: parseAmountCents(split.amount, `split for ${split.username}`) };
      });
      const sum = splits.reduce((total, split) => total + split.amountCents, 0);
      if (sum !== totalCents) {
        throw new ApiError(400, `Custom splits must add up to the expense total (off by ${sum - totalCents} cents)`);
      }
    }

    if (splits.some((split) => !memberIds.has(split.userId))) {
      throw new ApiError(400, "Splits can only include group members");
    }

    await prisma.expense.create({
      data: {
        groupId: group.id,
        description: body.description,
        amountCents: totalCents,
        paidById,
        splits: { create: splits.map((split) => ({ userId: split.userId, amountCents: split.amountCents })) },
      },
    });

    const updated = await requireGroup(group.id, req.user.id);
    res.status(201).json({ group: serializeGroup(updated) });
  }),
);

groupsRouter.delete(
  "/:groupId/expenses/:expenseId",
  asyncHandler(async (req, res) => {
    const group = await requireGroup(req.params.groupId, req.user.id);
    const expense = group.expenses.find((item) => item.id === req.params.expenseId);
    if (!expense) throw notFound("Expense not found");
    if (expense.paidById !== req.user.id && group.createdById !== req.user.id) {
      throw new ApiError(403, "Only the payer or group creator can delete an expense");
    }

    await prisma.expense.delete({ where: { id: expense.id } });
    const updated = await requireGroup(group.id, req.user.id);
    res.json({ group: serializeGroup(updated) });
  }),
);
