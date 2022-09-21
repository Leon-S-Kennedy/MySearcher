package com.libowen.searcher.indexer.mapper;

import com.libowen.searcher.indexer.model.Document;
import com.libowen.searcher.indexer.model.InvertedRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository //注册springBean
@Mapper     //是一个Mybatis管理的Mapper
public interface IndexDatabaseMapper {
    void batchInsertForwardIndexes(@Param("list")List<Document> documentList);

    void batchInsertInvertedIndexes(@Param("list")List<InvertedRecord> recordList);
}
