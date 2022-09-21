package com.libowen.searcher.web;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface SearchMapper {
    List<Document> query(
            @Param("word") String word,
            @Param("limit") int limit,
            @Param("offset")int offset
    );
    List<DocumentWithWeight> queryWithWeight(
            @Param("word") String word,
            @Param("limit") int limit,
            @Param("offset")int offset
    );
}
