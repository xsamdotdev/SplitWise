package dev.xsam.splitwise.data

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(val id: String, val username: String, val createdAt: String? = null)

@Serializable
data class AuthResponse(val token: String, val user: UserDto)

@Serializable
data class MeResponse(val user: UserDto)

@Serializable
data class ErrorResponse(val error: String, val details: List<ErrorDetailDto>? = null)

@Serializable
data class ErrorDetailDto(val path: String = "", val message: String = "")

@Serializable
data class GroupSummaryDto(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val memberCount: Int,
    val myBalanceCents: Int,
)

@Serializable
data class GroupsResponse(val groups: List<GroupSummaryDto>)

@Serializable
data class MemberDto(val id: String, val username: String)

@Serializable
data class SplitDto(val userId: String, val username: String? = null, val amountCents: Int)

@Serializable
data class ExpenseDto(
    val id: String,
    val description: String,
    val amountCents: Int,
    val createdAt: String? = null,
    val paidBy: MemberDto,
    val splits: List<SplitDto> = emptyList(),
)

@Serializable
data class BalanceDto(val userId: String, val username: String? = null, val netCents: Int)

@Serializable
data class DebtDto(
    val fromUserId: String,
    val fromUsername: String? = null,
    val toUserId: String,
    val toUsername: String? = null,
    val amountCents: Int,
)

@Serializable
data class GroupDetailDto(
    val id: String,
    val name: String,
    val createdAt: String? = null,
    val createdByUsername: String? = null,
    val members: List<MemberDto>,
    val expenses: List<ExpenseDto>,
    val balances: List<BalanceDto>,
    val simplifiedDebts: List<DebtDto>,
)

@Serializable
data class GroupResponse(val group: GroupDetailDto)

@Serializable
data class SignupRequest(val username: String, val password: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class CreateGroupRequest(val name: String, val memberUsernames: List<String> = emptyList())

@Serializable
data class AddMemberRequest(val username: String)

@Serializable
data class CustomSplitRequest(val username: String, val amount: String)

@Serializable
data class CreateExpenseRequest(
    val description: String,
    val amount: String,
    val paidByUsername: String,
    val splitType: String = "equal",
    val participantUsernames: List<String>? = null,
    val splits: List<CustomSplitRequest>? = null,
)
