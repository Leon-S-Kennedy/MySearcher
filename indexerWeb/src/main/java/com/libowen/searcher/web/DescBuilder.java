package com.libowen.searcher.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DescBuilder {
    public Document build(List<String> queryList, Document doc){
        String content = doc.getContent().toLowerCase();
        String word="";
        int i=-1;
        for (String query : queryList) {
            i = content.indexOf(query);
            if(i!=-1){
                word=query;
                break;
            }
        }
        if(i==-1){
            log.debug("docId={}中不包含{}",doc.getDocId(),queryList);
            throw new RuntimeException();
        }

        int from= Math.max(i - 120, 0);
        int to= Math.min(i + 120, content.length());
        String desc=content.substring(from,to);
        desc=desc.replace(word,"<i>"+word+"</i>");
        doc.setDesc(desc);
        return doc;
    }
}
