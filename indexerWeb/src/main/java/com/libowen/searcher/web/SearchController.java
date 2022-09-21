package com.libowen.searcher.web;

import lombok.extern.slf4j.Slf4j;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Controller
public class SearchController {
    private final SearchMapper mapper;
    private final DescBuilder desc;

    @Autowired
    public SearchController(SearchMapper mapper, DescBuilder desc) {
        this.desc = desc;
        ToAnalysis.parse("我是一个孤独的飞行员");
        this.mapper = mapper;
    }

    @GetMapping("/web")
    public String search(String query, @RequestParam(value = "page",required = false) String pageString, Model model){
        log.debug("查询query={}",query);
        if(query==null){
            log.debug("query为null,重定向到首页");
            return "redirect:/";
        }
        query = query.trim().toLowerCase();
        if(query.isEmpty()){
            log.debug("query为空，重定向到首页");
            return "redirect:/";
        }
        List<String> queryList = ToAnalysis.parse(query)
                .getTerms()
                .stream()
                .map(Term::getName)
                .collect(Collectors.toList());
        if(queryList.isEmpty()){
            log.debug("query分词后为空，重定向到首页");
            return "redirect:/";
        }

        log.debug("查询的词：{}",query);
        int limit=20;
        int offset=0;
        int page=1;

        if(pageString!=null){
            pageString=pageString.trim();
            try {
                page=Integer.parseInt(pageString);
                if(page<=0){
                    page=1;
                }
                limit=page*20;
            }catch (NumberFormatException exception){

            }
        }
        List<DocumentWithWeight> totalList=new ArrayList<>();
        for (String word : queryList) {
            List<DocumentWithWeight> documentList=mapper.queryWithWeight(word,limit,offset);
            totalList.addAll(documentList);
        }
        //针对所有词的查询结果进行聚合
        Map<Integer,DocumentWithWeight>  documentMap=new HashMap<>();
        for (DocumentWithWeight documentWithWeight : totalList) {
            int docId = documentWithWeight.getDocId();
            if(documentMap.containsKey(docId)){
                DocumentWithWeight item=documentMap.get(docId);
                item.weight+=documentWithWeight.weight;
                continue;
            }
            DocumentWithWeight item=new DocumentWithWeight(documentWithWeight);
            documentMap.put(docId,item);
        }
        Collection<DocumentWithWeight> values = documentMap.values();
        List<DocumentWithWeight> list=new ArrayList<>(values);
        Collections.sort(list,(item1,item2)->{
            return item2.weight-item1.weight;
        });

        int from=(page-1)*20;
        int to=from+20;
        List<DocumentWithWeight> subList = list.subList(from, to);
        List<Document> documentList = subList
                .stream()
                .map(DocumentWithWeight::toDocument)
                .collect(Collectors.toList())   ;

        //此时已经获得查询列表
        List<String> wordList=queryList;
        documentList = documentList
                .stream()
                .map(doc -> desc.build(queryList, doc))
                .collect(Collectors.toList());

        model.addAttribute("query",query);
        model.addAttribute("docList",documentList);
        model.addAttribute("page",page);
        model.addAttribute("desc",desc);

        return "search";
    }
}
