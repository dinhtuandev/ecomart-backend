package com.ecomart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPageResponse {

    private Long id;
    private String slug;
    private String title;
    private String content;
    private LocalDateTime updatedAt;
}
