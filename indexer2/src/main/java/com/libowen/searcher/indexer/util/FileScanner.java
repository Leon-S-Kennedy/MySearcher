package com.libowen.searcher.indexer.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileScanner {
    public List<File> scanFile(String rootPath, FileFilter filter){
        List<File> resultList=new ArrayList<>();
        //此处采用深度优先遍历整个根目录
        File rootFile = new File(rootPath);
        traversal(rootFile,filter,resultList);

        return resultList;
    }

    private void traversal(File directoryFile, FileFilter filter, List<File> resultList) {
        //1.通过目录，得到该目录下的孩子文件有哪些
        File[] files = directoryFile.listFiles();
        if(files==null){
            return;
        }
        //2.遍历每个文件，检查是否符合条件
        for (File file : files) {
            if(filter.accept(file)){
                //说明符合条件，需要把该文件加入到Listz中
                resultList.add(file);
            }
        }
        //3.针对目录情况继续遍历
        for (File file : files) {
            if (file.isDirectory()){
                traversal(file,filter,resultList);
            }
        }
    }
}
