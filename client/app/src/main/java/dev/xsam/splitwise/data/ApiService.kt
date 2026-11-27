package dev.xsam.splitwise.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(@Header("Authorization") token: String): MeResponse

    @GET("groups")
    suspend fun groups(@Header("Authorization") token: String): GroupsResponse

    @POST("groups")
    suspend fun createGroup(
        @Header("Authorization") token: String,
        @Body request: CreateGroupRequest,
    ): GroupResponse

    @GET("groups/{groupId}")
    suspend fun group(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: String,
    ): GroupResponse

    @POST("groups/{groupId}/members")
    suspend fun addMember(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: String,
        @Body request: AddMemberRequest,
    ): GroupResponse

    @POST("groups/{groupId}/expenses")
    suspend fun addExpense(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: String,
        @Body request: CreateExpenseRequest,
    ): GroupResponse

    @DELETE("groups/{groupId}/expenses/{expenseId}")
    suspend fun deleteExpense(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: String,
        @Path("expenseId") expenseId: String,
    ): GroupResponse
}
