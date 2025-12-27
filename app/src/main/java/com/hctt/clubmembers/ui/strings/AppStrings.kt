package com.hctt.clubmembers.ui.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage { EN, ZH;
    fun toggle() = if (this == EN) ZH else EN
}

data class AppStrings(
    val languageToggleLabel: String,
    val languageToggleContentDescription: String,
    val settings: String,
    val membersTitle: (Int) -> String,
    val addMember: String,
    val searchExpired: String,
    val searchExpiredTitle: String,
    val searchExpiredFieldLabel: String,
    val results: String,
    val back: String,
    val backToMembers: String,
    val noExpiry: String,
    val expired: String,
    val avatar: String,
    val name: String,
    val email: String,
    val phone: String,
    val expiration: String,
    val paymentAmount: String,
    val paymentHistory: String,
    val noPayments: String,
    val sortName: String,
    val sortCreated: String,
    val sortExpiration: String,
    val attachAvatar: String,
    val takePhoto: String,
    val save: String,
    val cancel: String,
    val addMemberTitle: String,
    val editMemberTitle: String,
    val adminLogin: String,
    val password: String,
    val rememberMe: String,
    val login: String,
    val continueWithGoogle: String,
    val settingsTitle: String,
    val offlineCache: (String) -> String,
    val lastSync: (String?) -> String,
    val syncNow: String
)

private val englishStrings = AppStrings(
    languageToggleLabel = "EN/中文",
    languageToggleContentDescription = "Toggle language",
    settings = "Settings",
    membersTitle = { count -> "Members ($count)" },
    addMember = "Add Member",
    searchExpired = "Search Expired",
    searchExpiredTitle = "Search Expired",
    searchExpiredFieldLabel = "Search expired",
    results = "Results",
    back = "Back",
    backToMembers = "Back to members",
    noExpiry = "No expiry",
    expired = "Expired",
    avatar = "Avatar",
    name = "Name",
    email = "Email",
    phone = "Phone",
    expiration = "Expiration (YYYY-MM-DD)",
    paymentAmount = "Payment Amount",
    paymentHistory = "Payment History",
    noPayments = "No payments yet",
    attachAvatar = "Attach Avatar",
    takePhoto = "Take Photo",
    save = "Save",
    cancel = "Cancel",
    addMemberTitle = "Add Member",
    editMemberTitle = "Edit Member",
    adminLogin = "Admin Login",
    password = "Password",
    rememberMe = "Remember me",
    login = "Login",
    continueWithGoogle = "Continue with Google",
    settingsTitle = "Settings",
    offlineCache = { cache -> "Offline cache: $cache" },
    lastSync = { last -> "Last sync: ${last ?: "Never"}" },
    syncNow = "Sync now",
    sortName = "Name",
    sortCreated = "Created",
    sortExpiration = "Expiration"
)

private val chineseStrings = AppStrings(
    languageToggleLabel = "中文/EN",
    languageToggleContentDescription = "切换语言",
    settings = "设置",
    membersTitle = { count -> "会员 ($count)" },
    addMember = "新增会员",
    searchExpired = "过期查询",
    searchExpiredTitle = "过期查询",
    searchExpiredFieldLabel = "搜索过期会员",
    results = "结果",
    back = "返回",
    backToMembers = "返回会员列表",
    noExpiry = "无到期日",
    expired = "已过期",
    avatar = "头像",
    name = "姓名",
    email = "邮箱",
    phone = "电话",
    expiration = "到期日 (YYYY-MM-DD)",
    paymentAmount = "缴费金额",
    paymentHistory = "缴费记录",
    noPayments = "暂无缴费记录",
    attachAvatar = "上传头像",
    takePhoto = "拍摄照片",
    save = "保存",
    cancel = "取消",
    addMemberTitle = "新增会员",
    editMemberTitle = "编辑会员",
    adminLogin = "管理员登录",
    password = "密码",
    rememberMe = "记住我",
    login = "登录",
    continueWithGoogle = "使用 Google 登录",
    settingsTitle = "设置",
    offlineCache = { cache -> "离线缓存: $cache" },
    lastSync = { last -> "上次同步: ${last ?: "从未"}" },
    syncNow = "立即同步",
    sortName = "姓名",
    sortCreated = "创建时间",
    sortExpiration = "到期日"
)

val LocalStrings = staticCompositionLocalOf { chineseStrings }

@Composable
fun ProvideStrings(language: AppLanguage, content: @Composable () -> Unit) {
    val strings = remember(language) {
        when (language) {
            AppLanguage.ZH -> chineseStrings
            AppLanguage.EN -> englishStrings
        }
    }
    CompositionLocalProvider(LocalStrings provides strings) { content() }
}
