package com.libowen.searcher.indexer.model;

import lombok.Data;
import lombok.SneakyThrows;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class Document {
    private Integer docId;
    private String title;
    private String url;
    private String content;

    //专门给测试留下来的构造方法
    public Document(String title,String url,String content){
        this.title=title;
        this.url=url;
        this.content=content;
    }
    private final static HashSet<String> ignoredWordSet=new HashSet<>();
    static {
        ignoredWordSet.add(" ");
        ignoredWordSet.add("\t");
        ignoredWordSet.add(".");
        ignoredWordSet.add(",");
        ignoredWordSet.add("。");
        ignoredWordSet.add("，");
        ignoredWordSet.add("(");
        ignoredWordSet.add(")");
        ignoredWordSet.add("-");
        ignoredWordSet.add(";");
        ignoredWordSet.add("/");
        ignoredWordSet.add("&");
    }

    //针对文档进行分词，并计算每个此的权重
    public Map<String,Integer> segWordAndCalcWeight(){
        //对标题进行分词
        List<String> wordInTitle = ToAnalysis.parse(title)
                .getTerms()
                .stream()
                .parallel()     //通过parallel进行优化
                .map(Term::getName)
                .filter(s -> !ignoredWordSet.contains(s))
                .collect(Collectors.toList());
        //统计标题中每个词出现的次数
        Map<String,Integer> titleWordCount=new HashMap<>();
        for (String word : wordInTitle) {
            int count=titleWordCount.getOrDefault(word,0);
            titleWordCount.put(word,count+1);
        }

        //对正文进行分词
        List<String> wordInContent = ToAnalysis.parse(content)
                .getTerms()
                .stream()
                .parallel()     //通过parallel进行优化
                .map(Term::getName)
                .filter(s -> !ignoredWordSet.contains(s))
                .collect(Collectors.toList());
        //统计正文中每个词出现的次数
        Map<String,Integer> contentWordCount=new HashMap<>();
        for (String word : wordInContent) {
            int count=contentWordCount.getOrDefault(word,0);
            contentWordCount.put(word,count+1);
        }

        Map<String,Integer> wordToWeight=new HashMap<>();
        Set<String> wordSet=new HashSet<>(wordInTitle);
        wordSet.addAll(wordInContent);
        for (String word : wordSet) {
            Integer titleCount = titleWordCount.getOrDefault(word, 0);
            Integer contentCount = contentWordCount.getOrDefault(word, 0);
            wordToWeight.put(word,titleCount*10+contentCount);
        }
        return wordToWeight;
    }

    public Document(File file , String urlPrefix, File rootFile){
        this.title=parseTitle(file);
        this.url=parseUrl(file,urlPrefix,rootFile);
        this.content=parseContent(file);
    }

    @SneakyThrows
    private String parseContent(File file) {
        StringBuilder sb=new StringBuilder();
        try(FileInputStream is = new FileInputStream(file)){
            try(Scanner scanner = new Scanner(is,"ISO-8859-1")) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    sb.append(line).append(" ");
                }
                return sb.toString()
                        .replaceAll("<script.*?>.*?</script>", " ")
                        .replaceAll("<.*?>"," ")
                        .replaceAll("\\s+"," ")
                        .trim();
            }
        }
    }

/*
    @SneakyThrows
    private String parseContent(File file) {
        StringBuilder sb=new StringBuilder();
        try(FileInputStream is = new FileInputStream(file)){
            try(Scanner scanner = new Scanner(is,"ISO-8859-1")){
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    //使用正则表达式去掉标签
                    line = line.replaceAll("<script[^>]*>[^<]*</script>", " ");
                    line=line.replaceAll("<[^>]*>"," ");
                    sb.append(line).append(" ");

                }
            }
        }
        return sb.toString().replaceAll("\\s+"," ").trim();
    }
*/

    @SneakyThrows
    private String parseUrl(File file, String urlPrefix, File rootFile) {
        String rootPath = rootFile.getCanonicalPath();
        rootPath=rootPath.replace("/","\\");
        if (!rootPath.endsWith("\\")){
            rootPath=rootPath+"\\";
        }
        String filePath = file.getCanonicalPath();
        String relativePath = filePath.substring(rootPath.length());
        relativePath = relativePath.replace("\\", "/");

        return urlPrefix+relativePath;

    }

    private String parseTitle(File file) {
        //从文件名中解析
        String name = file.getName();
        return name.substring(0,name.length()-".html".length());
    }

}
