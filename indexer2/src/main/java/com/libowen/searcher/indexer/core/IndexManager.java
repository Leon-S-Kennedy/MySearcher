package com.libowen.searcher.indexer.core;

import com.libowen.searcher.indexer.aop.Timing;
import com.libowen.searcher.indexer.mapper.IndexDatabaseMapper;
import com.libowen.searcher.indexer.model.Document;
import com.libowen.searcher.indexer.model.InvertedRecord;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class IndexManager {
    private final IndexDatabaseMapper mapper;
    private final ExecutorService executorService;
    @Autowired
    public IndexManager(IndexDatabaseMapper mapper, ExecutorService executorService) {
        this.mapper = mapper;
        this.executorService = executorService;
    }

    //先批量生成保存正排索引
    public void saveForwardIndexes(List<Document> documentList){
        //1.批量插入时每次插入10条
        int batchSize=10;
        //一共需要执行多少回
        int listSize=documentList.size();
        int times=(int)Math.ceil(1.0*listSize/batchSize);
        log.debug("一共需要{}批",times);
        //开始分批插入
        //int complete=0;
        for (int i = 0; i < listSize; i+=batchSize) {
            int from=i;
            int to=Integer.min(from+batchSize,listSize);
            //log.debug("本批次要插入的文档: from={},to={}",from,to);
            List<Document> subList = documentList.subList(from, to);
            //针对subList做插入
            mapper.batchInsertForwardIndexes(subList);
/*
            complete+=(to-from);
            log.debug("已经插入了{}个文档",complete);
*/

        }
    }
    @Timing("创建+保存正排索引")
    @SneakyThrows
    public void saveForwardIndexesConcurrent(List<Document> documentList) {
        //1.批量插入时每次插入10条
        int batchSize = 10;
        //一共需要执行多少回
        int listSize = documentList.size();
        int times = (int) Math.ceil(1.0 * listSize / batchSize);
        log.debug("一共需要{}批", times);
        //开始分批插入
        //int complete=0;
        CountDownLatch latch=new CountDownLatch(times);
        for (int i = 0; i < listSize; i += batchSize) {
            int from = i;
            int to = Integer.min(from + batchSize, listSize);
            Runnable task=()->{
                //log.debug("本批次要插入的文档: from={},to={}",from,to);
                List<Document> subList = documentList.subList(from, to);
                //针对subList做插入
                mapper.batchInsertForwardIndexes(subList);
                latch.countDown();
            };
            executorService.submit(task);
        }
        latch.await();
    }
    @SneakyThrows
    public void saveInvertedIndexes(List<Document> documentList){
        int batchSize=10000;
        List<InvertedRecord> recordList=new ArrayList<>();
        for (Document document : documentList) {
            Map<String, Integer> wordToWeight = document.segWordAndCalcWeight();
            for (Map.Entry<String, Integer> entry : wordToWeight.entrySet()) {
                String word=entry.getKey();
                int docId=document.getDocId();
                int weight= entry.getValue();

                InvertedRecord record=new InvertedRecord(word,docId,weight);
                recordList.add(record);

                if(recordList.size()==batchSize){
                    mapper.batchInsertInvertedIndexes(recordList);      //批量插入
                    recordList.clear();                                 //清空recordList
                }
            }
        }
        //recordList可能还没有空,还要执行一次插入
        if(!recordList.isEmpty()){
            mapper.batchInsertInvertedIndexes(recordList);
            recordList.clear();
        }
    }

    static class InvertedInsertTask implements Runnable{
        private final CountDownLatch latch;
        private final int batchSize;
        private final List<Document> documentList;
        private final IndexDatabaseMapper mapper;

        InvertedInsertTask(CountDownLatch latch,int batchSize, List<Document> documentList, IndexDatabaseMapper mapper) {
            this.latch=latch;
            this.batchSize = batchSize;
            this.documentList = documentList;
            this.mapper = mapper;
        }

        @Override
        public void run() {
            List<InvertedRecord> recordList=new ArrayList<>();
            for (Document document : documentList) {
                Map<String, Integer> wordToWeight = document.segWordAndCalcWeight();
                for (Map.Entry<String, Integer> entry : wordToWeight.entrySet()) {
                    String word=entry.getKey();
                    int docId=document.getDocId();
                    int weight= entry.getValue();

                    InvertedRecord record=new InvertedRecord(word,docId,weight);
                    recordList.add(record);

                    if(recordList.size()==batchSize){
                        mapper.batchInsertInvertedIndexes(recordList);      //批量插入
                        recordList.clear();                                 //清空recordList
                    }
                }
            }
            //recordList可能还没有空,还要执行一次插入
            if(!recordList.isEmpty()){
                mapper.batchInsertInvertedIndexes(recordList);
                recordList.clear();
                latch.countDown();
            }
        }
    }

    @Timing("创建+保存倒排索引")
    @SneakyThrows
    public void saveInvertedIndexesConcurrent(List<Document> documentList){
        int batchSize=10000;
        int groupSize=50;
        int listSize=documentList.size();
        int times=(int)Math.ceil(listSize*1.0/groupSize);
        CountDownLatch latch=new CountDownLatch(times);
        List<InvertedRecord> recordList=new ArrayList<>();
        for (int i=0;i<listSize;i+=groupSize) {
            int from=i;
            int to=Integer.min(from+groupSize,listSize);
            List<Document> subList = documentList.subList(from, to);
            Runnable task=new InvertedInsertTask(latch,batchSize,subList,mapper);
            executorService.submit(task);

        }
        latch.await();
    }

}
