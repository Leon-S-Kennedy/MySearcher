package com.libowen.searcher.web;

import lombok.Data;

@Data
public class DocumentWithWeight {
    private int docId;
    private String title;
    private String url;
    private String content;
    public int weight;
    public DocumentWithWeight(){}
    public DocumentWithWeight(DocumentWithWeight documentWithWeight) {
        this.docId=documentWithWeight.docId;
        this.title=documentWithWeight.title;
        this.url=documentWithWeight.url;
        this.content=documentWithWeight.content;
        this.weight=documentWithWeight.weight;
    }
    public Document toDocument(){
        Document document=new Document();
        document.setDocId(this.docId);
        document.setTitle(this.title);
        document.setUrl(this.url);
        document.setContent(this.content);

        return document;
    }
}
