package cn.et.index;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.wltea.analyzer.lucene.IKAnalyzer;

import cn.et.spilder.Arcticle;
import cn.et.spilder.CsdnSpilder;
/**
 * lucene操作类
  *时间：2017-6-30 上午09:14:48
  *作者： LM
  *联系方式：973465719@qq.com
 *
 */
public class LuceneUtils {
	//定义IK分词器
	static Analyzer analyzer = new IKAnalyzer();
	//批量处理内存存储的最大docuemnt个数
	final static int dealCount=50;
	//html格式化器 在关键字前后加上红色字体标签
	static SimpleHTMLFormatter htmlFormatter=new SimpleHTMLFormatter("<font color=\"red\">","</font>");
	/**
	 * 关闭writer对象
	 * @param iw
	 * @throws IOException
	 */
	public static void close(IndexWriter iw) throws IOException{
		iw.close();
	}
	public static IndexWriter getRamWriter(RAMDirectory ramDirectory) throws IOException{
		IndexWriterConfig iwc=new IndexWriterConfig(Version.LUCENE_47,analyzer);
		IndexWriter iw=new IndexWriter(ramDirectory, iwc);
		return iw;
	}
	public static IndexWriter getWriter(String dir) throws IOException{
//		FSDirectory fsDirectory= FSDirectory.open(Paths.get(dir));
		FSDirectory fsDirectory= FSDirectory.open(new File(dir));
		IndexWriterConfig iwc=new IndexWriterConfig(Version.LUCENE_47,analyzer);
		IndexWriter iw=new IndexWriter(fsDirectory, iwc);
		return iw;
	}
	public static List<Arcticle> search(String indexDir,String keys) throws Exception{
		FSDirectory fsDirectory= FSDirectory.open(new File(indexDir));
		IndexSearcher searcher=new IndexSearcher(DirectoryReader.open(fsDirectory));
		MultiFieldQueryParser qp=new MultiFieldQueryParser(Version.LUCENE_47, new String[]{"title","content"}, analyzer);
		Query query=qp.parse(keys);
		//初始化高亮器
		Highlighter high=new Highlighter(htmlFormatter, new QueryScorer(query));
		TopDocs td=searcher.search(query, 10);
		ScoreDoc[] sd=td.scoreDocs;
		List<Arcticle> arcList=new ArrayList<Arcticle>();
		for(ScoreDoc ss:sd){
			Document doc=searcher.doc(ss.doc);
			Arcticle arc=new Arcticle();
			arc.setTitle(doc.getField("title").stringValue());
			TokenStream tokenStream = analyzer.tokenStream("desc", new StringReader(doc.get("desc")));  
			String str = high.getBestFragment(tokenStream,  doc.get("desc"));
			arc.setDescription(str);
			arc.setUrl(CsdnSpilder.rootDir+doc.getField("url").stringValue());
			arc.setCreateTime(doc.getField("createTime").stringValue());
			arcList.add(arc);
		}
		return arcList;
	}
	/**
	 * 优化方式 先将索引写入内存 到一定量后写入磁盘
	 * @param dir
	 * @param arc
	 * @throws IOException
	 */
	public static void indexs(String dir,List<Arcticle> arc) throws IOException{
		RAMDirectory ramDirectory=new RAMDirectory();
		IndexWriter ramWriter=getRamWriter(ramDirectory);
		new File(dir).mkdirs();
		//索引文件存在于目录中 IndexWriter只是个写入对象
		IndexWriter iw=getWriter(dir);
		for(int i=1;i<=arc.size();i++){
			Arcticle srcTmp=arc.get(i-1);
			if(i%dealCount==0 || i==arc.size()){
				//必须关闭writer才能将目录索引数据写入到其他的writer中
				ramWriter.commit();
				ramWriter.close();
				iw.addIndexes(ramDirectory);
				if(i<arc.size())
					ramWriter=getRamWriter(ramDirectory);
			}else{
				index(ramWriter,srcTmp);
			}
		}
		iw.commit();
		iw.close();
	}
	/**
	 * 直接写入磁盘
	 * @param writer
	 * @param arc
	 * @return
	 * @throws IOException
	 */
	public static Document index(IndexWriter writer,Arcticle arc) throws IOException{
		Document doc=new Document();
		doc.add(new TextField("title", arc.getTitle(), Store.YES));
		doc.add(new TextField("url", arc.getUrl(), Store.YES));
		doc.add(new TextField("createTime", arc.getCreateTime(), Store.YES));
		doc.add(new TextField("desc", arc.getDescription(), Store.YES));
		doc.add(new TextField("content", Jsoup.parse(FileUtils.readFileToString(new File(arc.getFilePath()))).text(), Store.YES));
		writer.addDocument(doc);
		return doc;
	}
}
