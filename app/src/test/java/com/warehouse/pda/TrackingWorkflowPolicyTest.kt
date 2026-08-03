package com.warehouse.pda

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackingWorkflowPolicyTest {
  @Test
  fun `outbound requires source and salesperson`() {
    assertEquals(
      "请选择出库仓库",
      trackingContextError(PdaOperation.DirectOutbound, OperationFormState())
    )

    assertEquals(
      "请选择销售人员",
      trackingContextError(
        PdaOperation.DirectOutbound,
        OperationFormState(directSourceWarehouseId = "warehouse-a")
      )
    )

    assertNull(
      trackingContextError(
        PdaOperation.DirectOutbound,
        OperationFormState(
          directSourceWarehouseId = "warehouse-a",
          directSalespersonId = "salesperson-a"
        )
      )
    )
  }

  @Test
  fun `warehouse outbound rejects same source and target`() {
    assertEquals(
      "目标仓库不能与出库仓库相同",
      trackingContextError(
        PdaOperation.DirectOutbound,
        OperationFormState(
          directSourceWarehouseId = "warehouse-a",
          directDestinationType = "warehouse",
          directTargetWarehouseId = "warehouse-a"
        )
      )
    )

    assertNull(
      trackingContextError(
        PdaOperation.DirectOutbound,
        OperationFormState(
          directSourceWarehouseId = "warehouse-a",
          directDestinationType = "warehouse",
          directTargetWarehouseId = "warehouse-b"
        )
      )
    )
  }

  @Test
  fun `return requires warehouse`() {
    assertEquals(
      "请选择回库仓库",
      trackingContextError(PdaOperation.SalesReturn, OperationFormState())
    )
    assertNull(
      trackingContextError(
        PdaOperation.SalesReturn,
        OperationFormState(returnWarehouseId = "warehouse-a")
      )
    )
  }

  @Test
  fun `scan announcement reports valid total`() {
    assertEquals("已扫 12 件", scanAnnouncement(validCount = 12, hasError = false))
    assertEquals("扫码错误，当前有效 12 件", scanAnnouncement(validCount = 12, hasError = true))
  }
}
