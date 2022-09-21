package com.libowen.searcher.indexer;

import com.libowen.searcher.indexer.command.Indexer;
import com.libowen.searcher.indexer.mapper.IndexDatabaseMapper;
import com.libowen.searcher.indexer.model.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class IndexerApplicationTests {
    @MockBean   //通过这个注解,不需要真正的执行Indexer Bean下的操作
    private Indexer indexer;
    @Autowired
    private IndexDatabaseMapper mapper;

    @Test
    void batchInsert() {
        List<Document> list=new ArrayList<>();
        for (int i=0;i<10;i++){
            String s = String.valueOf(i);
            Document document = new Document(s, s, s);
            list.add(document);
        }
        mapper.batchInsertForwardIndexes(list);
    }

}
