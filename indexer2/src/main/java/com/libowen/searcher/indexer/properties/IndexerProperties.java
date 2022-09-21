package com.libowen.searcher.indexer.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("searcher.indexer")
@Data
public class IndexerProperties {
    private String docRootPath;
    private String urlPrefix;
    private String indexRootPath;
}
