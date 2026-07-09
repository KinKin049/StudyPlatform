package com.cupk.academy.dto;

import java.util.List;

public record TeacherWorkbenchResponse(
        int ungradedAssignments,
        int unreadComments,
        int ungradedExams,
        List<TeacherWorkbenchMetricResponse> metrics,
        List<TeacherMailboxMessageResponse> mailbox
) {
}
