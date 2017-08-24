package cn.et.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.stereotype.Component;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class Search {
	//����Ŀ¼
	public final static String indexDir="F:/myindex/";
	//�ִ���
	static Analyzer analyzer = new IKAnalyzer();
	/**
	 *ʱ�䣺2017-7-3 ����11:33:03
	 *���ߣ� LM
	 *��ϵ��ʽ��973465719@qq.com
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		FSDirectory fsDirectory= FSDirectory.open(new File(indexDir));
		IndexSearcher searcher=new IndexSearcher(DirectoryReader.open(fsDirectory));
		QueryParser qp=new QueryParser(Version.LUCENE_47, "content", analyzer);
		Query query=qp.parse("android");
		TopDocs td=searcher.search(query, 10);
		ScoreDoc[] sd=td.scoreDocs;
		for(ScoreDoc ss:sd){
			Document doc=searcher.doc(ss.doc);
			System.out.println(doc.getField("title")+"--"+doc.getField("url"));
		}
	}

}
