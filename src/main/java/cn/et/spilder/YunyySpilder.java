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

public class YunyySpilder {
	
	public static String baseUrl="http://www.yunyy.cc/";
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
		String url=baseUrl+"/search.html?searchword="+keyWord;
		Document doc=getDoc(url);
		System.out.println(doc.toString());
		Map map=getMovie(doc);
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
	
	public static Map getMovie(Document doc){
		Map map=new HashMap();
		Elements peles=doc.select("#content");
		Elements header=peles.select(".ui-title");
		String total=header.select(".red").get(1).text();
		System.out.println("共产生记录数:"+total);
		map.put("total", total);
		if(Integer.parseInt(total)==0){
			return null;
		}
		Elements movies=peles.select(".yun-link");
		List list=new ArrayList();
		int i=0;
		for(Element e:movies){
			if(i>=50){
				break;
			}
			String title=e.attr("title");
			String playUrl=e.attr("href");
			String imgUrl=e.child(0).child(0).attr("src");
			String actor=e.child(1).select(".actor").text();
			Map mov=new HashMap();
			mov.put("title", title);
			mov.put("imgUrl", imgUrl);
			mov.put("actor", actor);
			mov.put("playList", getPlayInfo(playUrl));
			list.add(mov);
			i++;
		}
		map.put("movList", list);
		return map;
	}
	public static List getPlayInfo(String playUrl){
		String url=baseUrl+playUrl;
		Document doc=getDoc(url);
		Elements playul=doc.select(".playul");
		Elements a=playul.select("a");
		List list=new ArrayList();
		for(Element e:a){
			String title=e.attr("title");
			String href=e.attr("href");
			Map cmap=new HashMap();
			cmap.put("section", title);
			cmap.put("playUrl", href);
			list.add(cmap);
		}
		return list;
	}
	
}
