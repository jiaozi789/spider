package cn.et.spilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BaiduSpilder {
	
	public static String baseUrl="http://so.iqiyi.com/so/q_";
	/**
	 *时间：2017-7-26 下午02:44:16
	 *作者： LM
	 *联系方式：973465719@qq.com
	 * @param args
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException {
		String keyWord="鬼吹灯";
		System.out.println(URLEncoder.encode(keyWord, "UTF-8"));
		String url="http://so.iqiyi.com/so/q_%E9%AC%BC%E5%90%B9%E7%81%AF?source=input&refersource=lib&sr=452504470426";
		Document doc=getDoc(url);
		System.out.println(doc.toString());
		List map=getMovie(doc);
		System.out.println(map);
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
	
	public static List getMovie(Document doc){
		Map map=new HashMap();
		Elements movies=doc.select(".list_item");
		List list=new ArrayList();
		int i=0;
		for(Element e:movies){
			if(i>=10){
				break;
			}
			//获取标题
			Element resultTitle=e.select(".result_title").get(0);
			String title=resultTitle.child(0).attr("title");
			
			
			String playUrl=e.attr("href");
			//获取描述信息
			String actor=e.select(".result_info_txt").text();
			Map mov=new HashMap();
			mov.put("title", title);
			
			//获取图片url
			String imgUrl=e.child(0).child(0).attr("src");
			mov.put("imgUrl", imgUrl);
			mov.put("actor", actor);
			
			//获取播放列表
			Element playElement=resultTitle.nextElementSibling().nextElementSibling().nextElementSibling();
			if(playElement==null){
				continue;
			}
			if(!"info_item mt15".equals(playElement.attr("class"))){
				playElement=playElement.nextElementSibling();
				if(playElement==null){
					continue;
				}
			}
			Elements pes=playElement.select(".album_link");
			List playlist=new ArrayList();
			for(Element pe:pes){
				Element pele=pe.parent().parent();
				if(pele.attr("style")==null || "".equals(pele.attr("style"))){
					String href=pe.attr("href");
					if(!href.startsWith("http")){
						continue;
					}
					Map cmap=new HashMap();
					cmap.put("section", pe.attr("title"));
					
					if(href.startsWith("http://so.iqiyi.com")){
						href=getDoc(href).select("META").get(1).attr("content").split("URL='")[1].split("'")[0];
					}
					cmap.put("playUrl", href);
					
					playlist.add(cmap);
				}
			}
			
			
			mov.put("playList", playlist);
			list.add(mov);
			i++;
		}
		return list;
	}
	
}
