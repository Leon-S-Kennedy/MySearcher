package com.libowen.searcher.indexer.command;

import com.libowen.searcher.indexer.core.IndexManager;
import com.libowen.searcher.indexer.model.Document;
import com.libowen.searcher.indexer.properties.IndexerProperties;
import com.libowen.searcher.indexer.util.FileScanner;
import lombok.extern.slf4j.Slf4j;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 构建索引模块
 */
@Slf4j
@Component
public class Indexer implements CommandLineRunner {
    //读取配置信息
    private final IndexerProperties properties;
    //文件扫描对象
    private final FileScanner fileScanner;
    private final IndexManager indexManager;
    private final ExecutorService executorService;

    @Autowired
    public Indexer(IndexerProperties indexerProperties, FileScanner fileScanner, IndexManager indexManager, ExecutorService executorService) {
        this.properties = indexerProperties;
        this.fileScanner = fileScanner;
        this.indexManager = indexManager;
        this.executorService = executorService;
    }


    @Override
    public void run(String... args) throws Exception {
        log.info("程序的逻辑入口");
        log.debug("开始分词预热");
        ToAnalysis.parse("我是一个孤独的飞行员");
        log.debug("分词预热完成");

        //1.扫描出所有的html文件
        log.debug("开始扫描目录，找到所有的html文件。{}",properties.getDocRootPath());
        List<File> htmlFileList=fileScanner.scanFile(properties.getDocRootPath(),file->{
            return file.isFile()&&file.getName().endsWith(".html");
        });
        log.debug("扫描结束，一共得到文件{}个",htmlFileList.size());

        //2.针对每个html文件，得到其标题，url,正文信息封装到文档对象中
/*
        for (File htmlFile : htmlFileList) {
            new Document(htmlFile,properties.getUrlPrefix(),new File(properties.getDocRootPath()));
        }
*/
        File rootFile = new File(properties.getDocRootPath());
        List<Document> documentList = htmlFileList.stream()
                .parallel()     //添加parallel使得变成并行的
                .map(file -> new Document(file, properties.getUrlPrefix(), rootFile))
                .collect(Collectors.toList());
        log.debug("构建文档成功,共{}个文件",documentList.size());


        //3.进行正排索引的保存
        indexManager.saveForwardIndexesConcurrent(documentList);
        log.debug("正排索引保存完成");
        //4.进行倒排索引的保存
        indexManager.saveInvertedIndexesConcurrent(documentList);
        log.debug("倒排索引保存完成");

        executorService.shutdown();
    }
}
