package com.mileway.feature.approvals.di

import com.mileway.core.data.dao.ApprovalCommentDao
import com.mileway.core.data.dao.ClarificationDao
import com.mileway.feature.approvals.repository.ApprovalCommentRepository
import com.mileway.feature.approvals.repository.ClarificationRepository
import com.mileway.feature.approvals.repository.RoomApprovalCommentRepository
import com.mileway.feature.approvals.repository.RoomClarificationRepository
import com.mileway.feature.approvals.viewmodel.ApprovalsViewModel
import com.mileway.feature.approvals.viewmodel.ClarificationHistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val approvalsModule =
    module {
        // PLAN_V28 P28.2: the persistent clarification-room store (Room-backed via core:data's
        // ClarificationDao — see MilewayDatabase's migration 41→42).
        single<ClarificationRepository> { RoomClarificationRepository(get<ClarificationDao>()) }
        // PLAN_V28 P28.7: the permanent comment thread (Room-backed via core:data's
        // ApprovalCommentDao — see MilewayDatabase's migration 44→45).
        single<ApprovalCommentRepository> { RoomApprovalCommentRepository(get<ApprovalCommentDao>()) }
        viewModelOf(::ApprovalsViewModel)
        viewModelOf(::ClarificationHistoryViewModel)
    }
