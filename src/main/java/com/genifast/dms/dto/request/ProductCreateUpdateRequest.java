package com.genifast.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ProductCreateUpdateRequest {
    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotBlank(message = "Product type is required")
    private String type;

    @NotNull(message = "Price is required")
    @PositiveOrZero
    private Long price;

    @NotNull(message = "Quantity is required")
    @PositiveOrZero
    private Long quantity;
}
