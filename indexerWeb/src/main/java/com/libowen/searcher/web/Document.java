package com.libowen.searcher.web;

import lombok.Data;

@Data
public class Document {
    private Integer docId;
    private String title;
    private String url;
    private String content;
    private String desc;
}
