package com.warehouse.pda.data

data class ApiEnvelope<T>(
  val data: T? = null,
  val error: String? = null
)

data class CurrentUser(
  val id: String,
  val username: String,
  val displayName: String,
  val roles: List<UserRole>
)

data class UserRole(
  val code: String,
  val name: String
)

data class Goods(
  val id: String,
  val code: String,
  val name: String,
  val category: String,
  val unit: String,
  val spec: String,
  val sortOrder: Int = 0,
  val status: String
)

data class WarehouseRecord(
  val id: String,
  val code: String,
  val name: String,
  val type: String,
  val parentId: String?,
  val manager: String,
  val sortOrder: Int = 0,
  val status: String
)

data class StorageLocation(
  val id: String,
  val warehouseId: String,
  val zone: String,
  val code: String,
  val name: String,
  val status: String
)

data class Salesperson(
  val id: String,
  val code: String,
  val name: String,
  val phone: String,
  val region: String,
  val status: String
)

data class TerminalStore(
  val id: String,
  val name: String,
  val contact: String,
  val phone: String,
  val address: String,
  val status: String
)

data class WarehouseStock(
  val id: String,
  val warehouseId: String,
  val goodsId: String,
  val quantity: Int,
  val lastChangedAt: String
)

data class WarehouseState(
  val goods: List<Goods>,
  val warehouses: List<WarehouseRecord>,
  val locations: List<StorageLocation>,
  val salespeople: List<Salesperson>,
  val terminalStores: List<TerminalStore>,
  val warehouseStocks: List<WarehouseStock>? = emptyList()
)

data class InventoryItem(
  val id: String,
  val barcode: String,
  val goodsId: String,
  val ownerType: String,
  val warehouseId: String?,
  val locationId: String?,
  val salespersonId: String?,
  val status: String,
  val productionDate: String?,
  val shelfLifeDate: String?,
  val inboundSource: String,
  val lastMovedAt: String
)

data class StockMovement(
  val id: String,
  val itemId: String,
  val barcode: String,
  val goodsId: String,
  val type: String,
  val fromLabel: String,
  val toLabel: String,
  val operator: String,
  val occurredAt: String,
  val note: String
)

data class InventoryDetailResult(
  val item: InventoryItem,
  val movements: List<StockMovement>
)

data class BarcodeValidationRequest(
  val mode: String,
  val barcodes: List<String>,
  val goodsId: String? = null,
  val warehouseId: String? = null
)

data class BarcodeValidationResult(
  val barcode: String,
  val ok: Boolean,
  val label: String,
  val detail: String,
  val item: InventoryItem? = null
)

data class LoginRequest(
  val username: String,
  val password: String
)

data class LogoutResult(
  val loggedOut: Boolean
)

data class HealthStatus(
  val status: String,
  val database: String,
  val webVersion: String,
  val apiContractVersion: String,
  val serverTime: String
)

data class InboundSubmitRequest(
  val source: String,
  val warehouseId: String,
  val locationId: String,
  val goodsId: String,
  val terminalStoreId: String? = null,
  val productionDate: String? = null,
  val quantity: Int? = null,
  val barcodes: List<String> = emptyList(),
  val clientRequestId: String? = null
)

data class OutboundLineSubmitRequest(
  val goodsId: String,
  val targetQuantity: Int? = null,
  val barcodes: List<String>
)

data class OutboundSubmitRequest(
  val type: String,
  val sourceWarehouseId: String,
  val targetWarehouseId: String? = null,
  val targetLocationId: String? = null,
  val salespersonId: String? = null,
  val goodsId: String? = null,
  val barcodes: List<String> = emptyList(),
  val lines: List<OutboundLineSubmitRequest>? = null,
  val clientRequestId: String? = null
)

data class SalesReturnSubmitRequest(
  val returnWarehouseId: String,
  val returnLocationId: String,
  val barcodes: List<String>,
  val clientRequestId: String? = null
)

data class PendingSubmission(
  val kind: String,
  val requestId: String,
  val requestJson: String,
  val summary: String,
  val createdAt: Long
)

data class SubmitResult(
  val orderId: String,
  val inboundOrderId: String? = null,
  val quantity: Int? = null,
  val items: List<InventoryItem>
)

data class PdaReleaseInfo(
  val versionCode: Int,
  val versionName: String,
  val apkUrl: String? = null,
  val notes: List<String> = emptyList(),
  val publishedAt: String? = null,
  val forceUpdate: Boolean = false
)
