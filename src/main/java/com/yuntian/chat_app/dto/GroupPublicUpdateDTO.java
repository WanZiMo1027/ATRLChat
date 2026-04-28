package com.yuntian.chat_app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GroupPublicUpdateDTO {
    @JsonProperty("isPublic")
    private Boolean publicVisible;
}
