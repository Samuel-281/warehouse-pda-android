package com.warehouse.pda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.warehouse.pda.data.WarehouseRepository

class MainActivity : ComponentActivity() {
  private val viewModel by viewModels<MainViewModel> {
    MainViewModel.factory(WarehouseRepository(applicationContext))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      WarehousePdaRoot(viewModel = viewModel)
    }
  }
}
