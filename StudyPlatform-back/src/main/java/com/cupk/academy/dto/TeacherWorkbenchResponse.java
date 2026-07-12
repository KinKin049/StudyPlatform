package com.cupk.academy.dto;

import java.util.List;

/**
 * 教师工作台响应DTO，用于返回教师工作台的综合信息，包括待批改作业、未读评论等。
 */
public record TeacherWorkbenchResponse(
        int ungradedAssignments,
        int unreadComments,
        int ungradedExams,
        List<TeacherWorkbenchMetricResponse> metrics,
        List<TeacherMailboxMessageResponse> mailbox
) {
}
