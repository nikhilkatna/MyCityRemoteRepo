package com.mycity.shared.filterdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterDTO {

    private Long filterId;
    private String filterName;
    private String filterType;
    private int filterValue;
}

