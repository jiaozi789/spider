package cn.et.spilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import cn.et.index.LuceneUtils;
/**
 * Csdn的蜘蛛 需要自行分析博客的html语法的特点
  *时间：2017-6-29 上午10:42:47
  *作者： LM
  *联系方式：973465719@qq.com
 *
 */
public class CsdnSpilder {
	//存放下载文件的目录
	public final static  String SAVED_DIR="F:/mycsdn/";
	//CSDN的blog的官网
	public final static  String rootDir="http://blog.csdn.net/";
	//爬取的用户名
	public final static  String userName="liaomin416100569";
	//路徑分隔符
	public final static String separatorChar="/";
	//索引目录
	public final static String indexDir="F:/myindex/";
	//日志处理
	public final static Logger logger=Logger.getLogger(CsdnSpilder.class);
	
	/**
		开始爬虫的入口
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		logger.debug("开始处理:"+rootDir+userName);
		Document doc = getDoc(rootDir+userName);
		//获取所有的分类
		List<Category> category=getCategory(doc);
		logger.debug("获取到类型个数为：:"+category.size());
		for(Category tmCate:category){
			String curl=tmCate.getUrl();
			logger.debug("开始处理文章分类："+tmCate.getName()+",路径："+curl+",文章数："+tmCate.getCount());
			//创建目录
			new File(SAVED_DIR+curl).mkdirs();
			List<Arcticle> arcList=getArcticle(curl);
			logger.debug("开始索引");
			LuceneUtils.indexs(indexDir, arcList);
			logger.debug("处理完成");
		}
		
	}
	/**
	 * 无法获取重试获取Document对象
	 * @return
	 */
	public static Document getDoc(String path){
		Document doc;
		while(true){
			try {
				doc = Jsoup.connect(path).get();
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return doc;
	}
	
	@Test
	public void testArcticle() throws IOException{
		String curl="/liaomin416100569/article/category/650802";
		//创建目录
		new File(SAVED_DIR+curl).mkdirs();
		List<Arcticle> arcList=getArcticle(curl);
		LuceneUtils.indexs(indexDir, arcList);
		System.out.println(arcList.size());
	}
	
	/**
	 * 获取所有分类
	 * @return
	 * @throws IOException
	 */
	public static List<Category> getCategory(Document doc) throws IOException{
		Elements newsHeadlines = doc.select("#panel_Category a");
		String[] categoryArr=newsHeadlines.text().replaceAll(" +", " ").split(" ");
		List<Category> cates=new ArrayList<Category>();
		for(int i=0;i<newsHeadlines.size();i++){
			Element el=newsHeadlines.get(i);
			Category c=new Category();
			c.setName(el.text().trim());
			c.setCount(el.nextElementSibling().text());
			c.setUrl(el.attr("href"));
			cates.add(c);
		}
		return cates;
	}
	/**
	 * 通过分类获取文章 文章过多不建议调用此方法
	 * @param cl
	 * @return
	 * @throws IOException 
	 */
	public static List<Arcticle> getArcticle(String typeUrl) throws IOException{
		String category=rootDir+typeUrl;
		Document doc = getDoc(category);
		//判断是否存在分页
		Elements newsHeadlines = doc.select("#papelist");
		int totalPage=0;
		List<Arcticle> arcs=new ArrayList<Arcticle>();
		//存在分页
		if(newsHeadlines.size()>0){
			totalPage=Integer.parseInt(newsHeadlines.get(0).child(0).text().split("共")[1].split("页")[0]);
			for(int i=1;i<=totalPage;i++){
				arcs.addAll(getArcticle(typeUrl,i));
			}
		}else{
			arcs.addAll(getArcticle(typeUrl,1));
		}
		return arcs;
	}
	/**
	 * 获取分配中某一页的文章
	 * @param typeUrl
	 * @param page
	 * @return
	 * @throws IOException
	 */
	public static List<Arcticle> getArcticle(String typeUrl,int page) throws IOException{
		//獲取當前頁的url地址
		String pageUrl=rootDir+typeUrl+separatorChar+page;
		//获取需要保存的目录
		String destPath=SAVED_DIR+typeUrl+separatorChar+page;
		//提前创建该目录
		new File(destPath).mkdirs();
		//如果如法连接自动重连
		Document doc = getDoc(pageUrl);
		//获取到所有文章的标题标签
		Elements docs = doc.select(".link_title");
		List<Arcticle> arcs=new ArrayList<Arcticle>();
		for(int i=0;i<docs.size();i++){
			Element el=docs.get(i);
			Element ael=el.child(0);
			//获取标题上超链接的地址
			String url=ael.attr("href");
			String title=ael.text();
			Arcticle a=new Arcticle();
			a.setUrl(url);
			a.setTitle(title);
			//获取描述的标签
			Element descElement=el.parent().parent().nextElementSibling();
			String description=descElement.text();
			a.setDescription(description);
			//获取最后更新时间的标签
			Element timeElement=descElement.nextElementSibling();
			String createTime=timeElement.select(".link_postdate").text();
			a.setCreateTime(createTime);
			logger.debug(title+url+description+createTime);
			arcs.add(a);
			//保存到文件中
			String filePath=saveFile(destPath,rootDir+url,a);
			a.setFilePath(filePath);
		}
		return arcs;
	}
	/**
	 * 保存html内容到文件中
	 * @param destDir 需要保存的目標目錄
	 * @param htmlUrl 抓取的htmlurl
	 * @throws IOException 
	 */
	public static String saveFile(String destDir,String htmlUrl,Arcticle arc) throws IOException{
		Document doc = getDoc(htmlUrl);
		String fileName=htmlUrl.substring(htmlUrl.lastIndexOf("/")+1);
		String file=destDir+separatorChar+fileName+".html";
		File hfile=new File(file);
		boolean ifUpdate=true;
		//文件存在需要判断文件是否存在更新 
		if(hfile.exists()){
			Properties p=new Properties();
			p.load(new FileInputStream(destDir+separatorChar+fileName+".ini"));
			String createTime=p.getProperty("createTime");
			//之前的文章创建时间 小于 网上爬取文章 时间 所以文章被修改过 需要更新
			if(createTime.compareTo(arc.getCreateTime())<0){
				hfile.delete();
				ifUpdate=true;
			}else{
				ifUpdate=false;
			}
		}
		if(ifUpdate){
			//写入文件 并将信息写入资源文件
			Properties p=new Properties();
			p.setProperty("title", arc.getTitle());
			p.setProperty("url", arc.getUrl());
			p.setProperty("description", arc.getDescription());
			p.setProperty("createTime", arc.getCreateTime());
			p.store(new FileOutputStream(destDir+separatorChar+fileName+".ini"),htmlUrl);
			FileUtils.writeStringToFile(hfile, doc.toString(),"UTF-8");
		}
		return file;
	}
}
