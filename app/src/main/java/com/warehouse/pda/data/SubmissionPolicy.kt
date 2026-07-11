package com.warehouse.pda.data

object SubmissionPolicy {
  const val MAX_BARCODES = 500

  fun remainingCapacity(existingCount: Int): Int {
    return (MAX_BARCODES - existingCount.coerceAtLeast(0)).coerceAtLeast(0)
  }

  fun accepts(existingCount: Int, incomingCount: Int): Boolean {
    if (existingCount < 0 || incomingCount < 0) return false
    return existingCount + incomingCount <= MAX_BARCODES
  }
}
