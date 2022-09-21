package com.libowen.searcher.indexer.model;

import lombok.Data;

//该类用于映射倒排索引表中的数据
@Data
public class InvertedRecord {
    private String word;
    private int docId;
    private int weight;

    public InvertedRecord(String word,int docId,int weight){
        this.word=word;
        this.docId=docId;
        this.weight=weight;
    }
}
