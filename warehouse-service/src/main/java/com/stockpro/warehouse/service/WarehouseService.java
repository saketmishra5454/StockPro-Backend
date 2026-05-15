package com.stockpro.warehouse.service;

import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;

import java.util.List;

public interface WarehouseService {

    Warehouse createWarehouse(Warehouse warehouse);

    Warehouse getWarehouseById(int warehouseId);

    List<Warehouse> getAllWarehouses();

    Warehouse updateWarehouse(int warehouseId, Warehouse updated);

    void deactivateWarehouse(int warehouseId);

    void activateWarehouse(int warehouseId);

    StockLevel getStockLevel(int warehouseId, int productId);


    StockLevel updateStock(int warehouseId, int productId, int delta);


    StockLevel reserveStock(int warehouseId, int productId, int quantity);


    StockLevel releaseReservation(int warehouseId, int productId, int quantity);


    void transferStock(int fromWarehouseId, int toWarehouseId, int productId, int quantity);

    List<StockLevel> getLowStockItems();

    List<StockLevel> getStockByWarehouse(int warehouseId);

    List<StockLevel> getAllStockLevels();

    StockLevel initializeStock(int warehouseId, int productId, int initialQuantity);
}
