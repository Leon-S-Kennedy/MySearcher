# 文档搜索引擎
## 前言
这是在学习SSM框架和SpringBoot以后练习的项目，独立完成一个文档搜索引擎，根据用户输入的关键词来查询对应的文档，将结果按照一定的顺序展示给用户。  
具体实现由两个部分组成，分别是**索引构建模块**以及**搜索模块**。前一部分负责构建搜索需要的正排索引以及倒排索引，并将索引持久化到数据库中。后一部分负责将用户输入的关键词根据构建的索引进行查找，将信息展示给用户。
## 开发环境
集成开发环境：IDEA  
项目启动：SpringBoot  
项目搭建：Maven  
相关软件包：MySQL、jackson、ansj_seg、Lombok、Mybatis等
## 开发过程
**基本思路**  
我们应该如何实现将一个带查找的关键词和大量的文档进行匹配？直接靠MySQL的模糊查找么？显然是行不通的，那可能一次查找酒的十天半个月的。
本项目采用的方法是**倒排索引**，也就是提前构建有关一个个词的索引。也就是说，需要一张关键词对应文档信息的表，这样我们去查找关键词的时候就要快得多，因为复杂度降到了常数级。  
那么如何构建倒排索引呢？我们需要获取本地文档的信息，然后采用**分词技术**将文档中的词给提取出来，因此我们还需要每个文档的信息，也就是说为了构建倒排索引，我们还需要有关所有文档信息的**正排索引**，
至于正排索引，我们就只需要遍历本地目录，采取一定的文件操作以及数据处理操作来读取文件信息即可。  
当正排索引和倒排索引构建完毕以后，接下来只需要完成搜索部分，也就是将用户输入的关键词在倒排索引和正排索引中查找出来，然后用前端页面进行展示就完成了。
![图片1](D:\文档\jb\基本流程.jpg)
### 索引构建模块
**遍历本地文档目录**  
采用**深度优先**的方法来遍历目标目录，用一个FileFilter来对目录下的文件进行过滤，我们只要后缀名为.html的文件。
```java
public List<File> scanFile(String rootPath, FileFilter filter){
    List<File> resultList=new ArrayList<>();
    File rootFile = new File(rootPath);
    traversal(rootFile,filter,resultList);
    return resultList;
}
```
**将文档信息封装到类中**  
我们已经得到了所有文档的File对象，还需要将其转变为Document对象。Document类的成员变量为**docId、title、url、content**，也就是文档信息，title信息可以用文件名表示，url可以通过字符串拼接，关键在于如何从html文件获得文档的正文？
也就是说，我们如何从html源码中去掉相关的标签得到文档内容信息。我们通过**正则表达式**来对文档信息进行”清洗“。  
```java
List<Document> documentList = htmlFileList.stream()
    .parallel()     //添加parallel使得变成并行的
    .map(file -> new Document(file, properties.getUrlPrefix(), rootFile))
    .collect(Collectors.toList());
```
**构建并持久化正排索引**  
需要将正排索引保存到MySQL数据库中，也就是将Document对象中的信息插入到数据库中。  
**构建并持久化倒排索引**  
首先我们得知道我们需要什么样的倒排索引，倒排索引中除了主键外应该包含**分割出来的词(word)、文档的编号(docid)、该词在文档中的权重(weight)**。  
于是我们应该遍历得到的所有文档，然后对每个文档进行**分词**，然后分别计算出每个**词（word）** 在该文档中的 **权重(weight)**。然后再将以上信息持久化到数据库中，倒排索引就完成了。  
___
**构建索引模块的改进过程**  
1. 刚开始的时候，将正排索引和倒排索引插入到数据库表中的时候，是用一条sql语句完成的。然而我们发现，文档的数量较大的时候，通过遍历的方式一条一条的插入**效率相当低下**。正排索引大概需要插入**1W+** 条记录，还勉强可以接受，但是倒排索引大概需要插入**200W+** 的记录，采用循环的方式插入几乎需要**好几个小时**才能完成插入数据。  
2. 为了改变这种情况，采用了**批量插入**的方法。具体的做法是采用Mybatis的xml配置文件形式来实现批量插入。
```xml
    <insert id="batchInsertForwardIndexes" useGeneratedKeys="true" keyProperty="docId" keyColumn="docid">
        insert into forward_indexes (title,url,content) values
        <foreach collection="list" item="doc" separator=", ">
            (#{doc.title},#{doc.url},#{doc.content})
        </foreach>
    </insert>
```
3. 经过2的方法以后，插入的时间大大减少，由以前的**数个小时**变成现在的**1分多钟**。为了更快的完成索引的持久化，在2的基础上进一步的采用多线程的方式来实现。将索引持久化的过程压缩到**1分钟内**。
4. 由于以上的方法需要记录持久化索引需要的时间，于是利用**AOP**的思想，添加了一个计算方法的执行过程耗时的功能。
![图片2](D:\文档\jb\时间.jpg)
___
**当正排索引和倒排索引都构建完毕之后，索引构建模块就完成了，接下来就是完成搜索模块**
###搜索模块
该模块为一个Web模块，我们需要接收用户输入的**待查询信息**,然后针对待查询信息进行分词处理，然后在数据库中查询出相关的信息，然后在前端页面中进行渲染，该部分需要加入**分页功能**，这样方便用户进行浏览。  
首先，我们要对用户输入的信息进行检查，如果输入的信息为空或者经过分词后为空，就重定向到首页。当用户的输入合法的时候我们就要对用户的输入进行查询。
同样采用Mybatis的xml方式来进行查询。
```xml
    <resultMap id="DocumentResultMap" type="com.libowen.searcher.web.Document">
        <id property="docId" column="docid"/>
        <result property="title" column="title"/>
        <result property="url" column="url"/>
        <result property="content" column="content"/>
    </resultMap>
    <select id="query" resultMap="DocumentResultMap">
        select ii.docid,title,url,content
        from inverted_indexes ii
        join forward_indexes fi
        on ii.docid=fi.docid
        where word=#{word}
        order by weight desc
        limit ${limit}
        offset ${offset}
    </select>
```
可以看出我们需要查询的信息为**docid,title,url,content**.我们将这些信息通过模板的方式渲染到前端页面中就完成了。对于关键词的搜索。
___
**搜索模块的改进**
1. 我们发现直接进行查询的时候，查询效率非常低，因为倒排索引有200W+的记录，正排索引有1W+的记录，两个表联合查找起来效率非常的低，于是我们给表添加了索引，这样查找的时候速度有了很大的提升，由**2秒左右**缩减到只需要**0.1秒不到**。
```mysql
create index word_weight_index
    on inverted_indexes (word asc, weight desc);
```
2. 当用户的输入可以被分成多个词的时候，如何查询以及对查询结果进行聚合？当用户输入的待查询词经过分词以后，得到查询词列表，对每个词进行查询，我们都能得到limit*词的个数的记录，我们如何对这些结果进行聚合然后分页显示？  
我们需要查询出权重(weight)然后将这些结果进行重新聚合，那么问题来了，怎么聚合呢？**我们很自然的想到，将所有词的查询结果中docid相同的记录中的weight加起来**。
但是这样的话，在分页的时候我们取第一页的时候是每个词的前limit个进行聚合，第page页的时候取的是每个词的 **第(page-1)×limit个到page×limit个** 进行聚合，这样的结果显然是不正确的。我们该如何聚合以及分页？
3. 以上的方法要想保证正确性的话，我们再分页查找的时候只能每个词都查找**0到page * limit**个，然后再手动进行docid相同的进行权重的叠加，然后取出指定的页数的数据，这样就完成了多个词的搜索问题。
___
### 结果展示
![index](D:\文档\jb\index.jpg)
![index2](D:\文档\jb\index2.jpg)
![HashMap](D:\文档\jb\HashMap.jpg)
