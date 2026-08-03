package com.warehouse.pda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionReliabilityTest {
  @Test
  fun retainsRequestsWhoseResultIsUncertain() {
    assertTrue(shouldRetainPendingSubmission(NetworkRequestException("timeout", RuntimeException())))
    assertTrue(shouldRetainPendingSubmission(ResponseReadException("invalid response", RuntimeException())))
    assertTrue(shouldRetainPendingSubmission(ApiRequestException(401, "session expired")))
    assertTrue(shouldRetainPendingSubmission(ApiRequestException(503, "unavailable")))
    assertTrue(shouldRetainPendingSubmission(ApiRequestException(409, "该业务仍在处理中")))
  }

  @Test
  fun releasesRequestsAfterExplicitBusinessFailure() {
    assertFalse(shouldRetainPendingSubmission(ApiRequestException(400, "库存不足")))
    assertFalse(shouldRetainPendingSubmission(ApiRequestException(403, "无权限")))
    assertFalse(shouldRetainPendingSubmission(ApiRequestException(409, "请求编号已用于不同业务内容")))
  }

  @Test
  fun barcodeSubmissionLimitMatchesServerContract() {
    assertTrue(SubmissionPolicy.accepts(existingCount = 0, incomingCount = 500))
    assertFalse(SubmissionPolicy.accepts(existingCount = 0, incomingCount = 501))
    assertTrue(SubmissionPolicy.accepts(existingCount = 499, incomingCount = 1))
    assertFalse(SubmissionPolicy.accepts(existingCount = 499, incomingCount = 2))
    assertEquals(1, SubmissionPolicy.remainingCapacity(499))
  }

  @Test
  fun recognizesOnlyUnauthorizedResponsesAsExpiredSessions() {
    assertTrue(isSessionExpired(ApiRequestException(401, "请重新登录")))
    assertFalse(isSessionExpired(ApiRequestException(403, "无权限")))
    assertFalse(isSessionExpired(NetworkRequestException("断网", RuntimeException())))
  }
}
