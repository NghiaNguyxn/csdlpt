package com.example.csdlpt.mapper;

import com.example.csdlpt.dto.response.InventoryResponse;
import com.example.csdlpt.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    InventoryResponse toResponse(Inventory inventory);
}
