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
 * Csdn��֩�� ��Ҫ���з������͵�html�﷨���ص�
  *ʱ�䣺2017-6-29 ����10:42:47
  *���ߣ� LM
  *��ϵ��ʽ��973465719@qq.com
 *
 */
public class CsdnSpilder {
	//��������ļ���Ŀ¼
	public final static  String SAVED_DIR="F:/mycsdn/";
	//CSDN��blog�Ĺ���
	public final static  String rootDir="http://blog.csdn.net/";
	//��ȡ���û���
	public final static  String userName="liaomin416100569";
	//·���ָ���
	public final static String separatorChar="/";
	//����Ŀ¼
	public final static String indexDir="F:/myindex/";
	//��־����
	public final static Logger logger=Logger.getLogger(CsdnSpilder.class);
	
	/**
		��ʼ��������
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		logger.debug("��ʼ����:"+rootDir+userName);
		Document doc = getDoc(rootDir+userName);
		//��ȡ���еķ���
		List<Category> category=getCategory(doc);
		logger.debug("��ȡ�����͸���Ϊ��:"+category.size());
		for(Category tmCate:category){
			String curl=tmCate.getUrl();
			logger.debug("��ʼ�������·��ࣺ"+tmCate.getName()+",·����"+curl+",��������"+tmCate.getCount());
			//����Ŀ¼
			new File(SAVED_DIR+curl).mkdirs();
			List<Arcticle> arcList=getArcticle(curl);
			logger.debug("��ʼ����");
			LuceneUtils.indexs(indexDir, arcList);
			logger.debug("�������");
		}
		
	}
	/**
	 * �޷���ȡ���Ի�ȡDocument����
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
		//����Ŀ¼
		new File(SAVED_DIR+curl).mkdirs();
		List<Arcticle> arcList=getArcticle(curl);
		LuceneUtils.indexs(indexDir, arcList);
		System.out.println(arcList.size());
	}
	
	/**
	 * ��ȡ���з���
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
	 * ͨ�������ȡ���� ���¹��಻������ô˷���
	 * @param cl
	 * @return
	 * @throws IOException 
	 */
	public static List<Arcticle> getArcticle(String typeUrl) throws IOException{
		String category=rootDir+typeUrl;
		Document doc = getDoc(category);
		//�ж��Ƿ���ڷ�ҳ
		Elements newsHeadlines = doc.select("#papelist");
		int totalPage=0;
		List<Arcticle> arcs=new ArrayList<Arcticle>();
		//���ڷ�ҳ
		if(newsHeadlines.size()>0){
			totalPage=Integer.parseInt(newsHeadlines.get(0).child(0).text().split("��")[1].split("ҳ")[0]);
			for(int i=1;i<=totalPage;i++){
				arcs.addAll(getArcticle(typeUrl,i));
			}
		}else{
			arcs.addAll(getArcticle(typeUrl,1));
		}
		return arcs;
	}
	/**
	 * ��ȡ������ĳһҳ������
	 * @param typeUrl
	 * @param page
	 * @return
	 * @throws IOException
	 */
	public static List<Arcticle> getArcticle(String typeUrl,int page) throws IOException{
		//�@ȡ��ǰ퓵�url��ַ
		String pageUrl=rootDir+typeUrl+separatorChar+page;
		//��ȡ��Ҫ�����Ŀ¼
		String destPath=SAVED_DIR+typeUrl+separatorChar+page;
		//��ǰ������Ŀ¼
		new File(destPath).mkdirs();
		//����編�����Զ�����
		Document doc = getDoc(pageUrl);
		//��ȡ���������µı����ǩ
		Elements docs = doc.select(".link_title");
		List<Arcticle> arcs=new ArrayList<Arcticle>();
		for(int i=0;i<docs.size();i++){
			Element el=docs.get(i);
			Element ael=el.child(0);
			//��ȡ�����ϳ����ӵĵ�ַ
			String url=ael.attr("href");
			String title=ael.text();
			Arcticle a=new Arcticle();
			a.setUrl(url);
			a.setTitle(title);
			//��ȡ�����ı�ǩ
			Element descElement=el.parent().parent().nextElementSibling();
			String description=descElement.text();
			a.setDescription(description);
			//��ȡ������ʱ��ı�ǩ
			Element timeElement=descElement.nextElementSibling();
			String createTime=timeElement.select(".link_postdate").text();
			a.setCreateTime(createTime);
			logger.debug(title+url+description+createTime);
			arcs.add(a);
			//���浽�ļ���
			String filePath=saveFile(destPath,rootDir+url,a);
			a.setFilePath(filePath);
		}
		return arcs;
	}
	/**
	 * ����html���ݵ��ļ���
	 * @param destDir ��Ҫ�����Ŀ��Ŀ�
	 * @param htmlUrl ץȡ��htmlurl
	 * @throws IOException 
	 */
	public static String saveFile(String destDir,String htmlUrl,Arcticle arc) throws IOException{
		Document doc = getDoc(htmlUrl);
		String fileName=htmlUrl.substring(htmlUrl.lastIndexOf("/")+1);
		String file=destDir+separatorChar+fileName+".html";
		File hfile=new File(file);
		boolean ifUpdate=true;
		//�ļ�������Ҫ�ж��ļ��Ƿ���ڸ��� 
		if(hfile.exists()){
			Properties p=new Properties();
			p.load(new FileInputStream(destDir+separatorChar+fileName+".ini"));
			String createTime=p.getProperty("createTime");
			//֮ǰ�����´���ʱ�� С�� ������ȡ���� ʱ�� �������±��޸Ĺ� ��Ҫ����
			if(createTime.compareTo(arc.getCreateTime())<0){
				hfile.delete();
				ifUpdate=true;
			}else{
				ifUpdate=false;
			}
		}
		if(ifUpdate){
			//д���ļ� ������Ϣд����Դ�ļ�
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
