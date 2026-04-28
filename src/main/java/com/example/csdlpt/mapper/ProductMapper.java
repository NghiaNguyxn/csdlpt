package com.example.csdlpt.mapper;

import com.example.csdlpt.dto.request.ProductRequest;
import com.example.csdlpt.dto.response.ProductBasicResponse;
import com.example.csdlpt.dto.response.ProductResponse;
import com.example.csdlpt.entity.ProductBasic;
import com.example.csdlpt.entity.ProductDetail;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    ProductBasic toProductBasic(ProductRequest request);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductDetail toProductDetail(ProductRequest request);

    @Mapping(target = "id", source = "basic.id")
    @Mapping(target = "categoryId", source = "basic.category.id")
    ProductResponse toResponse(ProductBasic basic, ProductDetail detail);

    @Mapping(target = "categoryId", source = "category.id")
    ProductBasicResponse toBasicResponse(ProductBasic basic);

    List<ProductBasicResponse> toBasicResponses(List<ProductBasic> basics);

}
